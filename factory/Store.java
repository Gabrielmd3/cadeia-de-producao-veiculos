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
            System.out.println(storeName + " received: " + vehicle + " at position " + storeBeltPosition);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Vehicle getVehicle() throws InterruptedException {
        return inventory.take();
    }
}