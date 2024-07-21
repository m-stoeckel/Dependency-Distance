package org.texttechnologylab.dependency.graph;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.google.common.collect.Range;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.Traverser;
import org.jetbrains.annotations.NotNull;
import org.texttechnologylab.dependency.graph.random.RandomTree;
import org.texttechnologylab.dependency.graph.zs.Tree;

public class DependencyGraph {

    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = new LevenshteinDistance();
    public final ImmutableGraph<Integer> dependencyGraph;
    public final ImmutableGraph<Integer> dependencyGraphWithPunct;

    public DependencyGraph(
        ImmutableGraph<Integer> dependencyGraph, ImmutableGraph<Integer> dependencyGraphWithPunct
    ) throws InvalidDependencyGraphException {
        if (dependencyGraph.outDegree(0) < 0) {
            throw new InvalidDependencyGraphException("Dependency graph is empty!");
        }

        this.dependencyGraph = dependencyGraph;
        this.dependencyGraphWithPunct = dependencyGraphWithPunct;
    }

    /**
     * Calculates the length of the sentence without punctuation marks as the number
     * of edges in the dependency graph.
     *
     * @return Length of the sentence without punctuation marks.
     */
    public int getSentenceLength() {
        return this.dependencyGraph.edges().size();
    }

    /**
     * Calculates the length of the sentence without the root node
     * as the number of nodes in the dependency graph.
     *
     * @return Length of the sentence without the root node.
     */
    public int getNumberOfSyntacticLinks() {
        return this.dependencyGraph.edges().size() - 1;
    }

    /**
     * Calculates the root distance as the word index of the root node in the
     * sentence.
     *
     * @return The word index of the root node in the sentence.
     */
    public int rootDistance() {
        return this.dependencyGraph.successors(0).stream().findFirst().get();
    }

    /**
     * Calculates the distances between the nodes in the dependency graph.
     * The distances are ordered by the dependent id.
     *
     * @return A list of the distances between the nodes in the dependency graph.
     */
    public ArrayList<Integer> getDependencyDistances() {
        return this.dependencyGraph
            .edges()
            .stream()
            .filter(edge -> edge.source() > 0)
            .sorted(Comparator.comparingInt(EndpointPair::target))
            .map(edge -> {
                return Math.abs(edge.source() - edge.target());
            })
            .collect(Collectors.toCollection(ArrayList<Integer>::new));
    }

    /**
     * Calculates the sum of the distances between the nodes in the dependency
     * graph.
     *
     * @return The sum of the distances between the nodes in the dependency graph.
     */
    public int getDependencyDistanceSum() {
        return this.dependencyGraph.edges().stream().filter(edge -> edge.source() > 0).map(edge -> {
            return Math.abs(edge.source() - edge.target());
        }).reduce(0, (a, b) -> a + b);
    }

    /**
     * Calculates the mean dependency distance as the sum of the distances between
     * the
     * nodes in the dependency graph divided by the number of syntactic links.
     *
     * @return The mean dependency distance.
     * @see #getDependencyDistanceSum()
     * @see #getNumberOfSyntacticLinks()
     */
    public double mdd() {
        return this.getDependencyDistanceSum() / (double) this.getNumberOfSyntacticLinks();
    }

    /**
     * Calculates the normalized dependency distance as the absolute value of the
     * natural logarithm of the mean dependency distance divided by the square root
     * of the product of the root distance and the sentence length.
     *
     * @return The normalized dependency distance.
     * @see #mdd()
     * @see #rootDistance()
     * @see #getSentenceLength()
     */
    public double ndd() {
        return Math.abs(Math.log(this.mdd() / Math.sqrt(this.rootDistance() * this.getSentenceLength())));
    }

    /**
     * Calculates the number of crossing edges in the dependency graph.
     *
     * @return The number of crossing edges in the dependency graph.
     * @see #getNumberOfCrossings(EndpointPair)
     * getNumberOfCrossings(EndpointPair), which is used to calculate the
     * number of crossings of a single edge.
     */
    public int crossings() {
        return (this.dependencyGraph
            .edges()
            .stream()
            .filter(edge -> edge.source() > 0)
            .map(edge -> this.getNumberOfCrossings(edge))
            .reduce(0, (a, b) -> a + b) / 2);
    }

    /**
     * Calculates the number of crossings of a single edge in this dependency graph.
     * Two edges are considered to cross one edges starts within the range of the
     * and ends outside the range of the other edge.
     *
     * @param u The edge for which the number of crossings is calculated.
     * @return The number of crossings of the edge u.
     */
    private Integer getNumberOfCrossings(EndpointPair<Integer> u) {
        final Range<Integer> uRange = openRangeFromEdge(u);
        return (int) this.dependencyGraph
            .edges()
            .stream()
            .filter(edge -> edge.source() > 0)
            .filter(v -> u.source() != v.source() && u.source() != v.target() && u.target() != v.target() && u.target() != v.source() && uRange.contains(
                v.source()) != uRange.contains(v.target()))
            .count();
    }

    private static Range<Integer> openRangeFromEdge(EndpointPair<Integer> u) {
        return Range.open(Math.min(u.source(), u.target()), Math.max(u.source(), u.target()));
    }

