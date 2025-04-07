package factory;
public class Main {
    public static void main(String[] args) {
        FactoryServer factory = new FactoryServer();
        StoreClient[] stores = {new StoreClient("Store A"), new StoreClient("Store B"), new StoreClient("Store C")};
        
        Thread factoryThread = new Thread(() -> {
            factory.startProduction();
            factory.startServer();
        });
        factoryThread.start();
        
        for (StoreClient store : stores) {
            new Thread(() -> {
                try {
                    while (true) {
                        store.requestVehicle();
                        Thread.sleep(1500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        for (int i = 1; i <= 20; i++) {
            new Thread(new Customer(stores, i)).start();
        }
    }
}