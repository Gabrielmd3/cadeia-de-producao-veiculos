package factory;
import java.util.Random;

class Customer implements Runnable {

    // Lista de lojas disponíveis para o cliente comprar veículos
    private StoreClient[] stores;

    // Identificador único do cliente
    private int customerId;

    // Gerador de números aleatórios
    private Random random = new Random();
    
    // Construtor que recebe as lojas e o id do cliente
    public Customer(StoreClient[] stores, int customerId) {
        this.stores = stores;
        this.customerId = customerId;
    }

    // Método run que define o comportamento da thread do cliente
    @Override
    public void run() {
        try {
            // O cliente decide aleatoriamente quantos veículos ele vai comprar (entre 1 e 5)
            int vehiclesToBuy = random.nextInt(5) + 1;

            for (int i = 0; i < vehiclesToBuy; i++) {
                // Escolhe aleatoriamente uma loja entre as disponíveis
                StoreClient store = stores[random.nextInt(stores.length)];

                // Solicita um veículo da loja (espera se não houver veículos no estoque)
                Vehicle vehicle = store.getVehicle();

                // Exibe no console a informação da compra
                System.out.println("Customer " + customerId + " bought from " + store.storeName + ": " + vehicle);

                // Espera um tempo aleatório (até 5 segundos) antes da próxima compra
                Thread.sleep(random.nextInt(5000));
            }
        } catch (InterruptedException e) {
            // Captura interrupções na execução da thread (como sleep ou getVehicle)
            e.printStackTrace();
        }
    }
}