    /**
     * Calculates the weightest longest dependency path in the dependency graph
     * starting from the root node 0 where the weights are the distances between the
     * nodes in the sentence.
     *
     * @return The length of the longest dependency path in the dependency graph
     * from the root node.
     * @see #longestPath()
     * @see #calcLongestPath(Integer)
     */
    public int dependencyHeight() {
        return this.calcLongestPath(0);
    }

    /**
     * Calculates the weighted longest dependency path in the dependency graph
     * starting from the root node 0 where the weights are the distances between the
     * nodes in the sentence.
     *
     * @return The length of the longest dependency path in the dependency graph
     * from the root node.
     * @see #dependencyHeight()
     * @see #calcLongestPath(Integer)
     */
    public int longestPath() {
        return this.calcLongestPath(0);
    }

    /**
     * Recursively calculates the length of the longest dependency path in the
     * dependency graph starting from the given node.
     *
     * @param node The starting node of the longest dependency path.
     * @return The length of the longest dependency path from the given node.
     */
    private int calcLongestPath(Integer node) {
        return this.dependencyGraph
            .successors(node)
            .stream()
            .map(successor -> Math.abs(node - successor) + this.calcLongestPath(successor))
            .max(Comparator.naturalOrder())
            .orElse(0);
    }

    /**
     * Calculates the mean dependency depth as the average of the dependency depths
     * of all nodes in the dependency graph.
     * The dependency depth of a node is the length of the unweigthed longest path
     * in the dependency tree starting from the root node.
     *
     * @return The mean dependency depth.
     * @see #depthVariance()
     * @see #streamDependencyDepthFrom(Integer, Integer)
     */
    public double depthMean() {
        return this.streamDependencyDepthFrom(0, 0).mapToDouble(Double::valueOf).average().getAsDouble();
    }

    /**
     * Calculates the variance of the dependency depths of all nodes in the
     * dependency graph.
     *
     * @return The variance of the dependency depths.
     * @see #depthMean()
     * @see #streamDependencyDepthFrom(Integer, Integer)
     */
    public double depthVariance() {
        List<Integer> depths = this.streamDependencyDepthFrom(0, 0).collect(Collectors.toList());
        double mean = depths.stream().mapToDouble(Double::valueOf).average().getAsDouble();
        return depths
            .stream()
            .map(depth -> Math.pow(depth - mean, 2))
            .mapToDouble(Double::valueOf)
            .average()
            .getAsDouble();
    }

    /**
     * Calculates the height of the dependency tree as the length of the longest
     * path in the dependency graph starting from the root node.
     * This is equal to the maximum dependency depth plus one.
     *
     * @return The height of the dependency tree.
     * @see #streamDependencyDepthFrom(Integer, Integer)
     */
    public int treeHeight() {
        return this.streamDependencyDepthFrom(0, 1).max(Comparator.naturalOrder()).get();
    }

    /**
     * Recursively streams the dependency depths of all nodes in the dependency
     * graph starting from the given node.
     *
     * @param node  The starting node.
     * @param depth The depth of the given node.
     * @return A stream of the dependency depths of all nodes in the dependency
     * graph starting from the given node.
     */
    private Stream<Integer> streamDependencyDepthFrom(
        Integer node, Integer depth
    ) {
        return this.dependencyGraph
            .successors(node)
            .stream()
            .flatMap(successor -> Stream.concat(Stream.of(depth),
                                                this.streamDependencyDepthFrom(successor, depth + 1)
            ));
    }

    /**
     * Calculates the number of leaves in the dependency graph.
     *
     * @return The number of leaves in the dependency graph.
     * @see #recurseLeaves(Integer)
     */
    public int leaves() {
        return this.recurseLeaves(0);
    }

    /**
     * Recursively calculates the number of leaves in the dependency graph starting
     * from the given node.
     *
     * @param node The starting node.
     * @return The number of leaves in the dependency graph starting from the given
     * node.
     */
    private int recurseLeaves(Integer node) {
        return this.dependencyGraph
            .successors(node)
            .stream()
            .flatMap(successor -> this.dependencyGraph
                .successors(successor)
                .isEmpty() ? Stream.of(1) : Stream.of(this.recurseLeaves(successor)))
            .reduce(0, (a, b) -> a + b);
    }

    /**
     * Calculates the degree of the dependency tree as the maximum degree of all
     * nodes in the dependency graph, excluding the root node.
     *
     * @return The tree degree.
     * @see #streamNodeDegree()
     */
    public int treeDegree() {
        return streamNodeDegree().skip(1).max(Comparator.naturalOrder()).get();
    }

    /**
     * Calculates the mean degree of the dependency tree as the average of the
     * degrees of all nodes in the dependency graph, excluding the root node.
     *
     * @return The mean tree degree.
     * @see #treeDegreeVariance()
     * @see #streamNodeDegree()
     */
    public double treeDegreeMean() {
        return streamNodeDegree().skip(1).mapToDouble(Double::valueOf).average().getAsDouble();
    }

