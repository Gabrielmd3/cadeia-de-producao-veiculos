package factory;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class Customer implements Runnable {
    private StoreClient[] stores;
    private int customerId;
    private Random random = new Random();
    private List<Vehicle> buffer = new ArrayList<>();

    public void addCar(Vehicle vehicle) {
        buffer.add(vehicle);
    }
    
    public Customer(StoreClient[] stores, int customerId) {
        this.stores = stores;
        this.customerId = customerId;
    }

    @Override
    public void run() {
        try {
            int vehiclesToBuy = random.nextInt(25) + 1;
            for (int i = 0; i < vehiclesToBuy; i++) {
                StoreClient store = stores[random.nextInt(stores.length)];
                Vehicle vehicle = store.getVehicle(customerId);
                addCar(vehicle);
                System.out.println("Customer " + customerId + " bought from " + store.storeName + ": " + vehicle);
                Thread.sleep(random.nextInt(5000));
            }
            logBuffer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logBuffer() {
        try (FileWriter fw = new FileWriter("customer_garage.log", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("Customer: " + customerId + " | Number of Cars: " + buffer.size() + " | CarsLog: " + buffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
