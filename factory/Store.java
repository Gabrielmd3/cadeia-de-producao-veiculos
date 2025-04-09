package factory;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class StoreClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final int MAX_STORE_CAPACITY = 10;
    private BlockingQueue<Vehicle> inventory = new ArrayBlockingQueue<>(MAX_STORE_CAPACITY);
    public String storeName;

    public StoreClient(String storeName) {
        this.storeName = storeName;
    }

    public void requestVehicle() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            int storeBeltPosition = inventory.size();
            out.writeObject("REQUEST_VEHICLE " + storeName + " " + storeBeltPosition);
            Vehicle vehicle = (Vehicle) in.readObject();
            inventory.put(vehicle);
            vehicle.setStore(storeName);
            logBuy(vehicle);
            System.out.println(storeName + " received: " + vehicle + " at position " + storeBeltPosition);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Vehicle getVehicle(int customerId) throws InterruptedException {
        Vehicle vehicle = inventory.take();
        logSell(vehicle, customerId);
        return vehicle;
    }

    private void logBuy(Vehicle vehicle) {
        try (FileWriter fw = new FileWriter("store_shopping.log", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(vehicle.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logSell(Vehicle vehicle, int customerId) {
        try (FileWriter fw = new FileWriter("store_sales.log", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(vehicle.toString() + ", Customer: " + customerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
