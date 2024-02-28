package org.texttechnologylab.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.dependency.json.GraphIterator;
import org.texttechnologylab.dependency.json.GraphIterator.GraphIteratorException;
import org.texttechnologylab.dependency.json.GraphIteratorItem;

public class GraphIteratorTest {

    final String JSON_EXAMPLE = "{\n \"1234\": [\n  [[0, 1], [0, 2], [2, 3]],\n  [[3, 4]]\n ]\n}";
    final String EXPECTED_ID = "1234";
    final Integer[][] EXPECTED_DEP_EDGES = new Integer[][] { { 0, 1 }, { 0, 2 }, { 2, 3 } };
    final Integer[][] EXPECTED_PUNCT_EDGES = new Integer[][] { { 3, 4 } };

    @Test
    public void testExample() throws IOException {
        Reader reader = new StringReader(JSON_EXAMPLE);
        GraphIterator graphIterator = new GraphIterator(reader);
        Assertions.assertTrue(graphIterator.hasNext(), "GraphIterator should have next element");
        GraphIteratorItem item = graphIterator.next();
        Assertions.assertEquals("1234", item.textId, "graphIteratorItem.textId");
        Assertions.assertArrayEquals(EXPECTED_DEP_EDGES, item.dependencyEdges, "graphIteratorItem.dependencyEdges");
        Assertions.assertArrayEquals(EXPECTED_PUNCT_EDGES, item.punctEdges, "graphIteratorItem.punctEdges");

        Assertions.assertFalse(graphIterator.hasNext(), "GraphIterator should not have next element");
        Assertions.assertThrows(
            NoSuchElementException.class,
            () -> graphIterator.next(),
            "GraphIterator.next() should throw NoSuchElementException"
        );
    }

    @Test
    public void testEmpty() throws IOException {
        Reader reader = new StringReader("{}");
        GraphIterator graphIterator = new GraphIterator(reader);
        Assertions.assertFalse(graphIterator.hasNext(), "GraphIterator should not have next element");
        Assertions.assertThrows(
            NoSuchElementException.class,
            () -> graphIterator.next(),
            "GraphIterator.next() on empty JSON should throw NoSuchElementException"
        );
    }

    @Test
    public void testOldFormat() throws IOException {
        Reader reader = new StringReader("[\n [\n  [[0, 1], [0, 2], [2, 3]],\n  [[3, 4]]\n ]]");
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new GraphIterator(reader),
            "GraphIterator(reader) with invalid data should throw IllegalStateException"
        );
    }

    @Test
    public void testInvalidMissingSecondEdges() throws IOException {
        Reader reader = new StringReader("{\n \"1234\": [\n  [[0, 1], [0, 2], [2, 3]]\n ]\n}");
        GraphIterator graphIterator = new GraphIterator(reader);
        Assertions.assertThrows(
            GraphIteratorException.class,
            () -> graphIterator.next(),
            "GraphIterator.next() with missing second edge array should throw IllegalStateException"
        );
    }
}
