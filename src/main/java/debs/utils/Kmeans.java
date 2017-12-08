package debs.utils;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;


public class Kmeans {

    private static int maxIterations;
    private ArrayList<Integer> finalLabels = new ArrayList<>();
    private Set<Double> finalCentroids = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger(Kmeans.class);

    public Kmeans(int value) {
        maxIterations = value;
    }

    public ArrayList<Integer> getLabels() {
        return finalLabels;
    }

    public Set<Double> getCentroids() {
        return finalCentroids;
    }

    public void printDebugInfo(String str) {
        logger.debug(str);
    }

    public void compute(ArrayList<Double> values,
                        int numClusters) {
        Set<Double> uniqValues  = new LinkedHashSet<>();
        uniqValues.addAll(values);

        Set<Double> prevCentroids = new HashSet<>();

        int index = 0;
        // Use first k values as initial centroids
        for (Double v: uniqValues) {
            if (index < numClusters)
                prevCentroids.add(v);
            else
                break;
            index ++;
        }
        compute(values, numClusters, null, prevCentroids);
    }

    public void compute(ArrayList<Double> values,
                                 int numClusters,
                                 Double outgoingValue,
                                 Set<Double> seedCentroids) {
        boolean centroidsComputed = false;
        Double incomingValue;
        Set<Double> centroids = new HashSet<>();
        Set<Double> uniqValues = new LinkedHashSet<>();

        logger.debug(String.format("Num allowed cluster centroids: %s", numClusters));

        uniqValues.addAll(values);
        incomingValue = values.get(values.size() - 1);

        if (outgoingValue == null) {
            // initial clusters are the first k-distinct values
            int k = numClusters <= uniqValues.size() ? numClusters : uniqValues.size();
            int index = 0;
            for (Double val : uniqValues) {
                centroids.add(val);
                index++;
                if (index == k)
                    break;
            }
            if (numClusters >= uniqValues.size())
                // More clusters than unique values
                centroidsComputed = true;
        } else if (outgoingValue.equals(incomingValue)) {
            // no change in cluster centers
            centroids = seedCentroids;
            centroidsComputed = true;
            logger.debug(String.format("No change to centroids. Outgoing values == incoming: %s", incomingValue));
        } else if (uniqValues.size() <= numClusters) {
            // fewer unique values than required upper bound of cluster centers
            centroids = uniqValues;
            centroidsComputed = true;
            logger.debug(String.format("Fewer unique vals than max Clusters. Each is a centroid."));
        } else if (seedCentroids.contains(outgoingValue)) { // && !values.contains(outgoingValue)) {
            // if an outgoing value was the cluster center and it's not in values, then
            // next incoming value is a cluster center if it's not already one
            if (seedCentroids.contains(incomingValue)) {
                logger.debug("Rare case! A cluster center has same values as incoming");
            }
            logger.debug(String.format("Reseeding centroids due to empty cluster centre: %s. Replaced by: %s",
                                            outgoingValue, incomingValue));
            logger.debug(String.format("Old Seed Centroids: %s", Arrays.toString(seedCentroids.toArray())));
            centroids = seedCentroids;
            centroids.remove(outgoingValue);
            centroids.add(incomingValue);
            logger.debug(String.format("New Seed Centroids: %s", Arrays.toString(centroids.toArray())));
        } else if (seedCentroids.size() < numClusters) {
            // add last incoming value to set of centroids
            logger.debug(String.format("Fewer centroids than allowed. Adding incoming value: %s", incomingValue));
            centroids.add(incomingValue);
        }

        if (!centroidsComputed) {
            logger.debug(String.format("Computing Centroids with Seeds: %s", Arrays.toString(seedCentroids.toArray())));
            computeCentroids(values, numClusters, seedCentroids);
        } else {
            this.finalCentroids = centroids;
            this.finalLabels = assignLabels(values, centroids);
        }
        logger.debug(String.format("Values: %s", Arrays.toString(values.toArray())));
        logger.debug(String.format("Final Centroids: %s", Arrays.toString(finalCentroids.toArray())));
        logger.debug(String.format("Labels: %s", Arrays.toString(finalLabels.toArray())));
    }

