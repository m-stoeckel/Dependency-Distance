package org.texttechnologylab.dependency.app;

import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.ImmutableGraph.Builder;
import com.google.gson.Gson;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dkpro.core.api.resources.CompressionUtils;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.data.SentenceDataPoint;
import org.texttechnologylab.dependency.graph.InvalidDependencyGraphException;
import org.texttechnologylab.dependency.json.GraphIterator;
import org.texttechnologylab.dependency.json.GraphIteratorItem;

public class Run {

    public static void main(String[] args) {
        boolean pOverwrite = false;
        CompressionMethod pCompression = CompressionMethod.NONE;
        boolean pFailOnError = false;

        ArrayList<String> fileList = new ArrayList<String>();
        Iterator<String> iterator = Arrays.stream(args).iterator();
        Optional<String> pCorpusName = Optional.empty();
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
                case "--corpus":
                    pCorpusName = Optional.of(iterator.next());
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
        final String outputPath = fileList.remove(fileList.size() - 1);

        final boolean fOverwrite = pOverwrite;
        final boolean fFailOnError = pFailOnError;
        final CompressionMethod fCompression = pCompression;
        final Optional<String> fCorpusName = pCorpusName;

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
            .forEach(fileName -> process(fileName, outputPath, fOverwrite, fFailOnError, fCompression, fCorpusName));
    }

    private static void process(
        String fileName,
        final String outputPath,
        final boolean pOverwrite,
        final boolean pFailOnError,
        final CompressionMethod pCompression,
        final Optional<String> pCorpusName
    ) {
        try {
            Path inputPath = Paths.get(fileName);
            File outputFile = Paths.get(outputPath, inputPath.getFileName().toString() + pCompression.getExtension()).toFile();
            if (outputFile.exists() && !pOverwrite) {
                throw new IllegalArgumentException(String.format("Output file '%s' already exists and overwrite is disabled", outputFile));
            }

            String parser = inputPath.toFile().getParentFile().getName();
            String dateYear = inputPath.toFile().getName().split("\\.")[0];
            String documentId = pCorpusName.isPresent() ? pCorpusName.get() + "/" + parser + "/" + dateYear : parser + "/" + dateYear;
            String documentUri = "file://" + inputPath.toAbsolutePath().toString();

            GraphIterator graphIterator = new GraphIterator(Files.newBufferedReader(inputPath));
            ArrayList<NamedSentenceDataPoint> sentenceDataPoints = Streams
                .stream(graphIterator)
                .parallel()
                .map(item -> {
                    try {
                        return Optional.<NamedSentenceDataPoint>of(getSentenceDataPoint(item));
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

                System.out.printf("Wrote %d data points to '%s'%n", documentDataPoint.getSentences().size(), outputFile.toString());
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

    private static NamedSentenceDataPoint getSentenceDataPoint(GraphIteratorItem item) throws InvalidDependencyGraphException {
        Builder<Integer> graphBuilder = GraphBuilder.directed().<Integer>immutable().addNode(0);

        for (Integer[] edge : item.dependencyEdges) {
            graphBuilder.putEdge(edge[0], edge[1]);
        }
        ImmutableGraph<Integer> dependencyGraph = graphBuilder.build();

        for (Integer[] edge : item.punctEdges) {
            graphBuilder.putEdge(edge[0], edge[1]);
        }
        ImmutableGraph<Integer> dependencyGraphWithPunct = graphBuilder.build();

        return new NamedSentenceDataPoint(item.textId, dependencyGraph, dependencyGraphWithPunct);
    }
}
