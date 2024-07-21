package org.texttechnologylab.graph.zs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.texttechnologylab.dependency.graph.zs.Tree;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class ZhangShashaTest {

    @Test
    public void testExamples() throws IOException {
        testTreeDistance("f(d(a c(b)) e)", "f(c(d(a b)) e)", 2);

        testTreeDistance("a(b(c d) e(f g(i)))", "a(b(c d) e(f g(h)))", 1);

        testTreeDistance("d", "g(h)", 2);

        testTreeDistance("node0", "node1(node2)", 2);
        testTreeDistance("node11", "node1(node2)", 2);
    }

    private static void testTreeDistance(
        String s1, String s2, int expected
    ) throws IOException {
        Tree tree1 = new Tree(s1);
        Tree tree2 = new Tree(s2);
        int distance = Tree.ZhangShasha(tree1, tree2);
        Assertions.assertEquals(expected, distance);
        System.out.printf("'%s' <> '%s' = %d\n", s1, s2, distance);
    }

    @Test
    public void testAlternative() throws IOException {
        testTreeDistance("F(D(A C(B)) E)", "F(C(D(A B)) E)", 2);
        testTreeDistance("ä(D(A C(B)) E)", "ä(C(D(A B)) E)", 2);
    }
}
