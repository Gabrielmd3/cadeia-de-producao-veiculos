package factory;
import java.util.Random;

class Customer implements Runnable {
    private StoreClient[] stores;
    private int customerId;
    private Random random = new Random();
    
    public Customer(StoreClient[] stores, int customerId) {
        this.stores = stores;
        this.customerId = customerId;
    }

    @Override
    public void run() {
        try {
            int vehiclesToBuy = random.nextInt(5) + 1; // Cliente compra entre 1 e 5 veículos
            for (int i = 0; i < vehiclesToBuy; i++) {
                StoreClient store = stores[random.nextInt(stores.length)];
                Vehicle vehicle = store.getVehicle();
                System.out.println("Customer " + customerId + " bought from " + store.storeName + ": " + vehicle);
                Thread.sleep(random.nextInt(5000));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
