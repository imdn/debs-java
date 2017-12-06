package debs;

import java.util.LinkedHashMap;

public class Anomaly {
    private String anomalyId;
    private String machineId;
    private String timeStampId;
    private String timeStampValue;
    private String observedProperty;
    private Double observedProbability;

    public Anomaly(String id) {
        this.anomalyId = id;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public void setObservedProperty(String observedProperty) {
        this.observedProperty = observedProperty;
    }

    public void setTimeStampId(String timeStampId) {
        this.timeStampId = timeStampId;
    }

    public void setTimeStampValue(String timeStampValue) {
        this.timeStampValue = timeStampValue;
    }

    public void setObservedProbability(Double observedProbability) {
        this.observedProbability = observedProbability;
    }

    public String getAnomalyId() {
        return anomalyId;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getObservedProperty() {
        return observedProperty;
    }

    public Double getObservedProbability() {
        return observedProbability;
    }

    public String getTimeStampId() {
        return timeStampId;
    }

    public String getTimeStampValue() {
        return timeStampValue;
    }

    public String debugStr() {
        return String.format("AnomalyId: %s; MachineId: %s; TimeStampId:%s; ObsProp: %s; ObsProbability:%s",
                anomalyId, machineId, timeStampId, observedProperty, observedProbability);
    }
}
