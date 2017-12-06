package debs;

import debs.rdf.Parser;
import debs.rdf.Triple;
import debs.utils.Kmeans;
import debs.utils.MarkovModel;
import debs.utils.Metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TaskProcessor implements MachineEventListener {
    private static final int WINDOW_SIZE = 10;
    private static final int NUM_ITERATIONS = 50;
    private static final int NUM_TRANSITIONS = 5;
    private static final double P_THRESHOLD = 0.05;

    private static Metadata metadata = new Metadata();
    private static final EventCollection events = new EventCollection();
    private final Parser parser = new Parser();
    private boolean inputIsMetadata = false;
    private final LinkedHashMap<String, ArrayList<String>> machineToObsGrpMap = new LinkedHashMap<>();
    private final LinkedHashMap<String, ObservationWindow> machineObsWindow = new LinkedHashMap<>();
    private final Kmeans kmeans = new Kmeans(NUM_ITERATIONS);


    private static final Logger logger = LoggerFactory.getLogger(TaskProcessor.class);

    public TaskProcessor(String metadataFilename, boolean parsingMetadata) {
        if (parsingMetadata) {
            // If true read metadata into object and serialize it
            inputIsMetadata = true;
            logger.debug("Metadata serialization run!");
        } else {
            logger.debug("Loading serialized metadata ...");
            deSerializeMetadata(metadataFilename);
            parser.addEventListener(this);
        }
    }

    public void processMessage(byte[] message) {
        String line = new String(message, StandardCharsets.UTF_8);
        Triple triple = parser.getTriples(line.trim());
        if (inputIsMetadata) {
            metadata.processMetadata(triple);
        } else {
            parser.processObservations(triple, events, metadata);
        }
    }

    @Override
    public void observationGroupStreamedIn(String obsGrpId) {
        logger.debug("Observation Group was streamed in: " + obsGrpId);
        processEvent(obsGrpId);
    }

    public void cleanUp() {
        // Termination message received at this point
        parser.processFinalObservationGroup();
    }

    private void processEvent(String obsGrpId) {
        ObservationGroup og = events.getObservationGroup(obsGrpId);
        String machineId = og.getMachineId();

        // map current observation group to machineId
        machineToObsGrpMap.computeIfAbsent(machineId,
                creator -> new ArrayList<>()).add(obsGrpId);
        // add current observation group to the machine's observation window
        machineObsWindow.computeIfAbsent(machineId,
                create -> new ObservationWindow(WINDOW_SIZE)).addObservations(og.getObservations());

        assert machineToObsGrpMap.get(machineId).size() == machineObsWindow.get(machineId).getNumRows();

        int numObsInWindow = machineToObsGrpMap.get(machineId).size();

        if (numObsInWindow == WINDOW_SIZE) {
            List<String> curWindow = machineToObsGrpMap.get(machineId).subList(0, WINDOW_SIZE);
            String windowIds = String.join(",", curWindow);
            //logger.debug("Windows: " + windowIds);

            processObservationWindow(machineId, og);
            // Remove element at start
            //logger.debug("Outgoing: " + curWindow.get(0));
            machineToObsGrpMap.get(machineId).remove(0);
        } else if (numObsInWindow > WINDOW_SIZE) { //numObsInWindow > WINDOW_SIZE
            logger.debug("Error: More than 10 observations in current window");
        }
    }

    private void processObservationWindow(String machineId, ObservationGroup curObsGrp) {
        ObservationWindow curWindow = machineObsWindow.get(machineId);
        Set<String> properties = metadata.getPropertiesForMachine(machineId);
        ArrayList<Integer> centroidLabels;

        for (String p: properties) {
            //if (p.equals("_59_31") && curObsGrp.getGroupId().equals("ObservationGroup_53"))
            //    logger.debug("Current Property : " + p);

            ArrayList<Double> values = curWindow.getPropertyValues(p);
            Set<Double> centroids;
            ArrayList<Integer> states;

            int numClusters = metadata.getNumClustersForMachineProperty(machineId, p);
            if (curWindow.isInitialWindow()) {
                kmeans.compute(values, numClusters);
            } else {
                Set<Double> prevCentroids = curWindow.getPrevCentroidsForProperty(p);
                Double prevVal = curWindow.getPrevValueForProperty(p);
                kmeans.compute(values, numClusters, prevVal, prevCentroids);
            }
            states = kmeans.getLabels();
            centroids = kmeans.getCentroids();
            curWindow.setPrevCentroidsForProperty(p, centroids);
            detectAnomalies(states, machineId, p, curObsGrp);
        }
    }

    private void detectAnomalies(ArrayList<Integer> states, String machineId, String prop, ObservationGroup curObsGrp) {
        MarkovModel m = new MarkovModel(states);
        int N = states.size();

        for (int i = 0; i < N - NUM_TRANSITIONS; i++) {
            double probTransition = m.getTransitionProbability(i, i + NUM_TRANSITIONS);
            if (probTransition < P_THRESHOLD) {
                String tId = curObsGrp.getTimestampId();
                String logStr = String.format("Anomaly detected: Machine - %s; Property - %s; TimeStamp: %s; P(trans): %s",
                        machineId, prop, tId, probTransition);
            }
        }
    }

    public void serializeMetadata(String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(metadata);
            oos.close();
            fos.close();
            logger.debug("Metadata written to: " + filename);
            metadata.printMetadata();
        } catch (IOException e) {
            logger.debug("IOException", e);
        }
    }

    private void deSerializeMetadata(String filename) {
        logger.debug("Deserializing metadata from: ", filename);
        try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            metadata = (Metadata) ois.readObject();
            fis.close();
            ois.close();
        } catch (ClassNotFoundException e) {
            logger.debug("Class not found", e);
        } catch (IOException e) {
            logger.debug("IO Error", e);
        }
    }

    public void printMetadata() {
        metadata.printMetadata();
    }
}
