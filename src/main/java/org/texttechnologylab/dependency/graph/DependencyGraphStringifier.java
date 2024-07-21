package org.texttechnologylab.dependency.graph;

import com.google.common.graph.ImmutableGraph;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyGraphStringifier {

    public static String graphToZsStringRepresentation(
        final ImmutableGraph<Integer> graph
    ) {
        return graphToZsStringRepresentation(graph, 0);
    }

    public static String graphToZsStringRepresentation(
        final ImmutableGraph<Integer> graph, int rootNode
    ) {
        return graph.successors(rootNode).stream().flatMap(node -> {
            String nodeString = String.format("node%d", node);
            if (graph.successors(node).isEmpty()) {
                return Stream.of(nodeString);
            } else {
                String subTree = graphToZsStringRepresentation(graph, node);
                return Stream.of(String.format("%s(%s)", nodeString, subTree));
            }
        }).collect(Collectors.joining(" "));
    }
}
