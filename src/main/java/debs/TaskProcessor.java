package debs;

import debs.rdf.Parser;
import debs.rdf.Triple;
import debs.utils.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TaskProcessor implements MachineEventListener {
    private static Metadata metadata = new Metadata();
    private static EventCollection events = new EventCollection();
    private Parser parser = new Parser();
    private boolean inputIsMetadata = false;
    private LinkedHashMap<String, ArrayList<String>> machineToObsGrpMap = new LinkedHashMap<>();
    private LinkedHashMap<String, ObservationWindow> machineObsWindow = new LinkedHashMap<>();

    private static final int WINDOW_SIZE = 10;
    private static final int NUM_ITERATIONS = 50;
    private static final double P_THRESHOLD = 0.05;
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

            processObservationWindow(machineId );
            // Remove element at start
            //logger.debug("Outgoing: " + curWindow.get(0));
            machineToObsGrpMap.get(machineId).remove(0);
        } else if (numObsInWindow > WINDOW_SIZE) { //numObsInWindow > WINDOW_SIZE
            logger.debug("Error: More than 10 observations in current window");
        }
    }

    private void processObservationWindow(String machineId) {
        ObservationWindow curWindow = machineObsWindow.get(machineId);
        Set<String> properties = metadata.getPropertiesForMachine(machineId);

        for (String p: properties) {
            Double[] values = curWindow.getPropertyValues(p);
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
