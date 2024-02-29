package org.texttechnologylab;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.dkpro.core.api.resources.CompressionMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUICollectionReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.engine.DependencyMetricsEngine;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;
import org.texttechnologylab.utils.ExpectedDocumentAnnotations;
import org.texttechnologylab.utils.ExpectedValues;

public class Runner {

    @Test
    @Tag("runnable")
    public void GerParCor() {
        try {
            String pConfig = System.getProperty("config", this.getClass().getClassLoader().getResource("mongodb.ini").getPath());
            String pFilter = System.getProperty("filter", "{}");
            int pScale = Integer.parseInt(System.getProperty("scale", "8"));
            int pPoolsize = Integer.parseInt(System.getProperty("poolsize", "16"));

            String pOutput = System.getProperty("output");
            if (Objects.isNull(pOutput)) {
                throw new IllegalArgumentException("-Doutput must be given!");
            }
            boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
            CompressionMethod pCompression = CompressionMethod.valueOf(System.getProperty("compression", "NONE"));

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));
            boolean pFixDateYear = Boolean.parseBoolean(System.getProperty("fixDateYear", "true"));
            boolean pMkDirs = Boolean.parseBoolean(System.getProperty("mkdirs", "true"));

            System.out.printf(
                "Settings:\n" +
                "  pConfig:      %s\n" +
                "  pFilter:      %s\n" +
                "  pScale:       %d\n" +
                "  pPoolsize:    %d\n" +
                "  pOutput:      %s\n" +
                "  pOverwrite:   %b\n" +
                "  pCompression: %s\n" +
                "  pFailOnError: %b\n" +
                "  pFixDateYear: %b\n" +
                "  pMkDirs:      %b\n",
                pConfig,
                pFilter,
                pScale,
                pPoolsize,
                pOutput,
                pOverwrite,
                pCompression,
                pFailOnError,
                pFixDateYear,
                pMkDirs
            );

            Path outputPath = Path.of(pOutput);
            if (!outputPath.toFile().exists() && pMkDirs) {
                outputPath.toFile().mkdirs();
            }

            MongoDBConfig mongoDbConfig = new MongoDBConfig(pConfig);
            System.out.printf("MongoDBConfig:\n  %s\n", mongoDbConfig);

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIGerParCorReader(mongoDbConfig, pFilter));

            DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(pScale)
                .withCasPoolsize(pPoolsize)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                createEngineDescription(
                    DependencyMetricsEngine.class,
                    DependencyMetricsEngine.PARAM_TARGET_LOCATION,
                    pOutput,
                    DependencyMetricsEngine.PARAM_OVERWRITE,
                    pOverwrite,
                    DependencyMetricsEngine.PARAM_COMPRESSION,
                    pCompression,
                    DependencyMetricsEngine.PARAM_FAIL_ON_ERROR,
                    pFailOnError,
                    DependencyMetricsEngine.PARAM_FIX_DATE_YEAR,
                    pFixDateYear
                )
            )
                .withScale(pScale)
                .build();
            composer.add(dependency);

            composer.run(processor, "mDD");
            composer.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Tag("runnable")
    public void GerParCorFile() {
        try {
            String pInput = System.getProperty("input");
            if (Objects.isNull(pInput)) {
                throw new IllegalArgumentException("-Dinput must be given!");
            }
            String pEnding = System.getProperty("ending", ".xmi.gz");

            int pScale = Integer.parseInt(System.getProperty("scale", "8"));
            int pPoolsize = Integer.parseInt(System.getProperty("poolsize", "16"));

            String pOutput = System.getProperty("output");
            if (Objects.isNull(pOutput)) {
                throw new IllegalArgumentException("-Doutput must be given!");
            }
            boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
            CompressionMethod pCompression = CompressionMethod.valueOf(System.getProperty("compression", "NONE"));

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));
            boolean pFixDateYear = Boolean.parseBoolean(System.getProperty("fixDateYear", "true"));
            boolean pMkDirs = Boolean.parseBoolean(System.getProperty("mkdirs", "true"));

            System.out.printf(
                "Settings:\n" +
                "  pInput:       %s\n" +
                "  pScale:       %d\n" +
                "  pPoolsize:    %d\n" +
                "  pOutput:      %s\n" +
                "  pOverwrite:   %b\n" +
                "  pCompression: %s\n" +
                "  pFailOnError: %b\n" +
                "  pFixDateYear: %b\n" +
                "  pMkDirs:      %b\n",
                pInput,
                pScale,
                pPoolsize,
                pOutput,
                pOverwrite,
                pCompression,
                pFailOnError,
                pFixDateYear,
                pMkDirs
            );

            Path outputPath = Path.of(pOutput);
            if (!outputPath.toFile().exists() && pMkDirs) {
                outputPath.toFile().mkdirs();
            }

            DUUICollectionReader reader = new DUUIFileReader(pInput, pEnding);
            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(reader);

            DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(pScale)
                .withCasPoolsize(pPoolsize)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                createEngineDescription(
                    DependencyMetricsEngine.class,
                    DependencyMetricsEngine.PARAM_TARGET_LOCATION,
                    pOutput,
                    DependencyMetricsEngine.PARAM_OVERWRITE,
                    pOverwrite,
                    DependencyMetricsEngine.PARAM_COMPRESSION,
                    pCompression,
                    DependencyMetricsEngine.PARAM_FAIL_ON_ERROR,
                    pFailOnError,
                    DependencyMetricsEngine.PARAM_FIX_DATE_YEAR,
                    pFixDateYear
                )
            )
                .withScale(pScale)
                .build();
            composer.add(dependency);

            composer.run(processor, "mDD");
            composer.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Tag("integration")
    public void testGerParCorFile(@TempDir Path tempDir) throws JsonSyntaxException, IOException {
        System.setProperty("input", this.getClass().getClassLoader().getResource("xmi/").getPath());
        System.setProperty("ending", "test-geklappt.xmi");
        System.setProperty("output", tempDir.toString());
        System.setProperty("scale", "1");
        System.setProperty("poolsize", "1");
        System.setProperty("overwrite", "false");

        GerParCorFile();

        ExpectedDocumentAnnotations eda = ExpectedDocumentAnnotations.get20211223();

        Path outputFile = Files.list(tempDir.resolve(eda.dateYear.get())).filter(p -> p.toFile().isFile()).findFirst().get();
        DocumentDataPoint dp = new Gson().fromJson(Files.readString(outputFile), DocumentDataPoint.class);

        ExpectedValues.getExpectedForGeklappt().assertEquals(dp.getSentences().get(0));
        eda.assertEquals(dp.getDocumentAnnotation());

        Assertions.assertTrue(true);
    }
}
