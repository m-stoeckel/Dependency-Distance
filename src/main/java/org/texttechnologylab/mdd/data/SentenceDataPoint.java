package org.texttechnologylab.mdd.data;

import java.util.ArrayList;
import java.util.List;

public class SentenceDataPoint {

    public int rootDistance = -1;
    public int numberOfSyntacticLinks = -1;
    protected final List<Integer> dependencyDistances;

    public SentenceDataPoint() {
        this.dependencyDistances = new ArrayList<>();
    }

    public void add(int distance) {
        this.dependencyDistances.add(distance);
    }

    public double mdd() {
        double mDD = (double) this.dependencyDistances.stream().reduce(0, Integer::sum);
        return mDD / (double) this.dependencyDistances.size();
    }

    public int getRootDistance() {
        return rootDistance;
    }

    public int getNumberOfSyntacticLinks() {
        return numberOfSyntacticLinks;
    }

    public List<Integer> getDependencyDistances() {
        return dependencyDistances;
    }
}