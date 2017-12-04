package debs;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ObservationGroup {
    private String groupId;
    private String machineId;
    private String timestampId;
    private String timestampVal;
    private String cycle;
    private LinkedHashMap<String, Observation> observations;

    public ObservationGroup(String id) {
        this.groupId = id;
        this.observations = new LinkedHashMap<>();
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public void setTimestampId(String timestampId) {
        this.timestampId = timestampId;
    }

    public void setTimestampVal(String timestampVal) {
        this.timestampVal = timestampVal;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    public void addObservation(Observation observation) {
        String oid = observation.getObservationId();
        observations.put(oid, observation);
    }

    public Observation getObservation(String id) {
        return this.observations.get(id);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getCycle() {
        return cycle;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getTimestampId() {
        return timestampId;
    }

    public String getTimestampVal() {
        return timestampVal;
    }

    public LinkedHashMap<String, Observation> getObservations() {
        return observations;
    }

}
