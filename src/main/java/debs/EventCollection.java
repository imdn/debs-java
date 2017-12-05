package debs;

import java.util.LinkedHashMap;
import java.util.ArrayList;

public class EventCollection {
    // Key:ObsGroupId; Value: <ArrayList of Observations>
    private static LinkedHashMap<String, ObservationGroup> machineObs = new LinkedHashMap<>();

    public void addObservationGroup(String oid, ObservationGroup o) {
        machineObs.put(oid, o);
    }

    public void addTimestampId(String oid, String tid) {
        machineObs.get(oid).setTimestampId(tid);
    }

    public void addTimestampValue(String oid, String value) {
        machineObs.get(oid).setTimestampVal(value);
    }

    public void addMachineId(String oid, String mid) {
        machineObs.get(oid).setMachineId(mid);
    }

    public void addCycleId(String oid, String cid) {
        machineObs.get(oid).setCycle(cid);
    }

    public void addObservation(String oid, Observation o) {
        machineObs.get(oid).addObservation(o);
        //machineObs.computeIfAbsent(oid,
        //        init -> new ArrayList<Observation>()).add(o);

    }

    public ObservationGroup getObservationGroup(String oid) {
        return machineObs.get(oid);
    }
}