package debs.utils;

import debs.Machine;
import debs.MachineModel;
import debs.MachineModelProperty;
import debs.rdf.Triple;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
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
    private HashMap<String, Machine> machines;
    private HashMap<String, MachineModel> models;

    private static final Logger logger = LoggerFactory.getLogger(Metadata.class);

    public Metadata() {
        machines = new HashMap<String, Machine>();
        models = new HashMap<String, MachineModel>();
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

    public HashMap<String, Machine> getMachines() {
        return machines;
    }

    public HashMap<String, MachineModel> getModels() {
        return models;
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
