package debs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MachineModel implements Serializable {
    private String modelName;
    private HashMap<String, MachineModelProperty> properties;

    private static final Logger logger = LoggerFactory.getLogger(MachineModel.class);;

    public MachineModel(String model) {
        this.modelName = model;
        this.properties= new HashMap<String, MachineModelProperty>();
    }

    public MachineModelProperty getProperty(String propId) {
        return properties.get(propId);
    }

    public HashMap<String, MachineModelProperty> getProperties() {
        return properties;
    }

    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }

    public void createProperty(String propId) {
        this.properties.put(propId, new MachineModelProperty(propId));
    }

    public void setProperty(String propId, String propName, String value) {
        MachineModelProperty p = this.getProperty(propId);

        switch (propName) {
            case "ProbabilityThreshold":
                float threshold = Float.parseFloat(value);
                this.properties.get(propId).setProbThreshold(threshold);
                break;

            case "StatefulProperty":
                this.properties.get(propId).setStateful(true);
                break;

            case "NumClusters":
                int numClusters = Integer.parseInt(value);
                this.properties.get(propId).setNumClusters(numClusters);
                break;

            default:
                String debugMsg = String.format(
                        "PropertyId: %s; PropertyName: %s; Value: %s", propId, propName, value);
                logger.debug("Could not set property");
        }
    }

    @Override
    public String toString() {
        String info = String.format("Model name: %s", modelName);
        return info;
    }
}

