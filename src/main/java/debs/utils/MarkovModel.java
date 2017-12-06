package debs.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MarkovModel {
    private final ArrayList<Integer> states;
    private final int numStates;
    private final int[][] transitionMatrix;
    private final double[][] transitionProbabilityMatrix;

    public MarkovModel(ArrayList<Integer> states) {
        this.states = states;
        Set<Integer> uniqueStates = new HashSet<>();
        uniqueStates.addAll(states);
        numStates = uniqueStates.size();
        transitionMatrix = new int[numStates][numStates];
        transitionProbabilityMatrix= new double[numStates][numStates];
        buildTransitionMatrix();
        buildTransitionProbabilityMatrix();
    }

    private void buildTransitionMatrix() {
        if (numStates == 1) {
            transitionMatrix[0][0] = 1;
        } else {
            for (int i = 1; i < states.size(); i++) {
                int startState = states.get(i - 1);
                int endState = states.get(i);
                transitionMatrix[startState][endState] += 1;
            }
        }
    }

    private void buildTransitionProbabilityMatrix() {
        if (numStates == 1) {
            transitionProbabilityMatrix[0][0] = 1;
        } else {
            for (int i = 0; i < numStates; i++) {
                int curRowSum = 0;
                for (int j = 0; j < numStates; j++) {
                    curRowSum += transitionMatrix[i][j];
                }
                if (curRowSum != 0) {
                    for (int k = 0; k < numStates; k++) {
                        transitionProbabilityMatrix[i][k] = ((double) transitionMatrix[i][k]) / curRowSum;
                    }
                }
            }
        }
    }

    public String getMatrixString() {
        return Arrays.deepToString(transitionMatrix);
    }
    public String getPMatrixString() {
        return Arrays.deepToString(transitionProbabilityMatrix);
    }

    public double getTransitionProbability(Integer startStateIndex, Integer endStateIndex) {
        assert startStateIndex < endStateIndex;
        double probability = 1;

        for (int i = startStateIndex; i < endStateIndex; i++) {
            int startState = states.get(i);
            int nextState = states.get(i+1);
            probability = probability * transitionProbabilityMatrix[startState][nextState];
        }
        return probability;
    }
}
