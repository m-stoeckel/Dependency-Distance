package org.texttechnologylab.graph.random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.dependency.graph.DependencyGraphStringifier;
import org.texttechnologylab.dependency.graph.random.RandomTree;
import org.texttechnologylab.dependency.graph.zs.Tree;

import java.io.IOException;

public class RandomTreeTest {

    @Test
    public void testRandomTree() {
        for (int j = 0; j < 10; j++) {
            for (int i = 1; i < 100; i++) {
                RandomTree.getRandomGraph(i);
            }
        }
    }

    @Test
    public void testRandomTreeToString() {
        for (int j = 0; j < 10; j++) {
            for (int i = 1; i < 100; i++) {
                DependencyGraphStringifier.graphToZsStringRepresentation(RandomTree.getRandomGraph(i));
            }
        }
    }

    @Test
    public void testRandomTreeToZsTree() throws IOException {
        for (int j = 0; j < 10; j++) {
            for (int i = 1; i < 100; i++) {
                new Tree(DependencyGraphStringifier.graphToZsStringRepresentation(RandomTree.getRandomGraph(i)));
            }
        }
    }
}
