package debs.rdf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import debs.*;
import debs.utils.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {
    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    private String curMachineId, curObsGroupId, curObsId, curOutputId, curValueId;
    private MachineEventListener machineEventListener;
    private Observation curObservation;
    private boolean skipObservation = false;

    public void addEventListener(MachineEventListener mListener) {
        machineEventListener = mListener;
    }

    public Triple getTriples(String line) {
        String[] parts = line.split("\\s+");

        Pattern regex1 = Pattern.compile("^<([^>]+)#(.+)>$");
        Pattern regex2 = Pattern.compile("^\"(.+)\"\\^\\^<(.+)#(.+)>$");

        boolean parsed = false;
        boolean isValue = false;

        String subject, object, predicate;

        URI[] uris = new URI[3];

        //logger.debug(line);

        Matcher m;

        for (int i=0; i < parts.length; i++) {
            m = regex1.matcher(parts[i]);
            String currentToken = parts[i];
            if (m.matches()) {
                String namespace = m.group(1);
                String fragment = m.group(2);
                uris[i] = new URI(namespace, fragment);
            } else {
                m = regex2.matcher(currentToken);
                if (m.matches()) {
                    String literalValue = m.group(1);
                    String namespace = m.group(2);
                    String fragment = m.group(3);
                    uris[i] = new URI(namespace, fragment, literalValue);
                } else {
                    //logger.debug("Error! No regex match for - " + currentToken);
                }
            }
        }
        return new Triple(uris);
    }

    public Triple getTurtleTriples(String line) {
        String[] parts = line.split("\\s+");

        Pattern regex1 = Pattern.compile("^(\\w+):(\\w+)(\\.)?$");
        Pattern regex2 = Pattern.compile("^\"(.+)\"\\^\\^(\\w+):(\\w+)(\\.)?$");

        boolean parsed = false;
        boolean isValue = false;

        String subject, object, predicate;

        URI[] uris = new URI[3];

        //logger.debug(line);

        Matcher m;

        for (int i=0; i < parts.length; i++) {
            m = regex2.matcher(parts[i]);
            String currentToken = parts[i];
            if (m.matches()) {
                String literalValue = m.group(1);
                String namespace = m.group(2);
                String fragment = m.group(3);
                uris[i] = new URI(namespace, fragment, literalValue);
            } else {
                m = regex1.matcher(currentToken);
                if (m.matches()) {
                    String namespace = m.group(1);
                    String fragment = m.group(2);
                    uris[i] = new URI(namespace, fragment);
                } else {
                    //logger.debug("Error! No regex match for - " + currentToken);
                }
            }
        }
        return new Triple(uris);
    }

    public void processObservations(Triple t, EventCollection e, Metadata md) {
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
                if (object.equals("MoldingMachineObservationGroup")) {
                    if (curObsGroupId != null) {
                        // Beginning of new event, start processing old event
                        //logger.debug("Send current event for processing: " + subject);
                        machineEventListener.observationGroupStreamedIn(curObsGroupId);
                    }
                    logger.debug("Beginning new observation group:" + subject);
                    resetTrackingVariables();
                    curObsGroupId = subject;
                    e.addObservationGroup(curObsGroupId, new ObservationGroup(curObsGroupId));
                }
                break;

            case "observationResultTime":
                // Triple: obsGrpID - observationResultTime - TimeStampId
                e.addTimestampId(subject, object);
                break;

            case "machine":
                // Triple: obsGrpID - machine - machineId
                e.addMachineId(subject, object);
                curMachineId = object;
                break;

            case "contains":
                // Triple: obsGrpID - contains - obsId
                curObsId = object;
                curObservation = new Observation(curObsId);
                break;

            case "hasValue":
                // Triple: outId - hasValue - valueId
                curValueId = object;
                break;

            case "observationResult":
                // Triple: obsId - observationResult - outId
                curOutputId = object;
                break;

            case "observedCycle":
                // Triple: obsGrpId - observedCycle - cycleId
                e.addCycleId(subject, object);
                break;

            case "observedProperty":
                // Triple: obsId - observedProperty - propId
                // We only need to keep track of stateful properties. Omit the rest
                if (md.isStatefulPropertyForMachine(curMachineId, object)) {
                    skipObservation = false;
                    curObservation.setObservedProperty(object);
                } else {
                    skipObservation = true;
                }
                break;

            case "valueLiteral":
                // Triples
                // valueID - valueLiteral - value
                // timestampId - valueLiteral - value

                if (skipObservation) break;
                if (subject.contains("Timestamp_")) {
                    e.addTimestampValue(curObsGroupId, object);
                } else if (subject.contains("Value_")) {
                    curObservation.setOutputVal(object);
                    e.addObservation(curObsGroupId, curObservation);
                    curObservation = null;
                }
                break;
        }
    }

    private void resetTrackingVariables() {
        // Upon beginning of a new observation group reset variables
        this.skipObservation = false;
        this.curObsGroupId = null;
        this.curMachineId = null;
        this.curObsId = null;
        this.curObservation = null;
        this.curOutputId = null;
        this.curValueId = null;
    }

    public void processFinalObservationGroup() {
        // Generate event for the last observation group that was streamed in
        machineEventListener.observationGroupStreamedIn(curObsGroupId);
    }
}
