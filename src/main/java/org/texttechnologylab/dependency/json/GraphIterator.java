package org.texttechnologylab.dependency.json;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Iterator for reading a list of graphs from a JSON file.
 *
 * The graphs are expected to be in the following format:
 *
 * <pre>{@code
 *[
 *  "1234": [                   // ID, usually from metadata "text_id"
 *    [[0, 1], [0, 2], [2, 3]], // dependency graph
 *    [[3, 4]]                  // punctuation edges
 *  ]
 *]
 * }</pre>
 */
public class GraphIterator implements Iterator<GraphIteratorItem> {

    private final Gson gson = new Gson();
    private final JsonReader reader;
    private final TypeAdapter<Integer[][]> edgeArrayAdapter;
    private final AtomicInteger counter = new AtomicInteger(0);

    public GraphIterator(Reader reader) throws IOException {
        this.edgeArrayAdapter = gson.getAdapter(new TypeToken<Integer[][]>() {});
        this.reader = gson.newJsonReader(reader);

        // Start the top-level object
        this.reader.beginObject();
    }

    @Override
    public boolean hasNext() {
        try {
            if (this.reader.hasNext()) {
                JsonToken peeked = this.reader.peek();
                switch (peeked) {
                    case NAME:
                        return true;
                    // case BEGIN_ARRAY:
                    // return true;
                    case END_ARRAY:
                        this.reader.endArray();
                        return this.hasNext();
                    case END_OBJECT:
                        this.reader.endObject();
                        // There is only one object in the JSON
                        return false;
                    case END_DOCUMENT:
                        return false;
                    default:
                        throw new IllegalStateException("Unexpected token! Expected JSON arrays but got: " + peeked);
                }
            }
            return false;
        } catch (IOException e) {
            throw new GraphIteratorException(e);
        }
    }

    @Override
    public GraphIteratorItem next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("No more elements");
        }
        this.counter.incrementAndGet();

        try {
            try {
                String textId = this.reader.nextName();

                try {
                    this.reader.beginArray();
                    Integer[][] dependencyEdges = this.edgeArrayAdapter.read(reader);
                    Integer[][] punctEdges = this.edgeArrayAdapter.read(reader);
                    this.reader.endArray();

                    return new GraphIteratorItem(textId, dependencyEdges, punctEdges);
                } catch (IllegalStateException e) {
                    throw new GraphIteratorException("Expected exactly two arrays of integer pairs", e);
                }
            } catch (IllegalStateException e) {
                throw new GraphIteratorException("Expected object key", e);
            }
        } catch (IOException e) {
            throw new GraphIteratorException(e);
        }
    }

    public int count() {
        return this.counter.get();
    }

    public static class GraphIteratorException extends RuntimeException {

        public GraphIteratorException(String message) {
            super(message);
        }

        public GraphIteratorException(Throwable cause) {
            super(cause);
        }

        public GraphIteratorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
