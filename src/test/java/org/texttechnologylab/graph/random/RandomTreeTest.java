package org.texttechnologylab.graph.random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.dependency.graph.DependencyGraphStringifier;
import org.texttechnologylab.dependency.graph.random.RandomTree;
import org.texttechnologylab.dependency.graph.zs.Tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Test
    public void testRandomTreeZhangShashaParallel() throws IOException {
        ArrayList<Integer> numbers = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            for (int i = 1; i < 100; i++) {
                numbers.add(i);
            }
        }
        List<Integer> resultList = numbers.stream().parallel().map(i -> {
            try {
                Tree tree1 = new Tree(DependencyGraphStringifier.graphToZsStringRepresentation(RandomTree.getRandomGraph(
                    i)));
                Tree tree2 = new Tree(DependencyGraphStringifier.graphToZsStringRepresentation(RandomTree.getRandomGraph(
                    i)));
                return Tree.ZhangShasha(tree1, tree2);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
