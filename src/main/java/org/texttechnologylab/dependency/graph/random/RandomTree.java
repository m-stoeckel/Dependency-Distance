package org.texttechnologylab.dependency.graph.random;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class RandomTree {

    public static ImmutableGraph<Integer> getRandomGraph(int numberOfNodes) {
        ImmutableGraph.Builder<Integer> graphBuilder = GraphBuilder
            .directed()
            .expectedNodeCount(numberOfNodes)
            .allowsSelfLoops(false)
            .<Integer>immutable()
            .addNode(0);

        Integer[] array = new Integer[numberOfNodes];
        for (int i = 0; i < numberOfNodes; i++) {
            array[i] = i + 1; // Fill the array with integers from 1 to n
        }
        Collections.shuffle(Arrays.asList(array));

        Random rng = new Random();
        graphBuilder.putEdge(0, array[numberOfNodes - 1]);
        for (int i = 0; i < numberOfNodes - 1; i++) {
            int offset = rng.nextInt(1, numberOfNodes - i);
            int head = array[i + offset];
            graphBuilder.putEdge(head, array[i]);
        }

        return graphBuilder.build();
    }
}
