package org.texttechnologylab.dependency.app;

import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.ImmutableGraph.Builder;
import com.google.gson.Gson;
import org.dkpro.core.api.resources.CompressionMethod;
import org.dkpro.core.api.resources.CompressionUtils;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.data.SentenceDataPoint;
import org.texttechnologylab.dependency.graph.InvalidDependencyGraphException;
import org.texttechnologylab.dependency.json.GraphIterator;
import org.texttechnologylab.dependency.json.GraphIteratorItem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunLiterature {

    static class PathPair {
        public Path out;
        public Path in;

        public PathPair(
            Path in, Path out
        ) {
            this.in = in;
            this.out = out;
        }
    }

    public static void main(String[] args) {
        boolean pOverwrite = false;
        CompressionMethod pCompression = CompressionMethod.NONE;
        boolean pFailOnError = false;

        ArrayList<String> fileList = new ArrayList<String>();
        Iterator<String> iterator = Arrays.stream(args).iterator();
        Optional<String> pCorpusName = Optional.empty();
        Optional<String> pPattern = Optional.empty();
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
                case "--pattern":
                    pPattern = Optional.of(iterator.next());
                    break;
                default:
                    fileList.add(argOrFlag);
            }
        }

        System.out.println(Arrays.toString(args));

        if (pCorpusName.isPresent()) {
            if (pCorpusName.get().toLowerCase().contains("coha")) {
                pPattern = pPattern.isPresent() ? pPattern : Optional.of(".*/(?<parser>[^/]+)/text_(?<subset>[^_]+)_(?<decade>\\d{4}).*");
            } else if (pCorpusName.get().toLowerCase().contains("dta")) {
                pPattern = pPattern.isPresent() ? pPattern : Optional.of(".*/(?<parser>[^/]+)/(?<subset>[^/]+/[^/]+)/(?<decade>\\d{4}).*");
            }
        } else {
            throw new IllegalArgumentException("Corpus name must be given!");
        }

        if (fileList.size() < 2) {
            throw new IllegalArgumentException(String.format(
                "Expected at least 2 files (input, output), but got %d: %s",
                fileList.size(),
                fileList
            ));
        }
        final String outputPath = fileList.remove(fileList.size() - 1);

        final boolean fOverwrite = pOverwrite;
        final boolean fFailOnError = pFailOnError;
        final CompressionMethod fCompression = pCompression;
        final String fCorpusName = pCorpusName.get();
        final String fPattern = pPattern.get();

        fileList.stream().flatMap((String pathString) -> {
            Path path = Paths.get(pathString).toAbsolutePath();
            File file = path.toFile();
            if (pathString.contains("*")) {
                final GlobVisitor visitor = new GlobVisitor(
                    pathString,
                    outputPath
                );
                try {
                    Path root = Paths.get(Arrays.stream(pathString.split("\\*")).findFirst().get());
                    Files.walkFileTree(
                        root,
                        visitor
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return visitor.stream();
            }
            if (file.isDirectory()) {
                return Stream.of(file.listFiles()).map(f -> new PathPair(
                    f.toPath(),
                    Paths.get(
                        outputPath,
                        f.getAbsolutePath().substring(path.toString().length())
                    )
                ));
            }
            return Stream.of(new PathPair(
                path,
                Paths.get(
                    outputPath,
                    file.getName()
                )
            ));
        }).parallel().map(pathPair -> process(
            pathPair.in.toFile(),
            pathPair.out.toFile(),
            fOverwrite,
            fFailOnError,
            fCompression,
            fCorpusName,
            fPattern
        )).toList();
    }

    private static boolean process(
        final File inputFile, File outputFile, final boolean pOverwrite, final boolean pFailOnError,
        final CompressionMethod pCompression, final String pCorpusName, final String pPattern
    ) {
        try {
            if (outputFile.exists() && !pOverwrite) {
                throw new IllegalArgumentException(String.format(
                    "Output file '%s' already exists and overwrite is disabled",
                    outputFile
                ));
            }

            Pattern pattern = Pattern.compile(pPattern);
            Matcher matcher = pattern.matcher(inputFile.getAbsolutePath());
            if (!matcher.matches()) {
                throw new IllegalArgumentException(String.format(
                    "File '%s' does not match pattern '%s'",
                    inputFile.getAbsolutePath(),
                    pPattern
                ));
            }

            String subset = matcher.group("subset").replace(
                '/',
                '-'
            );
            String parser = matcher.group("parser");
            String decade = matcher.group("decade");

            String documentId = pCorpusName + "/" + subset + "/" + parser + "/" + decade;
            String documentUri = "file://" + inputFile.toPath().toAbsolutePath();

            outputFile = Paths.get(
                outputFile.getParentFile().getAbsolutePath(),
                String.join(
                    "-",
                    decade,
                    subset,
                    parser
                ) + ".json" + pCompression.getExtension()
            ).toFile();


            GraphIterator graphIterator = new GraphIterator(new BufferedReader(new InputStreamReader(CompressionUtils.getInputStream(
                inputFile.getName(),
                new FileInputStream(inputFile)
            ))));

            ArrayList<NamedSentenceDataPoint> sentenceDataPoints = Streams.stream(graphIterator)
                .parallel()
                .map(item -> {
                    try {
                        return Optional.of(getSentenceDataPoint(item));
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
            documentDataPoint.getDocumentAnnotation().put(
                "file",
                inputFile.getName()
            );
            documentDataPoint.getDocumentAnnotation().put(
                "parser",
                parser
            );
            documentDataPoint.getDocumentAnnotation().put(
                "dateYear",
                decade
            );

            documentDataPoint.getDocumentMetaData().put(
                "documentId",
                documentId
            );
            documentDataPoint.getDocumentMetaData().put(
                "documentUri",
                documentUri
            );

            documentDataPoint.getSentences().addAll(sentenceDataPoints);

            System.out.printf(
                "Processed %d/%d graphs from '%s'%n",
                documentDataPoint.getSentences().size(),
                graphIterator.count(),
                inputFile.toPath()
            );

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                CompressionUtils.getOutputStream(outputFile),
                StandardCharsets.UTF_8
            ))) {
                writer.write(new Gson().toJson(documentDataPoint));

                System.out.printf(
                    "Wrote %d data points to '%s'%n",
                    documentDataPoint.getSentences().size(),
                    outputFile
                );
            }
        } catch (Exception e) {
            if (pFailOnError) {
                throw new RuntimeException(e);
            } else {
                System.err.printf(
                    "Exception while processing '%s':%n",
                    inputFile.getAbsolutePath()
                );
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private static class GlobVisitor extends SimpleFileVisitor<Path> {
        final ArrayList<String> innerList = new ArrayList<>();
        final PathMatcher matcher;
        final String root;
        final String outputPath;

        public GlobVisitor(
            final String pattern, final String outputPath
        ) {
            this.root = Paths.get(Arrays.stream(pattern.split("\\*")).findFirst().get()).toAbsolutePath().toString();
            this.outputPath = outputPath;
            this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        public FileVisitResult visitFile(
            Path file, BasicFileAttributes attrs
        ) {
            if (this.matcher.matches(file)) {
                innerList.add(file.toString());
            }
            return FileVisitResult.CONTINUE;
        }

        public Stream<PathPair> stream() {
            return innerList.stream().sorted().map(f -> {
                Path p = Paths.get(f);
                return new PathPair(
                    p,
                    Paths.get(
                        this.outputPath,
                        p.toAbsolutePath().toString().substring(this.root.length())
                    )
                );
            });
        }
    }

    private static class NamedSentenceDataPoint extends SentenceDataPoint {

        public final String textId;

        public NamedSentenceDataPoint(
            String textId, ImmutableGraph<Integer> dependencyGraph, ImmutableGraph<Integer> dependencyGraphWithPunct
        ) throws InvalidDependencyGraphException {
            super(
                dependencyGraph,
                dependencyGraphWithPunct
            );
            this.textId = textId;
        }
    }

    private static NamedSentenceDataPoint getSentenceDataPoint(
        GraphIteratorItem item
    ) throws InvalidDependencyGraphException {
        Builder<Integer> graphBuilder = GraphBuilder.directed().<Integer>immutable().addNode(0);

        for (Integer[] edge : item.dependencyEdges) {
            graphBuilder.putEdge(
                edge[0],
                edge[1]
            );
        }
        ImmutableGraph<Integer> dependencyGraph = graphBuilder.build();

        for (Integer[] edge : item.punctEdges) {
            graphBuilder.putEdge(
                edge[0],
                edge[1]
            );
        }
        ImmutableGraph<Integer> dependencyGraphWithPunct = graphBuilder.build();

        return new NamedSentenceDataPoint(
            item.textId,
            dependencyGraph,
            dependencyGraphWithPunct
        );
    }
}
