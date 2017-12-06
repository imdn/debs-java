package debs;

import java.util.ArrayList;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class OutputHandler {
    private final String SYSTEM_URI = System.getenv().get("SYSTEM_URI_KEY");
    private final String RDF_URI = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private final String MACHINE_URI ="<http://www.agtinternational.com/ontologies/I4.0#machine>";
    private final String IOT_URI = "<http://www.agtinternational.com/ontologies/IoTCore#%s>";
    private final String RESULT_URI = "<http://www.agtinternational.com/ontologies/DEBSAnalyticResults#%s>";
    private final String WMM_URI = "<http://www.agtinternational.com/ontologies/WeidmullerMetadata#%s>";
    private final String XML_DOUBLE_URI = "<http://www.w3.org/2001/XMLSchema#double>";
    private final String XML_DATETIME_URI = "<http://www.w3.org/2001/XMLSchema#dateTime>";

    private OutputEventListener outputEventListener;

    private int sequenceNum = 0;

    private static final Logger logger = LoggerFactory.getLogger(OutputHandler.class);

    public String createTriples(String subject, String predicate, String object) {
        return String.format("%s %s %s .", subject, predicate, object);
    }

    public void addEventListener(OutputEventListener oListener) {
        outputEventListener = oListener;
    }

    public void sendToOutputStream(String body) {
        outputEventListener.streamAnomalyOut(body);
    }

    public String createDebsURI(String fragment) {
        return String.format("<%s#%s>", SYSTEM_URI, fragment);
    }

    public void processAnomaly(Anomaly anom) {
        ArrayList<String> triples = new ArrayList<>();

        String anomalyURI = createDebsURI(anom.getAnomalyId());
        String anomalyType = String.format(RESULT_URI, "Anomaly");
        String timestampURI = createDebsURI(anom.getTimeStampId());

        triples.add(createTriples(anomalyURI, RDF_URI, anomalyType));
        triples.add(createTriples(anomalyURI, MACHINE_URI, String.format(WMM_URI, anom.getMachineId())));
        triples.add(createTriples(anomalyURI,
                String.format(RESULT_URI, "inAbnormalDimension"),
                String.format(WMM_URI, anom.getObservedProperty())));
        triples.add(createTriples(anomalyURI,
                String.format(RESULT_URI, "hasTimeStamp"),
                createDebsURI(anom.getTimeStampId())));
        triples.add(createTriples(anomalyURI,
                String.format(RESULT_URI, "hasProbabilityOfObservedAbnormalSequence"),
                String.format("%s^^%s", anom.getObservedProbability(), XML_DOUBLE_URI)));
        triples.add(createTriples(timestampURI, RDF_URI,
                    String.format(IOT_URI, "Timestamp")));
        triples.add(createTriples(timestampURI,
                String.format(IOT_URI, "ValueLiteral"),
                String.format("%s^^%s", anom.getTimeStampValue(), XML_DATETIME_URI)));

        for (String t: triples) {
            //logger.debug(t);
            sendToOutputStream(t);
        }
    }
}
