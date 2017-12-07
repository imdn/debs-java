package debs;

import debs.rdf.Parser;
import debs.rdf.Triple;
import debs.utils.Kmeans;
import debs.utils.MarkovModel;
import debs.utils.Metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TaskProcessor implements MachineEventListener {
    private static final int WINDOW_SIZE = 10;
    private static final int NUM_ITERATIONS = 50;
    private static final int NUM_TRANSITIONS = 5;
    private static final double P_THRESHOLD = 0.005;

    // for debugging.
    private static String PROPERTY_FILTER; //"_59_31"; //Process only selected property
    private static int OGROUP_LIMIT = -1; //50; // Max number of obs groups to process
    private static int remoteLogMsgCount = 0;

    private static Metadata metadata = new Metadata();
    private static final EventCollection events = new EventCollection();
    private final Parser parser = new Parser();
    private boolean inputIsMetadata = false;
    private final LinkedHashMap<String, ArrayList<String>> machineToObsGrpMap = new LinkedHashMap<>();
    private final LinkedHashMap<String, ObservationWindow> machineObsWindow = new LinkedHashMap<>();
    private final Kmeans kmeans = new Kmeans(NUM_ITERATIONS);
    private final AnomalyCollection anomalies = new AnomalyCollection();
    private int numObsGroupsProcessed;

    private static final Logger logger = LoggerFactory.getLogger(TaskProcessor.class);

    public TaskProcessor(String metadataFilename, boolean parsingMetadata, OutputEventListener outputListener) {
        if (parsingMetadata) {
            // If true read metadata into object and serialize it
            inputIsMetadata = true;
            logger.debug("Metadata serialization run!");
        } else {
            logger.debug("Loading serialized metadata ...");
            deSerializeMetadata(metadataFilename);
            parser.addEventListener(this);
            anomalies.addOutputListener(outputListener);
        }
    }

    public void processMessage(byte[] message) {
        String tuples = new String(message, StandardCharsets.UTF_8);

        /*
        if (remoteLogMsgCount < 10) {
            String logStr = String.format("\n=== Message #%s ===\n %s", remoteLogMsgCount, tuples);
            postRemoteLog(logStr);
            remoteLogMsgCount++;
        }*/

        if (inputIsMetadata) {
            Triple triple = parser.getTriples(tuples.trim());
            metadata.processMetadata(triple);
        } else {
            if ( OGROUP_LIMIT < 0 || (OGROUP_LIMIT > 0 && numObsGroupsProcessed <= OGROUP_LIMIT)) {
                String[] lines = tuples.split("\n+");
                for (String line: lines) {
                    Triple triple = parser.getTriples(line.trim());
                    //logger.debug(triple.toString());
                    parser.processObservations(triple, events, metadata);
                    if (OGROUP_LIMIT > 0 && numObsGroupsProcessed == OGROUP_LIMIT)
                        logger.info(
                                String.format("%s observation groups processed. Will ignore the rest",
                                        numObsGroupsProcessed));
                }
            }
        }
    }

    @Override
    public void observationGroupStreamedIn(String obsGrpId) {
        if (obsGrpId != null) {
            logger.debug("Observation Group was streamed in: " + obsGrpId);
            processEvent(obsGrpId);
            numObsGroupsProcessed++;
        }
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
            //List<String> curWindow = machineToObsGrpMap.get(machineId).subList(0, WINDOW_SIZE);
            //String windowIds = String.join(",", curWindow);
            //logger.debug("Windows: " + windowIds);

            processObservationWindow(machineId, og);

            // Remove outgoing obs group references at start
            //logger.debug("Outgoing: " + curWindow.get(0));
            events.removeObservationGroup(machineToObsGrpMap.get(machineId).get(0));
            machineToObsGrpMap.get(machineId).remove(0);
        } else if (numObsInWindow > WINDOW_SIZE) { //numObsInWindow > WINDOW_SIZE
            logger.debug("Error: More than WINDOWS_SIZE observations in current window");
        }
    }

    private void processObservationWindow(String machineId, ObservationGroup curObsGrp) {
        ObservationWindow curWindow = machineObsWindow.get(machineId);
        Set<String> properties = metadata.getPropertiesForMachine(machineId);
        ArrayList<Integer> centroidLabels;

        for (String p: properties) {
            //if (p.equals("_59_31") && curObsGrp.getGroupId().equals("ObservationGroup_53"))
            //    logger.debug("Current Property : " + p);
            if (PROPERTY_FILTER != null && !p.equals(PROPERTY_FILTER))
                continue;

            kmeans.printDebugInfo(
                    String.format("Machine: %s; ObsGrp: %s; Timestamp:%s; Property: %s",
                            machineId, curObsGrp.getGroupId(), curObsGrp.getTimestampId(), p));

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

        kmeans.printDebugInfo("Transition Matrix:\n" + m.getMatrixString());
        kmeans.printDebugInfo("Transition Probability Matrix:\n" + m.getPMatrixString());

        // We check only for the last N transitions
        int transitionEndIndex = N - 1;
        int transitionStartIndex = transitionEndIndex - NUM_TRANSITIONS;
        int i = N -1;
        double probTransition = m.getTransitionProbability(transitionStartIndex, transitionEndIndex);
        if (probTransition < P_THRESHOLD) {
            String anomalousObsGrpId = machineToObsGrpMap.get(machineId).get(transitionStartIndex);
            ObservationGroup anomOG = events.getObservationGroup(anomalousObsGrpId);
            anomalies.addAnomaly(anomOG, prop, probTransition);
            String logStr = String.format("Anomaly detected: Machine - %s; Property - %s; TimeStamp: %s; P(trans): %s",
                    machineId, prop, anomOG.getTimestampId(), probTransition);
            logger.debug(logStr);
            kmeans.printDebugInfo(logStr);
            List<String> curWindow = machineToObsGrpMap.get(machineId).subList(0, WINDOW_SIZE);
            String windowIds = String.join(",", curWindow);
            kmeans.printDebugInfo("Window: " + windowIds);
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
        logger.debug(String.format("Deserializing metadata from: %s", filename));
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

    public static void postRemoteLog(String message) {
        URL url;
        URLConnection con;
        HttpURLConnection http;
        try {
            url = new URL("http://imdn.pythonanywhere.com/postmsg");
            con = url.openConnection();
            http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            //http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            String urlParams = String.format("message=%s", message);
            logger.debug("UrlParams: " + urlParams);
            http.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(http.getOutputStream());
            wr.writeBytes(urlParams);
            wr.flush();
            wr.close();

            int responseCode = http.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + urlParams);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());

            Thread.sleep(1000);
        } catch (Exception e) {
            logger.debug("Exception", e);
        }
    }

}
