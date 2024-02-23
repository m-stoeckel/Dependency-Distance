package org.texttechnologylab.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.dkpro.core.api.resources.CompressionUtils;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.data.SentenceDataPoint;
import org.texttechnologylab.dependency.graph.InvalidDependencyGraphException;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.ImmutableGraph.Builder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

        String outputPath = fileList.remove(fileList.size() - 1);

        Type type = new TypeToken<ArrayList<ArrayList<Integer[][]>>>() {}.getType();
        Gson gson = new Gson();

        try {
            for (String fileName : fileList) {
                try {
                    Path inputPath = Paths.get(fileName);
                    File outputFile = Paths.get(outputPath, inputPath.getFileName().toString() + pCompression.getExtension()).toFile();
                    if (outputFile.exists() && !pOverwrite) {
                        throw new IllegalArgumentException(
                            String.format("Output file '%s' already exists and overwrite is disabled", outputFile)
                        );
                    }

                    String content = Files.readString(inputPath);
                    ArrayList<ArrayList<Integer[][]>> graphs = gson.fromJson(content, type);

                    System.out.printf("Loaded %d graphs from '%s'%n", graphs.size(), inputPath.toString());

                    String parser = inputPath.toFile().getParentFile().getName();
                    String dateYear = inputPath.toFile().getName().split("\\.")[0];
                    String documentId = "Bundestag/" + dateYear;
                    String documentUri = "DeuParl/" + parser + "/" + documentId;
                    DocumentDataPoint documentDataPoint = new DocumentDataPoint(
                        new TreeMap<>(Map.of("file", fileName, "dateYear", dateYear)),
                        new TreeMap<>(Map.of("documentId", documentId, "documentUri", documentUri))
                    );

                    final boolean finalPFailOnError = pFailOnError;
                    graphs
                        .stream()
                        .parallel()
                        .map(graph -> {
                            try {
                                return Optional.<SentenceDataPoint>of(getSentenceDataPoint(graph));
                            } catch (InvalidDependencyGraphException e) {
                                if (finalPFailOnError) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return Optional.<SentenceDataPoint>empty();
                        })
                        .forEach(dp -> {
                            if (dp.isPresent()) {
                                documentDataPoint.add(dp.get());
                            }
                        });

                    try (
                        BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(CompressionUtils.getOutputStream(outputFile), StandardCharsets.UTF_8)
                        )
                    ) {
                        String json = new Gson().toJson(documentDataPoint);
                        writer.write(json);
                    }
                } catch (Exception e) {
                    if (pFailOnError) {
                        throw e;
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SentenceDataPoint getSentenceDataPoint(ArrayList<Integer[][]> graph) throws InvalidDependencyGraphException {
        Builder<Integer> graphBuilder = GraphBuilder.directed().<Integer>immutable().addNode(0);
        for (Integer[] edge : graph.get(0)) {
            graphBuilder.putEdge(edge[0], edge[1]);
        }
        ImmutableGraph<Integer> dependencyGraph = graphBuilder.build();
        for (Integer[] edge : graph.get(1)) {
            graphBuilder.putEdge(edge[0], edge[1]);
        }
        ImmutableGraph<Integer> dependencyGraphWithPunct = graphBuilder.build();

        SentenceDataPoint sentenceDataPoint = new SentenceDataPoint(dependencyGraph, dependencyGraphWithPunct);
        return sentenceDataPoint;
    }

    private class JsonGraph {

        ArrayList<Integer[]> edges;
        ArrayList<Integer[]> punctEdges;
    }
}
