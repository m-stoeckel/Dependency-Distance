package org.texttechnologylab.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.dkpro.core.api.resources.CompressionUtils;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.data.SentenceDataPoint;
import org.texttechnologylab.dependency.graph.InvalidDependencyGraphException;

import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.ImmutableGraph.Builder;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;

public class Run {

    public static void main(String[] args) {
        boolean pOverwrite = false;
        CompressionMethod pCompression = CompressionMethod.NONE;
        boolean pFailOnError = false;

        ArrayList<String> fileList = new ArrayList<String>();
        Iterator<String> iterator = Arrays.stream(args).iterator();
        while (iterator.hasNext()) {
            String argOrFlag = iterator.next();
            switch (argOrFlag) {
                case "--overwrite":
                    pOverwrite = Boolean.parseBoolean(iterator.next());
                    break;
                case "--compression":
                    pCompression = CompressionMethod.valueOf(iterator.next());
                    break;
                case "--failOnError":
                    pFailOnError = Boolean.parseBoolean(iterator.next());
                    break;
                default:
                    fileList.add(argOrFlag);
            }
        }
        if (fileList.size() < 2) {
            throw new IllegalArgumentException(
                String.format("Expected at least 2 files (input, output), but got %d: %s", fileList.size(), fileList)
            );
        }

        final boolean fOverwrite = pOverwrite;
        final boolean fFailOnError = pFailOnError;
        final CompressionMethod fCompression = pCompression;
        final String outputPath = fileList.remove(fileList.size() - 1);

        fileList
            .stream()
            .flatMap((String path) -> {
                File file = Paths.get(path).toFile();
                if (file.isDirectory()) {
                    return Stream.of(file.listFiles()).map(File::getAbsolutePath);
                }
                if (path.contains("*")) {
                    final GlobVisitor visitor = new GlobVisitor(path);
                    try {
                        Files.walkFileTree(file.getParentFile().toPath(), visitor);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return visitor.stream();
                }
                return Stream.of(file.getAbsolutePath());
            })
            .parallel()
            .forEach(fileName -> process(fileName, outputPath, fOverwrite, fFailOnError, fCompression));
    }

    private static void process(
        String fileName,
        final String outputPath,
        final boolean pOverwrite,
        final boolean pFailOnError,
        final CompressionMethod pCompression
    ) {
        try {
            Path inputPath = Paths.get(fileName);
            File outputFile = Paths.get(outputPath, inputPath.getFileName().toString() + pCompression.getExtension()).toFile();
            if (outputFile.exists() && !pOverwrite) {
                throw new IllegalArgumentException(String.format("Output file '%s' already exists and overwrite is disabled", outputFile));
            }

            String parser = inputPath.toFile().getParentFile().getName();
            String dateYear = inputPath.toFile().getName().split("\\.")[0];
            String documentId = "Bundestag/" + dateYear;
            String documentUri = "DeuParl/" + parser + "/" + documentId;

            GraphIterator graphIterator = new GraphIterator(Files.newBufferedReader(inputPath));
            ArrayList<NamedSentenceDataPoint> sentenceDataPoints = Streams
                .stream(graphIterator)
                .parallel()
                .map(textIdAndgraph -> {
                    try {
                        String textId = textIdAndgraph.getLeft();
                        ArrayList<Integer[][]> graph = textIdAndgraph.getRight();
                        return Optional.<NamedSentenceDataPoint>of(getSentenceDataPoint(textId, graph));
                    } catch (InvalidDependencyGraphException e) {
                        if (pFailOnError) {
                            throw new RuntimeException(e);
                        }
                    }
                    return Optional.<NamedSentenceDataPoint>empty();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(ArrayList::new));

            DocumentDataPoint documentDataPoint = new DocumentDataPoint();
            documentDataPoint.getDocumentAnnotation().put("file", fileName);
            documentDataPoint.getDocumentAnnotation().put("parser", parser);
            documentDataPoint.getDocumentAnnotation().put("dateYear", dateYear);

            documentDataPoint.getDocumentMetaData().put("documentId", documentId);
            documentDataPoint.getDocumentMetaData().put("documentUri", documentUri);

            documentDataPoint.getSentences().addAll(sentenceDataPoints);

            System.out.printf(
                "Processed %d/%d graphs from '%s'%n",
                documentDataPoint.getSentences().size(),
                graphIterator.count(),
                inputPath.toString()
            );

            try (
                BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(CompressionUtils.getOutputStream(outputFile), StandardCharsets.UTF_8)
                )
            ) {
                writer.write(new Gson().toJson(documentDataPoint));

                System.out.printf("Wrote data points %d to '%s'%n", documentDataPoint.getSentences().size(), inputPath.toString());
            }
        } catch (Exception e) {
            if (pFailOnError) {
                throw new RuntimeException(e);
            } else {
                System.err.printf("Exception while processing '%s':%n", fileName);
                e.printStackTrace();
            }
        }
    }

    private static class GlobVisitor extends SimpleFileVisitor<Path> {

        final ArrayList<String> innerList = new ArrayList<>();
        final PathMatcher matcher;

        public GlobVisitor(final String pattern) {
            this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (this.matcher.matches(file)) {
                innerList.add(file.toString());
            }
            return FileVisitResult.CONTINUE;
        }

        public Stream<String> stream() {
            return innerList.stream();
        }
    }

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
    private static class GraphIterator implements Iterator<ImmutablePair<String, ArrayList<Integer[][]>>> {

        private final Gson gson = new Gson();
        private final JsonReader reader;
        private final TypeAdapter<String> key;
        private final TypeAdapter<ArrayList<Integer[][]>> adapter;
        private final AtomicInteger counter = new AtomicInteger(0);

        public GraphIterator(Reader reader) throws IOException {
            this.key = gson.getAdapter(new TypeToken<String>() {});
            this.adapter = gson.getAdapter(new TypeToken<ArrayList<Integer[][]>>() {});
            this.reader = gson.newJsonReader(reader);

            // Start the top-level object
            this.reader.beginObject();
        }

        @Override
        public boolean hasNext() {
            try {
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ImmutablePair<String, ArrayList<Integer[][]>> next() {
            try {
                if (!this.hasNext()) {
                    throw new NoSuchElementException("No more elements");
                }
                this.counter.incrementAndGet();
                String textId = this.key.read(this.reader);
                ArrayList<Integer[][]> edgeArrays = this.adapter.read(reader);
                return ImmutablePair.of(textId, edgeArrays);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public int count() {
            return this.counter.get();
        }
    }

    private static class NamedSentenceDataPoint extends SentenceDataPoint {

        public final String textId;

        public NamedSentenceDataPoint(
            String textId,
            ImmutableGraph<Integer> dependencyGraph,
            ImmutableGraph<Integer> dependencyGraphWithPunct
        ) throws InvalidDependencyGraphException {
            super(dependencyGraph, dependencyGraphWithPunct);
            this.textId = textId;
        }
    }

    private static NamedSentenceDataPoint getSentenceDataPoint(String textId, ArrayList<Integer[][]> graph)
        throws InvalidDependencyGraphException {
        Builder<Integer> graphBuilder = GraphBuilder.directed().<Integer>immutable().addNode(0);

        for (Integer[] edge : graph.get(0)) {
            graphBuilder.putEdge(edge[0], edge[1]);
        }
        ImmutableGraph<Integer> dependencyGraph = graphBuilder.build();

        for (Integer[] edge : graph.get(1)) {
            graphBuilder.putEdge(edge[0], edge[1]);
        }
        ImmutableGraph<Integer> dependencyGraphWithPunct = graphBuilder.build();

        return new NamedSentenceDataPoint(textId, dependencyGraph, dependencyGraphWithPunct);
    }
}
