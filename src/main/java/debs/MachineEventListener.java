package debs;

public interface MachineEventListener {
    // A machine event occurs whenever an entire observation group is streamed in
    void observationGroupStreamedIn(String obsGrpId);
}
