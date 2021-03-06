package debs.utils;

import debs.Machine;
import debs.MachineModel;
import debs.rdf.Triple;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Metadata implements Serializable {
    // Track current machine when parsing
    private transient String curModelId;
    private transient String curMachineId;
    private transient String curPropertyId;
    private transient String curProbThreshold;

    // Store properties of models and machines
    private final LinkedHashMap<String, Machine> machines;
    private final LinkedHashMap<String, MachineModel> models;

    private static final Logger logger = LoggerFactory.getLogger(Metadata.class);

    public Metadata() {
        machines = new LinkedHashMap<>();
        models = new LinkedHashMap<>();
    }

    public void processMetadata(Triple t) {
        // URI: Namespace#class - We are only interested in the class
        String subject = t.getSubject().getClassName();
        String predicate = t.getPredicate().getClassName();
        String object;

        if (t.getObject().getLitValue() != null) {
            object = t.getObject().getLitValue();
        } else {
            object = t.getObject().getClassName();
        }

        switch (predicate) {
            case "type":
                if (object.equals("MoldingMachine")) {
                    // Triple: machineId - Type - moldingMachine
                    curMachineId = subject;
                    machines.put(subject, new Machine(curMachineId));
                } else if (object.equals("StatefulProperty")) {
                    setCurModelProperty("StatefulProperty", "true");
                }
                break;

            case "hasModel":
                // Triple: machine - hasModel - machineModel
                // Add model to hash if it is new
                if (models.get(object) == null) {
                    models.put(object, new MachineModel(object));
                }
                // Set model of current machine
                machines.get(subject).setModel(object);
                curModelId = object;
                break;

            case "hasProperty":
                //  Triple: machineModel - hasProperty - property
                models.get(subject).createProperty(object);
                curPropertyId = object;
                break;

            case "hasNumberOfClusters":
                // Triple: propertId - hasNumberOfClusters - value
                setCurModelProperty("NumClusters", object);
                break;

            case "valueLiteral":
                // Triple: ProbThresholdID - valueLiteral - value
                if (subject.contains("ProbabilityThreshold")) {
                    curProbThreshold = object;
                }
                break;

            case "isThresholdForProperty":
                // Triple: ProbThresholdId -isThresholdForProperty - propertyId
                setCurModelProperty("ProbabilityThreshold", curProbThreshold);
                break;

            default:
                logger.debug("Ignoring Triple for metadata processing:\n" + t.toString());
        }
    }

    private void setCurModelProperty(String propertyName, String value) {
        // Set property value for current model and current property being parsed
        models.get(curModelId).setProperty(curPropertyId, propertyName, value);
    }

    public LinkedHashMap<String, Machine> getMachines() {
        return machines;
    }

    public LinkedHashMap<String, MachineModel> getModels() {
        return models;
    }

    public String getModelIdForMachine(String machineId) {
        return machines.get(machineId).getModel();
    }

    public Set<String> getPropertiesForMachine(String machineId) {
        String modelId = machines.get(machineId).getModel();
        return models.get(modelId).getPropertyKeys();
    }

    public int getNumClustersForMachineProperty(String machineId, String propertyId) {
        String modelId = machines.get(machineId).getModel();
        return models.get(modelId).getProperty(propertyId).getNumClusters();
    }

    public boolean isStatefulPropertyForMachine(String machineId, String propId) {
        // Check if a given property is stateful for a given machine
        String modelId = machines.get(machineId).getModel();
        if (models.get(modelId).hasProperty(propId)) {
            return models.get(modelId).getProperty(propId).isStateful();
        }
        return false;
    }

    public void printMetadata() {
        logger.debug("Machines Metadata");
        machines.forEach((k,v) -> logger.debug(v.toString()));

        logger.debug("Models Metadata");
        models.forEach((k,v) -> {
            logger.debug(v.toString());
            Set<String> props = v.getPropertyKeys();
            for (String p: props) {
                logger.debug(v.getProperty(p).toString());
            }
        });
    }
}
