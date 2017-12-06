package debs;

import java.util.HashSet;
import java.util.LinkedHashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class AnomalyCollection {
    // Keep track of properties in an observationGroup flagged as anomalies
    private LinkedHashMap<String, HashSet<String>> obsGrpPropertyMap = new LinkedHashMap<>();
    private int anomalyCount;
    private OutputHandler outputHandler = new OutputHandler();

    private static final Logger logger = LoggerFactory.getLogger(AnomalyCollection.class);

    public void addAnomaly(ObservationGroup og, String propertyId, double observedProbability) {
        if (hasAnomaly(og, propertyId)) {
            logger.debug(String.format("Ignoring duplicated anomaly for %s with property %s; Probability: %s",
                    og.getGroupId(), propertyId, observedProbability));
        } else {
            String obsGrpId = og.getGroupId();
            String anomalyId = String.format("Anomaly_%s", anomalyCount);
            Anomaly anom = new Anomaly(anomalyId);
            anom.setMachineId(og.getMachineId());
            anom.setTimeStampId(og.getTimestampId());
            anom.setTimeStampValue(og.getTimestampVal());
            anom.setObservedProperty(propertyId);
            anom.setObservedProbability(observedProbability);
            obsGrpPropertyMap.computeIfAbsent(obsGrpId,
                    create -> new HashSet<>()).add(propertyId);
            logger.debug(anom.debugStr());
            outputHandler.processAnomaly(anom);
            anomalyCount++;
        }
    }

    private boolean hasAnomaly(ObservationGroup og, String propertyId) {
        String obsGrpId = og.getGroupId();
        return obsGrpPropertyMap.containsKey(obsGrpId) && obsGrpPropertyMap.get(obsGrpId).contains(propertyId);
    }
}