    /**
     * Calculates the variance of the degrees of all nodes in the dependency graph,
     * excluding the root node.
     *
     * @return The variance of the degrees of all nodes in the dependency graph.
     * @see #treeDegreeMean()
     * @see #streamNodeDegree()
     */
    public double treeDegreeVariance() {
        List<Integer> degrees = streamNodeDegree().skip(1).collect(Collectors.toList());
        double mean = degrees.stream().mapToDouble(Double::valueOf).average().getAsDouble();
        return degrees
            .stream()
            .map(degree -> Math.pow(degree - mean, 2))
            .mapToDouble(Double::valueOf)
            .average()
            .getAsDouble();
    }

    /**
     * Streams the degrees of all nodes in the dependency graph.
     *
     * @return A stream of the degrees of all nodes in the dependency graph.
     */
    private Stream<Integer> streamNodeDegree() {
        return this.dependencyGraph.nodes().stream().map(node -> this.dependencyGraph.outDegree(node));
    }

    /**
     * Calculates the ratio of head-final relations to head-initial relations in the
     * dependency graph.
     *
     * @return The head-final ratio.
     */
    public double headFinalRatio() {
        return this.dependencyGraph
            .nodes()
            .stream()
            .skip(1)
            .map(head -> this.dependencyGraph
                .successors(head)
                .stream()
                .mapToDouble(dependent -> head > dependent ? 1. : 0.)
                .average())
            .filter(OptionalDouble::isPresent)
            .mapToDouble(OptionalDouble::getAsDouble)
            .average()
            .getAsDouble();
    }

    /**
     * Calculates the head-final distance as the Levenshtein distance between
     * order of the words in the sentence and the order of the words in the
     * dependency graph, when traversed from the root node in depth-first
     * pre-order.
     *
     * @return The head-final distance.
     */
    public int headFinalDistance() {
        ImmutableGraph<Integer> graph = this.dependencyGraphWithPunct;

        Integer rootNode = getRootNode(graph);

        final ArrayList<Integer> traversalOrder = getTraversalOrder(graph, rootNode);

        // Sort to obtain regular word order in sentence
        final ArrayList<Integer> wordOrder = new ArrayList<>(traversalOrder);
        wordOrder.sort(Comparator.naturalOrder());

        // Render node IDs as characters to obtain String representations of the orders
        char[] originalCharacters = new char[wordOrder.size()];
        char[] traversalCharacters = new char[traversalOrder.size()];
        for (int i = 0; i < traversalOrder.size(); i++) {
            originalCharacters[i] = (char) wordOrder.get(i).intValue();
            traversalCharacters[i] = (char) traversalOrder.get(i).intValue();
        }

        // Calculate the Levenshtein (edit) distance between the orders
        return LEVENSHTEIN_DISTANCE.apply(new String(originalCharacters), new String(traversalCharacters));
    }

    @NotNull
    private Integer getRootNode() {
        return getRootNode(this.dependencyGraphWithPunct);
    }

    @NotNull
    public static Integer getRootNode(ImmutableGraph<Integer> graph) {
        return graph.successors(0).stream().findFirst().get();
    }

    @NotNull
    public static ArrayList<Integer> getTraversalOrder(
        final ImmutableGraph<Integer> graph
    ) {
        return getTraversalOrder(graph, getRootNode(graph));
    }

    @NotNull
    /**
     * Traverse the dependency graph in depth-first pre-order starting from the root node, and collect the order of the nodes.
     */ public static ArrayList<Integer> getTraversalOrder(
        final ImmutableGraph<Integer> graph, Integer rootNode
    ) {
        final ArrayList<Integer> traversalOrder = new ArrayList<>(graph.nodes().size());
        Traverser.forGraph(graph).depthFirstPreOrder(rootNode).forEach(node -> traversalOrder.add(node));
        return traversalOrder;
    }

    public int randomTreeDistance() throws InvalidDependencyGraphException {
        try {
            String treeString = this.graphToZsStringRepresentation();
            Tree zsTree = new Tree(treeString);
            int treeSize = this.dependencyGraph.nodes().size() - 1;
            int maxTries = 16;
            for (int i = 0; i < maxTries; i++) {
                try {
                    String randomTreeString = DependencyGraphStringifier.graphToZsStringRepresentation(RandomTree.getRandomGraph(
                        treeSize));
                    try {
                        Tree zsRandomTree = new Tree(randomTreeString);
                        try {
                            return Tree.ZhangShasha(zsTree, zsRandomTree);
                        } catch (Exception e) {
                            throw new RuntimeException(String.format("Failed to compare trees:\n%s\n%s",
                                                                     treeString,
                                                                     randomTreeString
                            ), e);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to stringfy random tree", e);
                    }
                } catch (Exception e) {
                    // Ignore any exception here...
                    throw new RuntimeException("Failed to generate random tree", e);
                }
            }
            String message = String.format("Failed to generate a valid random tree of length %d in %d tries!",
                                           treeSize,
                                           maxTries
            );
            throw new RuntimeException(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String graphToZsStringRepresentation() {
        return DependencyGraphStringifier.graphToZsStringRepresentation(this.dependencyGraph);
    }

}
