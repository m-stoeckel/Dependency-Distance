package org.texttechnologylab.mdd.data;

import java.util.ArrayList;

public interface DependencyDataPoint {

    int getSentenceLength();
    int getNumberOfSyntacticLinks();
    int getDependencyDistanceSum();
    ArrayList<Integer> getDependencyDistances();
    
    int rootDistance();

    double mdd();
    double ndd();
    
    int crossings();
    int dependencyHeight();
    
    double depthMean();
    double depthVariance();

    int leaves();
    int treeHeight();
    int treeDegree();
    double treeDegreeMean();
    double treeDegreeVariance();
}