package factory;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class FactoryServer {
    // Constantes da fábrica
    private static final int MAX_PARTS = 500; // Estoque máximo de peças
    private static final int MAX_BELT_CAPACITY = 40; // Capacidade da esteira
    private static final int NUM_STATIONS = 4; // Número de estações de produção
    private static final int WORKERS_PER_STATION = 5; // Funcionários por estação
    private static final int PORT = 5000; // Porta do servidor socket

    // Controle de peças (estoque limitado)
    private Semaphore partsSemaphore = new Semaphore(MAX_PARTS);

    // Cada estação tem uma ferramenta (semáforo de 1)
    private Semaphore[] tools = new Semaphore[NUM_STATIONS];

    // Esteira circular compartilhada onde os veículos produzidos são armazenados
    private BlockingQueue<Vehicle> belt = new ArrayBlockingQueue<>(MAX_BELT_CAPACITY);

    // Construtor da fábrica
    public FactoryServer() {
        // Inicializa as ferramentas com capacidade 1 (1 ferramenta por estação)
        for (int i = 0; i < NUM_STATIONS; i++) {
            tools[i] = new Semaphore(1);
        }
    }

    // Inicia a produção em todas as estações com seus funcionários
    public void startProduction() {
        for (int stationId = 0; stationId < NUM_STATIONS; stationId++) {
            for (int workerId = 0; workerId < WORKERS_PER_STATION; workerId++) {
                final int sId = stationId;
                final int wId = workerId;

                // Cada funcionário é uma thread
                new Thread(() -> {
                    try {
                        while (true) {
                            produceVehicle(sId, wId); // Produz um carro
                            Thread.sleep(2000); // Pausa de 2 segundos (simula tempo de produção)
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    // Produz um veículo
    public void produceVehicle(int stationId, int workerId) throws InterruptedException {
        partsSemaphore.acquire(); // Consome uma peça do estoque
        tools[stationId].acquire(); // Pega a ferramenta da estação

        // Cria o veículo e adiciona na esteira
        int beltPosition = belt.size(); // Posição atual da esteira
        Vehicle vehicle = new Vehicle(stationId, workerId, beltPosition);
        belt.put(vehicle); // Coloca o veículo na esteira

        System.out.println("Produced: " + vehicle);
        logProduction(vehicle); // Registra no log de produção

        tools[stationId].release(); // Libera a ferramenta
        // partsSemaphore.release();
    }

    // Vende um veículo para uma loja
    public Vehicle sellVehicle(String storeName, int storeBeltPosition) throws InterruptedException {
        Vehicle vehicle = belt.take(); // Remove um veículo da esteira
        logSale(vehicle, storeName, storeBeltPosition); // Registra a venda
        return vehicle;
    }

    // Inicia o servidor da fábrica para receber pedidos das lojas
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Factory server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Aguarda conexão de uma loja
                new Thread(() -> handleClient(clientSocket)).start(); // Lida com o pedido em uma nova thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Processa pedidos de clientes conectados via socket
    private void handleClient(Socket clientSocket) {
        try (
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            // Lê o pedido da loja
            String request = (String) in.readObject();
            if (request.startsWith("REQUEST_VEHICLE")) {
                // Exemplo: "REQUEST_VEHICLE LojaA POSITION 3"
                String storeName = request.split(" ")[2];
                int storeBeltPosition = Integer.parseInt(request.split(" ")[3]);

                System.out.println("FACTORY: " + storeBeltPosition);
                Vehicle vehicle = sellVehicle(storeName, storeBeltPosition); // Vende o veículo
                out.writeObject(vehicle); // Envia o veículo de volta para a loja
                System.out.println("Vehicle sent to " + storeName + ": " + vehicle);
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Registra informações da produção no arquivo de log
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

    // Registra informações da venda no arquivo de log
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
