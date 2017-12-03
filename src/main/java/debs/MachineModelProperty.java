package debs;

import java.io.Serializable;

public class MachineModelProperty implements Serializable{
    private String propId;
    private float probThreshold;
    private int numClusters;
    private boolean isStateful = false;

    MachineModelProperty(String id) {
        this.propId = id;
    }

    public void setProbThreshold(float probThreshold) {
        this.probThreshold = probThreshold;
    }

    public void setNumClusters(int numClusters) {
        this.numClusters = numClusters;
    }

    public void setStateful(boolean stateful) {
        isStateful = stateful;
    }

    public String getPropId() {
        return propId;
    }

    public float getProbThreshold() {
        return probThreshold;
    }

    public int getNumClusters() {
        return numClusters;
    }

    public boolean isStateful() {
        return isStateful;
    }

    @Override
    public String toString() {
        String info = String.format("PropId: %s; P(threshold): %s; #Clusters: %s; Stateful:%s",
                propId, probThreshold, numClusters, isStateful);
        return info;
    }
}
