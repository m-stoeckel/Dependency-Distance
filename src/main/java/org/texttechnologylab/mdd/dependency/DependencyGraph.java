package org.texttechnologylab.mdd.dependency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.Traverser;

public class DependencyGraph {

    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = new LevenshteinDistance();
    public final ImmutableGraph<Integer> dependencyGraph;
    public final ImmutableGraph<Integer> dependencyGraphWithPunct;

    public DependencyGraph(ImmutableGraph<Integer> dependencyGraph, ImmutableGraph<Integer> dependencyGraphWithPunct) {
        assert dependencyGraph.outDegree(0) == 1;
        this.dependencyGraph = dependencyGraph;
        this.dependencyGraphWithPunct = dependencyGraphWithPunct;
    }

    /**
     * Returns the length of the sentence, i.e. the number of words in the sentence.
     * Calculated as the number of edges in the dependency graph minus 1, as the
     * root node is not a word.
     */
    public int getSentenceLength() {
        return this.dependencyGraph.edges().size();
    }

    public int getNumberOfSyntacticLinks() {
        return this.dependencyGraph.edges().size() - 1;
    }

    public int rootDistance() {
        return this.dependencyGraph.successors(0).stream().findFirst().get();
    }

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

    public int getDependencyDistanceSum() {
        return this.dependencyGraph.edges()
            .stream()
            .filter(edge -> edge.source() > 0)
            .map(edge -> {
                return Math.abs(edge.source() - edge.target());
            })
            .reduce(0, (a, b) -> a + b);
    }

    public double mdd() {
        return this.getDependencyDistanceSum() / (double) this.getNumberOfSyntacticLinks();
    }

    public double ndd() {
        return Math.abs(Math.log(this.mdd() / Math.sqrt(this.rootDistance() * this.getSentenceLength())));
    }

    public int crossings() {
        return this.dependencyGraph.edges().stream().map(edge -> this.getNumberOfCrossings(edge)).reduce(0, (a, b) -> a + b);
    }

    private int getNumberOfCrossings(EndpointPair<Integer> u) {
        return (int) this.dependencyGraph.edges()
            .stream()
            .map(v -> (u.source() < v.source() && u.target() < v.target()) || (u.source() > v.source() && u.target() > v.target()))
            .map(b -> b ? 1 : 0)
            .reduce(0, (a, b) -> a + b);
    }

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

    private Stream<Integer> getDependencyDepthStream(Integer node, int depth) {
        return this.dependencyGraph.successors(node)
            .stream()
            .flatMap(successor -> Stream.concat(Stream.of(depth), this.getDependencyDepthStream(successor, depth + 1)));
    }

    public double depthMean() {
        return this.getDependencyDepthStream(0, 0).mapToDouble(Double::valueOf).average().getAsDouble();
    }

    public double depthVariance() {
        List<Integer> depths = this.getDependencyDepthStream(0, 0).collect(Collectors.toList());
        double mean = depths.stream().mapToDouble(Double::valueOf).average().getAsDouble();
        return depths.stream().map(depth -> Math.pow(depth - mean, 2)).mapToDouble(Double::valueOf).average().getAsDouble();
    }

    public int leaves() {
        return this.recurseLeaves(0);
    }

    private int recurseLeaves(Integer node) {
        return this.dependencyGraph.successors(node)
            .stream()
            .flatMap(successor ->
                this.dependencyGraph.successors(successor).isEmpty() ? Stream.of(1) : Stream.of(this.recurseLeaves(successor))
            )
            .reduce(0, (a, b) -> a + b);
    }

    public int treeHeight() {
        return this.getDependencyDepthStream(0, 1).max(Comparator.naturalOrder()).get();
    }

    private Stream<Integer> getDegreeStream() {
        return this.dependencyGraph.nodes().stream().skip(1).map(node -> this.dependencyGraph.outDegree(node));
    }

    public int treeDegree() {
        return getDegreeStream().max(Comparator.naturalOrder()).get();
    }

    public double treeDegreeMean() {
        return getDegreeStream().mapToDouble(Double::valueOf).average().getAsDouble();
    }

    public double treeDegreeVariance() {
        List<Integer> degrees = getDegreeStream().collect(Collectors.toList());
        double mean = degrees.stream().mapToDouble(Double::valueOf).average().getAsDouble();
        return degrees.stream().map(degree -> Math.pow(degree - mean, 2)).mapToDouble(Double::valueOf).average().getAsDouble();
    }

    public double headFinalRatio() {
        return this.dependencyGraph.nodes()
            .stream()
            .skip(1)
            .map(head -> this.dependencyGraph.successors(head).stream().mapToDouble(dependent -> head > dependent ? 1. : 0.).average())
            .filter(OptionalDouble::isPresent)
            .mapToDouble(OptionalDouble::getAsDouble)
            .average()
            .getAsDouble();
    }

    public int headFinalDistance() {
        Integer root = this.dependencyGraph.successors(0).stream().findFirst().get();
        final ArrayList<Integer> traversalOrder = new ArrayList<>(this.dependencyGraph.nodes().size() - 1);
        Traverser.forGraph(this.dependencyGraphWithPunct).depthFirstPostOrder(root).forEach(node -> traversalOrder.add(node));

        final ArrayList<Integer> orignalOrder = new ArrayList<>(traversalOrder);
        orignalOrder.sort(Comparator.naturalOrder());

        char[] originalCharacters = new char[orignalOrder.size()];
        char[] traversalCharacters = new char[traversalOrder.size()];
        for (int i = 0; i < traversalOrder.size(); i++) {
            originalCharacters[i] = (char) (orignalOrder.get(i).intValue());
            traversalCharacters[i] = (char) (traversalOrder.get(i).intValue());
        }

        int distance = LEVENSHTEIN_DISTANCE.apply(new String(originalCharacters), new String(traversalCharacters));

        return distance;
    }
}
