package debs;

import java.io.Serializable;

public class Machine implements Serializable {
    private final String id;
    private String type;
    private String model;

    public Machine(String id) {
        this.id = id;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setMachineType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("MachineID: %s; Type: %s; Model: %s", id, type, model);
    }
}
