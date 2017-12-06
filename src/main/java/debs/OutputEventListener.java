package debs;

public interface OutputEventListener {
    // occurs when an anomaly is ready to be streamed out
    void streamAnomalyOut(String triple);
}
