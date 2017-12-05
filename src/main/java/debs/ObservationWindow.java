package debs;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ObservationWindow {
    /**
     * An observation window is a (WINDOW_SIZE x NUM_STATEFUL_PROPERTIES) array
     * of observations for a given Machine
     *
     * propertyIndex - Maps propertyId to the column where the index is stored
     */
    private LinkedHashMap<String, Integer> propertyIndex = new LinkedHashMap<>();
    private ArrayList<ArrayList<Observation>> window;
    private ArrayList<Observation> outgoingObservations;
    private static int windowSize;

    public ObservationWindow(int size) {
        window = new ArrayList<>(size);
        windowSize = size;
    }

    private int getPropertyIndex(String propId) {
        return propertyIndex.get(propId);
    }

    private boolean checkPropertyIndicesMatch(ArrayList<Observation> observations) {
        // When adding a new set of observations, make sure the indices match
        int index = 0;
        for (Observation o:observations) {
            String prop = o.getObservedProperty();
            if (index != propertyIndex.get(prop))
                return false;
        }
        return true;
    }

    public void addObservations(ArrayList<Observation> observations) {
        if (window.size() == windowSize) {
            outgoingObservations = window.get(0);
            window.remove(0);
        } else if (window.size() == 0) {
            int index = 0;
            for (Observation o: observations) {
                propertyIndex.put(o.getObservedProperty(), index);
                index++;
            }
        } else {
            assert checkPropertyIndicesMatch(observations);
        }
        window.add(observations);
    }

    public int getNumRows() {
        return window.size();
    }

    public int getNumCols() {
        if (window.size() > 0 )
            return window.get(0).size();
        return 0;
    }

    public Double[] getPropertyValues(String propId) {
        ArrayList<Double> values = new ArrayList<>();
        int index = getPropertyIndex(propId);
        for (ArrayList<Observation> row: window) {
            Observation o = row.get(index);
            assert o.getObservedProperty().equals(propId);
            values.add(Double.parseDouble(o.getOutputVal()));
        }
        return values.toArray(new Double[(values.size())]);
    }
}
