package org.texttechnologylab.graph;

import com.google.common.graph.ImmutableGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.dependency.graph.DependencyGraph;
import org.texttechnologylab.dependency.graph.random.RandomTree;

import java.util.HashSet;

public class DependencyGraphTest {

    @Test
    public void testRandomTree() {
        ImmutableGraph<Integer> randomGraph = RandomTree.getRandomGraph(8);

        HashSet<Integer> nodes = new HashSet<>(randomGraph.nodes());
        nodes.remove(0);

        Assertions.assertEquals(nodes, new HashSet<>(DependencyGraph.getTraversalOrder(randomGraph)));
    }

}
