package org.texttechnologylab.dependency.data;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.texttechnologylab.dependency.graph.DependencyGraph;
import org.texttechnologylab.dependency.graph.InvalidDependencyGraphException;

import com.google.common.graph.ImmutableGraph;

public class SentenceDataPoint {

    public final ArrayList<Integer[]> dependencyEdges;
    public final ArrayList<Integer[]> dependencyEdgesWithPunct;

    public final ArrayList<Integer> dependencyDistances;
    public final double depthMean;
    public final double depthVariance;
    public final double headFinalRatio;
    public final double mdd;
    public final double ndd;
    public final double treeDegreeMean;
    public final double treeDegreeVariance;
    public final int crossings;
    public final int dependencyDistanceSum;
    public final int dependencyHeight;
    public final int headFinalDistance;
    public final int leaves;
    public final int numberOfSyntacticLinks;
    public final int rootDistance;
    public final int sentenceLength;
    public final int treeDegree;
    public final int treeHeight;
    public final int treeEditDistance;

    public SentenceDataPoint(ImmutableGraph<Integer> dependencyGraph, ImmutableGraph<Integer> dependencyGraphWithPunct)
        throws InvalidDependencyGraphException {
        DependencyGraph dg = new DependencyGraph(dependencyGraph, dependencyGraphWithPunct);

        try {
            this.dependencyEdges =
                dg.dependencyGraph
                    .edges()
                    .stream()
                    .map(edge -> new Integer[] { edge.source(), edge.target() })
                    .collect(Collectors.toCollection(ArrayList::new));
            this.dependencyEdgesWithPunct =
                dg.dependencyGraphWithPunct
                    .edges()
                    .stream()
                    .map(edge -> new Integer[] { edge.source(), edge.target() })
                    .collect(Collectors.toCollection(ArrayList::new));
            this.dependencyDistances = dg.getDependencyDistances();
            this.dependencyDistanceSum = dg.getDependencyDistanceSum();
            this.sentenceLength = dg.getSentenceLength();
            this.numberOfSyntacticLinks = dg.getNumberOfSyntacticLinks();
            this.rootDistance = dg.rootDistance();
            this.dependencyHeight = dg.dependencyHeight();
            this.mdd = dg.mdd();
            this.ndd = dg.ndd();
            this.treeHeight = dg.treeHeight();
            this.depthMean = dg.depthMean();
            this.depthVariance = dg.depthVariance();
            this.leaves = dg.leaves();
            this.treeDegree = dg.treeDegree();
            this.treeDegreeMean = dg.treeDegreeMean();
            this.treeDegreeVariance = dg.treeDegreeVariance();
            this.headFinalRatio = dg.headFinalRatio();
            this.headFinalDistance = dg.headFinalDistance();
            this.crossings = dg.crossings();
            this.treeEditDistance = dg.randomTreeDistance();
        } catch (Exception e) {
            throw new InvalidDependencyGraphException("An exception occurred while calculating metrics!", e);
        }
    }
}