    private void computeCentroids(ArrayList<Double> values, int numClusters,
                                                Set<Double> initCentroids) {
        Double dist = null;
        Double minDist = null;
        LinkedHashMap<Double, Double> valueToCentroidMap;
        Set<Double> computedCentroids;
        Integer currentIteration = 0;

        do {
            // First get nearest centroids
            valueToCentroidMap = findNearestCentroid(values, initCentroids);

            // Compute new centroids. First create an inverse map
            LinkedHashMap<Double, ArrayList<Double>> centroidToValueMap = new LinkedHashMap<>();
            for (Double val : valueToCentroidMap.keySet()) {
                Double centroid = valueToCentroidMap.get(val);
                centroidToValueMap.computeIfAbsent(centroid, create -> new ArrayList<>()).add(val);
            }

            // calculate mean of centroids
            computedCentroids = new HashSet<>();

            for (Double cen: centroidToValueMap.keySet()) {
                ArrayList<Double> valuesList = centroidToValueMap.get(cen);
                Double mean = valuesList.stream().mapToDouble(val -> val).average().getAsDouble();
                computedCentroids.add(mean);
//                String logStr = String.format("Centroid: {%s} -> {%s}; Average: {%s}", cen,
//                        Arrays.toString(valuesList.toArray()), mean);
//                logger.debug(logStr);
            }

            if (computedCentroids.containsAll(initCentroids) && computedCentroids.size() == initCentroids.size()) {
                //if (computedCentroids.equals(initCentroids)) //bad equality check
                logger.debug("Convergence achived in iteration: " + currentIteration.toString());
                //logger.debug(String.format("Centroids: %s", Arrays.toString(initCentroids.toArray())));
                break;
            }
            else {
//                logger.debug("Iteration: " + currentIteration.toString());
//                logger.debug(String.format("Centroids: %s", Arrays.toString(initCentroids.toArray())));
//                logger.debug(String.format("New: %s", Arrays.toString(computedCentroids.toArray())));
                initCentroids = computedCentroids;
            }
            currentIteration++;

        } while (currentIteration < maxIterations);

        this.finalLabels = assignLabels(values, computedCentroids, valueToCentroidMap);
        this.finalCentroids = computedCentroids;
    }

    private ArrayList<Integer> assignLabels(ArrayList<Double> values, Set<Double> centroids) {
        LinkedHashMap<Double, Double> valueToCentroidMap;
        valueToCentroidMap = findNearestCentroid(values, centroids);
        return assignLabels(values, centroids, valueToCentroidMap);
    }

    private ArrayList<Integer> assignLabels(ArrayList<Double> values,
                                           Set<Double> centroids,
                                           LinkedHashMap<Double, Double> valueToCentroidMap) {
        ArrayList<Integer> labels = new ArrayList<>(values.size());
        HashMap<Double, Integer> states = new HashMap<>();

        int index = 0;
        // Assign state label to centroids starting from 0
        for (Double centroid: centroids) {
            states.put(centroid, index);
            index++;
        }
        // Get centroid state label for values
        for (Double val: values) {
            Double centroid = valueToCentroidMap.get(val);
            labels.add(states.get(centroid));
        }
        return labels;
    }

    private LinkedHashMap<Double, Double> findNearestCentroid(ArrayList<Double> values,
                                                                    Set<Double>  centroids) {
        // Given values and centroids, find the centroid with min distance
        Double minDist, dist;
        LinkedHashMap<Double, Double> valueToCentroidMap = new LinkedHashMap<>();

        for (Double v : values) {
            minDist = null;
            if (valueToCentroidMap.containsKey(v))
                // We've already computed the centroid for this value
                continue;
            for (Double c : centroids) {
                dist = Math.abs(v - c);
                if (minDist == null) {
                    minDist = dist;
                    valueToCentroidMap.put(v, c);
                } else {
                    if (minDist.equals(dist)) {
                        // Distance is same. Add cluster with highest value
                        Double assignedCentroid = valueToCentroidMap.get(v);
                        Double bigger = Math.max(assignedCentroid, c);
                        valueToCentroidMap.replace(v, bigger);
                        logger.debug(String.format("Inside iteration: Same distance from val(%.4f) to centroids (%.4f) and (%.4f). Chosen: %s",
                                v, c, assignedCentroid, bigger));
                    } else if (minDist > dist) {
                        // Remove existing centroid and add the new centroid
                        valueToCentroidMap.put(v, c);
                        minDist = dist;
                    }
                }
            }
        }

        return valueToCentroidMap;
    }


}
