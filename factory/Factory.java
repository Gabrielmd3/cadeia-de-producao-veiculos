package factory;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class FactoryServer {
    // Constantes da fábrica
    private static final int MAX_PARTS = 500;                // Estoque máximo de peças
    private static final int MAX_BELT_CAPACITY = 40;           // Capacidade da esteira circular
    private static final int NUM_STATIONS = 4;                 // Número de estações de produção
    private static final int WORKERS_PER_STATION = 5;          // Funcionários por estação (dispostos em círculo)
    private static final int PORT = 5000;                      // Porta do servidor socket

    // Controle de peças (estoque limitado) - usado para simular o consumo de peças para montagem
    private Semaphore partsSemaphore = new Semaphore(MAX_PARTS);

    // Ferramentas de montagem: para cada estação, temos um array de semáforos representando as ferramentas.
    // Cada estação possui WORKERS_PER_STATION ferramentas, distribuídas de forma circular.
    private Semaphore[][] tools = new Semaphore[NUM_STATIONS][WORKERS_PER_STATION];

    // Esteira circular compartilhada para armazenar os veículos produzidos (buffer com capacidade limitada)
    private BlockingQueue<Vehicle> belt = new ArrayBlockingQueue<>(MAX_BELT_CAPACITY);

    // Construtor da fábrica: inicializa as ferramentas de cada estação
    public FactoryServer() {
        // Para cada estação e para cada ferramenta (de cada funcionário), instanciar um semáforo com 1 permissão.
        for (int station = 0; station < NUM_STATIONS; station++) {
            for (int worker = 0; worker < WORKERS_PER_STATION; worker++) {
                tools[station][worker] = new Semaphore(1);
            }
        }
    }

    // Inicia a produção de veículos em todas as estações
    public void startProduction() {
        for (int stationId = 0; stationId < NUM_STATIONS; stationId++) {
            for (int workerId = 0; workerId < WORKERS_PER_STATION; workerId++) {
                final int sId = stationId;
                final int wId = workerId;
                // Cada funcionário é uma thread que produzirá veículos continuamente
                new Thread(() -> {
                    try {
                        while (true) {
                            produceVehicle(sId, wId);  // Produz um veículo na estação 'sId' pelo funcionário 'wId'
                            Thread.sleep(2000);         // Pausa de 2 segundos simulando o tempo de produção
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    // Método que simula a produção de um veículo
    public void produceVehicle(int stationId, int workerId) throws InterruptedException {
        // Adquire uma peça do estoque (simula o consumo de uma peça)
        partsSemaphore.acquire();
        
        // Cada funcionário precisa pegar 2 ferramentas: a da posição dele (esquerda) e a da direita
        // Como os funcionários estão dispostos circularmente, a ferramenta da direita é:
        // (workerId + 1) % WORKERS_PER_STATION
        Semaphore leftTool = tools[stationId][workerId];
        Semaphore rightTool = tools[stationId][(workerId + 1) % WORKERS_PER_STATION];

        // Adquire as duas ferramentas necessárias para montar o veículo
        leftTool.acquire();
        rightTool.acquire();

        // Obtém a posição atual na esteira (buffer) - essa posição representa onde o veículo será colocado
        int beltPosition = belt.size();

        // Cria o veículo com as informações da estação, funcionário e posição na esteira
        Vehicle vehicle = new Vehicle(stationId, workerId, beltPosition);

        // Adiciona o veículo na esteira circular (bloqueia se a esteira estiver cheia)
        belt.put(vehicle);

        System.out.println("Produced: " + vehicle);
        logProduction(vehicle);  // Registra no log de produção (arquivo)

        // Libera as ferramentas após a produção, permitindo que outros funcionários as utilizem
        rightTool.release();
        leftTool.release();
        
        // Libera a peça no estoque (na nossa simulação a peça é retornada após uso)
        partsSemaphore.release();
    }

    // Vende um veículo para uma loja
    // Quando a fábrica vende, ela retira um veículo da esteira e registra a venda no log
    public Vehicle sellVehicle(String storeName, int storeBeltPosition) throws InterruptedException {
        // Remove um veículo da esteira (bloqueia se não houver veículos)
        Vehicle vehicle = belt.take();
        // Registra a venda no log com informações adicionais: nome da loja e posição na esteira da loja
        logSale(vehicle, storeName, storeBeltPosition);
        return vehicle;
    }

    // Inicia o servidor que receberá requisições de lojas (via Socket)
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Factory server started on port " + PORT);
            // Loop infinito para aceitar conexões de lojas continuamente
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Aguarda uma conexão de uma loja
                // Cria uma nova thread para tratar a requisição da loja separadamente
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Trata as requisições vindas das lojas via Socket
    private void handleClient(Socket clientSocket) {
        try (
            // Cria fluxos para enviar e receber objetos (os dados são serializados)
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            // Lê a requisição vinda da loja. Exemplo de mensagem:
            // "REQUEST_VEHICLE LojaA POSITION 3"
            String request = (String) in.readObject();
            if (request.startsWith("REQUEST_VEHICLE")) {
                // O split assume que a mensagem tem o formato correto:
                // [0] = "REQUEST_VEHICLE", [1] = <algum token>, [2] = Nome da loja, [3] = Posição da esteira da loja
                String storeName = request.split(" ")[2];
                int storeBeltPosition = Integer.parseInt(request.split(" ")[3]);

                System.out.println("FACTORY: " + storeBeltPosition);
                // Vende (retira) um veículo da esteira e registra a venda
                Vehicle vehicle = sellVehicle(storeName, storeBeltPosition);
                // Envia o veículo para a loja
                out.writeObject(vehicle);
                System.out.println("Vehicle sent to " + storeName + ": " + vehicle);
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Registra informações da produção em um log (arquivo de texto)
    private void logProduction(Vehicle vehicle) {
        try (
            FileWriter fw = new FileWriter("factory_production.log", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)
        ) {
            // Registra os dados do veículo (incluindo posição na esteira) no log
            out.println(vehicle.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Registra informações da venda, incluindo qual loja recebeu o veículo e a posição na esteira da loja
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
