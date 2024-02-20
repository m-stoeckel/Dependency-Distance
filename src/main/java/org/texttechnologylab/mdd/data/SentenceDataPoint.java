package org.texttechnologylab.mdd.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableGraph;

public class SentenceDataPoint implements DependencyDataPoint {

    private final ImmutableGraph<Integer> dependencyGraph;

    public SentenceDataPoint(ImmutableGraph<Integer> dependencyGraph) {
        assert dependencyGraph.outDegree(0) == 1;
        this.dependencyGraph = dependencyGraph;
    }

    @Override
    public int getSentenceLength() {
        return this.dependencyGraph.nodes().size() - 2;
    }

    @Override
    public int getNumberOfSyntacticLinks() {
        return this.dependencyGraph.edges().size();
    }

    @Override
    public int getRootDistance() {
        return this.dependencyGraph.successors(0).stream().findFirst().get();
    }

    @Override
    public ArrayList<Integer> getDependencyDistances() {
        return this.dependencyGraph.edges()
                .stream()
                .filter(edge -> edge.source() > 0)
                .sorted(Comparator.comparingInt(EndpointPair::target))
                .map(edge -> {
                    return Math.abs(edge.source() - edge.target());
                })
                .collect(Collectors.toCollection(ArrayList<Integer>::new));
    }

    @Override
    public int getDependencyDistanceSum() {
        return this.dependencyGraph.edges()
                .stream()
                .filter(edge -> edge.source() > 0)
                .map(edge -> {
                    return Math.abs(edge.source() - edge.target());
                })
                .reduce(0, (a, b) -> a + b);
    }

    @Override
    public double mdd() {
        return ((double) this.getDependencyDistanceSum() / (double) this.getSentenceLength());
    }

    @Override
    public int getNumberOfCrossings() {
        return this.dependencyGraph.edges().stream().map(edge -> this.getNumberOfCrossings(edge)).reduce(0,
                (a, b) -> a + b);
    }

    private int getNumberOfCrossings(EndpointPair<Integer> u) {
        return (int) this.dependencyGraph.edges()
                .stream()
                .map(v -> (u.source() < v.source() && u.target() > v.target())
                        || (u.source() > v.source() && u.target() < v.target()))
                .map(b -> b ? 1 : 0)
                .reduce(0, (a, b) -> a + b);
    }
}
