package org.texttechnologylab.mdd.data;

import java.util.ArrayList;

public interface DependencyDataPoint {

    int getSentenceLength();

    int getNumberOfSyntacticLinks();

    int getRootDistance();

    ArrayList<Integer> getDependencyDistances();

    int getDependencyDistanceSum();

    double mdd();

    int getNumberOfCrossings();

}