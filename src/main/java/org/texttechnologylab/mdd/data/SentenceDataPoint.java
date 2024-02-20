package org.texttechnologylab.mdd.data;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jetty.server.RequestLog.Collection;

public class SentenceDataPoint implements DependencyDataPoint {

    private final ImmutableGraph<Integer> dependencyGraph;
    private final ImmutableGraph<Integer> dependencyGraphWithPunct;

    public SentenceDataPoint(ImmutableGraph<Integer> dependencyGraph, ImmutableGraph<Integer> dependencyGraphWithPunct) {
        assert dependencyGraph.outDegree(0) == 1;
        this.dependencyGraph = dependencyGraph;
        this.dependencyGraphWithPunct = dependencyGraphWithPunct;
    }

    /**
     * Returns the length of the sentence, i.e. the number of words in the sentence.
     * Calculated as the number of edges in the dependency graph minus 1, as the
     * root node is not a word.
     */
    @Override
    public int getSentenceLength() {
        return this.dependencyGraph.edges().size();
    }

    @Override
    public int getNumberOfSyntacticLinks() {
        return this.dependencyGraph.edges().size() - 1;
    }

    @Override
    public int rootDistance() {
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
        return this.getDependencyDistanceSum() / (double) this.getNumberOfSyntacticLinks();
    }

    @Override
    public double ndd() {
        return Math.abs(Math.log(this.mdd() / Math.sqrt(this.rootDistance() * this.getSentenceLength())));
    }

    @Override
    public int crossings() {
        return this.dependencyGraph.edges().stream().map(edge -> this.getNumberOfCrossings(edge)).reduce(0, (a, b) -> a + b);
    }

    private int getNumberOfCrossings(EndpointPair<Integer> u) {
        return (int) this.dependencyGraph.edges()
            .stream()
            .map(v -> (u.source() < v.source() && u.target() > v.target()) || (u.source() > v.source() && u.target() < v.target()))
            .map(b -> b ? 1 : 0)
            .reduce(0, (a, b) -> a + b);
    }

    @Override
    public int dependencyHeight() {
        return this.getMaxDependencyHeight(0);
    }

    private int getMaxDependencyHeight(Integer node) {
        return this.dependencyGraph.successors(node)
            .stream()
            .map(successor -> Math.abs(node - successor) + this.getMaxDependencyHeight(successor))
            .max(Comparator.naturalOrder())
            .orElse(0);
    }

    @Override
    public double depthMean() {
        return this.getAllDependencyDepth(0, 0).stream().mapToDouble(Double::valueOf).average().getAsDouble();
    }

    private List<Integer> getAllDependencyDepth(Integer node, int depth) {
        return this.dependencyGraph.successors(node)
            .stream()
            .flatMap(successor -> Stream.concat(Stream.of(depth), this.getAllDependencyDepth(successor, depth + 1).stream()))
            .collect(Collectors.toList());
    }

    @Override
    public double depthVariance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'depthVariance'");
    }

    @Override
    public int leaves() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'leaves'");
    }

    @Override
    public int treeHeight() {
        return this.getAllDependencyDepth(0, 1).stream().max(Comparator.naturalOrder()).get();
    }

    @Override
    public int treeDegree() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'treeDegree'");
    }

    @Override
    public double treeDegreeMean() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'treeDegreeMean'");
    }

    @Override
    public double treeDegreeVariance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'treeDegreeVariance'");
    }
}
