package factory;
import java.io.*;
import java.util.Random;

class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int idCounter = 1;
    private final int id;
    private final String color;
    private final String type;
    private final int stationId;
    private final int workerId;
    private final int beltPosition;
    private String store;

    public Vehicle(int stationId, int workerId, int beltPosition) {
        this.id = idCounter++;
        this.color = getRandomColor();
        this.type = getRandomType();
        this.stationId = stationId;
        this.workerId = workerId;
        this.beltPosition = beltPosition;
        this.store = "";
    }

    public void setStore(String store) {
        this.store = store;
    }

    private String getRandomColor() {
        String[] colors = {"Red", "Green", "Blue"};
        return colors[new Random().nextInt(colors.length)];
    }

    private String getRandomType() {
        String[] types = {"SUV", "SEDAN"};
        return types[new Random().nextInt(types.length)];
    }

    public int getBeltPosition() {
        return beltPosition;
    }

    @Override
    public String toString() {
        return "Vehicle[ ID:" + id + ", Color:" + color + ", Type:" + type + ", Station:" + stationId + ", Worker:" + workerId + ", Belt Position:" + beltPosition + ", Store:" + store + " ]";
    }
}
