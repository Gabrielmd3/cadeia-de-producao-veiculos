package factory;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class StoreClient {
    // Endereço do servidor da fábrica (localhost para testes locais)
    private static final String SERVER_HOST = "localhost";
    
    // Porta em que a fábrica está escutando
    private static final int SERVER_PORT = 5000;
    
    // Capacidade máxima do "estoque" da loja (a esteira circular da loja)
    private static final int MAX_STORE_CAPACITY = 35;
    
    // Fila (buffer) para armazenar os veículos recebidos da fábrica
    private BlockingQueue<Vehicle> inventory = new ArrayBlockingQueue<>(MAX_STORE_CAPACITY);
    
    // Nome da loja (será usado nos logs e na identificação da loja na requisição)
    public String storeName;

    // Construtor que define o nome da loja
    public StoreClient(String storeName) {
        this.storeName = storeName;
    }

    // Método responsável por fazer uma requisição de veículo à fábrica
    public void requestVehicle() {
        try (
            // Cria uma conexão com o servidor da fábrica
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            
            // Prepara para enviar dados para o servidor
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            
            // Prepara para receber dados do servidor
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            // Calcula a posição atual da esteira da loja (baseado no tamanho atual do buffer)
            int storeBeltPosition = inventory.size();
            System.out.println("STORE: " + storeBeltPosition);
            
            // Envia uma requisição com a seguinte estrutura:
            // "REQUEST_VEHICLE <nome_da_loja> <posição_na_esteira>"
            out.writeObject("REQUEST_VEHICLE " + storeName + " " + storeBeltPosition);
            
            // Recebe o veículo como resposta da fábrica
            Vehicle vehicle = (Vehicle) in.readObject();
            
            // Armazena o veículo recebido no buffer da loja
            inventory.put(vehicle);
            
            // Exibe no console a informação do veículo recebido
            System.out.println(storeName + " received: " + vehicle + " at position " + storeBeltPosition);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace(); // Trata exceções relacionadas à comunicação ou interrupções
        }
    }

    // Método que permite um cliente retirar (comprar) um veículo da loja
    public Vehicle getVehicle() throws InterruptedException {
        return inventory.take(); // Retira o primeiro veículo disponível (espera se estiver vazio)
    }
}
