package org.texttechnologylab.json;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.dependency.json.GraphIterator;
import org.texttechnologylab.dependency.json.GraphIterator.GraphIteratorException;
import org.texttechnologylab.dependency.json.GraphIteratorItem;

public class GraphIteratorTest {

    final String EXPECTED_ID = "1234";
    final Integer[][] EXPECTED_DEP_EDGES = new Integer[][] { { 0, 1 }, { 0, 2 }, { 2, 3 } };
    final Integer[][] EXPECTED_PUNCT_EDGES = new Integer[][] { { 3, 4 } };

    @Test
    public void testExample() throws IOException {
        GraphIterator graphIterator = new GraphIterator(
            new InputStreamReader(GraphIteratorTest.class.getClassLoader().getResourceAsStream("json/test-example.json"))
        );
        Assertions.assertTrue(graphIterator.hasNext(), "GraphIterator should have next element");
        GraphIteratorItem item = graphIterator.next();
        Assertions.assertEquals(EXPECTED_ID, item.textId, "graphIteratorItem.textId");
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
    public void testEmptyObject() throws IOException {
        GraphIterator graphIterator = new GraphIterator(new StringReader("{}"));
        Assertions.assertFalse(graphIterator.hasNext(), "GraphIterator should not have next element");
        Assertions.assertThrows(
            NoSuchElementException.class,
            () -> graphIterator.next(),
            "GraphIterator.next() on empty JSON should throw NoSuchElementException"
        );
    }

    @Test
    public void testEmptyString() throws IOException {
        Reader reader = new StringReader("");
        Assertions.assertThrows(
            EOFException.class,
            () -> new GraphIterator(reader),
            "GraphIterator.next() on empty JSON should throw NoSuchElementException"
        );
    }

    @Test
    public void testOldFormat() throws IOException {
        Reader reader = new InputStreamReader(
            GraphIteratorTest.class.getClassLoader().getResourceAsStream("json/test-invalid-old_format.json")
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new GraphIterator(reader),
            "GraphIterator(reader) with invalid format should throw IllegalStateException"
        );
    }

    @Test
    public void testInvalidMissingSecondEdges() throws IOException {
        Reader reader = new InputStreamReader(
            GraphIteratorTest.class.getClassLoader().getResourceAsStream("json/test-invalid-missing_punct.json")
        );
        GraphIterator graphIterator = new GraphIterator(reader);
        Assertions.assertThrows(
            GraphIteratorException.class,
            () -> graphIterator.next(),
            "GraphIterator.next() with missing second edge array should throw IllegalStateException"
        );
    }
}
