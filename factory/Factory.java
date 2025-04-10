package factory;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class FactoryServer {
    private static final int MAX_PARTS = 500;                
    private static final int MAX_BELT_CAPACITY = 40;           
    private static final int NUM_STATIONS = 4;                 
    private static final int WORKERS_PER_STATION = 5;          
    private static final int PORT = 5000;                      

    private Semaphore partsSemaphore = new Semaphore(MAX_PARTS);
    private Semaphore[][] tools = new Semaphore[NUM_STATIONS][WORKERS_PER_STATION];
    private BlockingQueue<Vehicle> belt = new ArrayBlockingQueue<>(MAX_BELT_CAPACITY);
    public FactoryServer() {
        for (int station = 0; station < NUM_STATIONS; station++) {
            for (int worker = 0; worker < WORKERS_PER_STATION; worker++) {
                tools[station][worker] = new Semaphore(1);
            }
        }
    }

    public void startProduction() {
        for (int stationId = 0; stationId < NUM_STATIONS; stationId++) {
            for (int workerId = 0; workerId < WORKERS_PER_STATION; workerId++) {
                final int sId = stationId;
                final int wId = workerId;
                new Thread(() -> {
                    try {
                        while (true) {
                            produceVehicle(sId, wId);
                            Thread.sleep(2000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    public void produceVehicle(int stationId, int workerId) throws InterruptedException {
        partsSemaphore.acquire();
        Semaphore leftTool = tools[stationId][workerId];
        Semaphore rightTool = tools[stationId][(workerId + 1) % WORKERS_PER_STATION];

        leftTool.acquire();
        rightTool.acquire();

        int beltPosition = belt.size();
        Vehicle vehicle = new Vehicle(stationId, workerId, beltPosition);
        belt.put(vehicle);

        System.out.println("Produced: " + vehicle);
        logProduction(vehicle);

        rightTool.release();
        leftTool.release();
        partsSemaphore.release();
    }

    public Vehicle sellVehicle(String storeName, int storeBeltPosition) throws InterruptedException {
        Vehicle vehicle = belt.take();
        logSale(vehicle, storeName, storeBeltPosition);
        return vehicle;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Factory server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            String request = (String) in.readObject();
            if (request.startsWith("REQUEST_VEHICLE")) {
                String storeName = request.split(" ")[2];
                int storeBeltPosition = Integer.parseInt(request.split(" ")[3]);
                Vehicle vehicle = sellVehicle(storeName, storeBeltPosition);
                out.writeObject(vehicle);
                System.out.println("Vehicle sent to " + storeName + ": " + vehicle);
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logProduction(Vehicle vehicle) {
        try (
            FileWriter fw = new FileWriter("factory_production.log", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)
        ) {
            out.println(vehicle.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logSale(Vehicle vehicle, String storeName, int storeBeltPosition) {
        try (
            FileWriter fw = new FileWriter("factory_sales.log", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)
        ) {
            out.println(vehicle.toString() + ", Store: " + storeName + ", Store Belt Position: " + storeBeltPosition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
