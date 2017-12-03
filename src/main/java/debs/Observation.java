package debs;

public class Observation {
    private String observationId;
    private String observedProperty;
    private String outputId;
    private String outputVal;

    Observation(String id) {
        this.observationId = id;
    }

    public void setObservedProperty(String observedProperty) {
        this.observedProperty = observedProperty;
    }

    public void setOutputId(String outputId) {
        this.outputId = outputId;
    }

    public void setOutputVal(String outputVal) {
        this.outputVal = outputVal;
    }

    public String getObservationId() {
        return observationId;
    }

    public String getObservedProperty() {
        return observedProperty;
    }

    public String getOutputId() {
        return outputId;
    }

    public String getOutputVal() {
        return outputVal;
    }
}

