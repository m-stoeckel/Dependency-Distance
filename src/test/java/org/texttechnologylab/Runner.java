package org.texttechnologylab;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUICollectionReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.mdd.engine.DependencyDistanceEngine;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;

import java.nio.file.Path;
import java.util.Objects;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class Runner {

    @Test
    public void GerParCor() {
        try {
            String pConfig = System.getProperty("config", "src/test/resources/mongodb.ini");
            String pFilter = System.getProperty("filter", "{}");
            int pScale = Integer.parseInt(System.getProperty("scale", "8"));
            int pPoolsize = Integer.parseInt(System.getProperty("poolsize", "16"));

            String pOutput = System.getProperty("output", "/storage/projects/stoeckel/syntactic-language-change/mdd/");
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
                    pConfig, pFilter, pScale, pPoolsize, pOutput, pOverwrite, pCompression, pFailOnError,
                    pFixDateYear, pMkDirs);

            Path outputPath = Path.of(pOutput);
            if (!outputPath.toFile().exists() && pMkDirs) {
                outputPath.toFile().mkdirs();
            }

            MongoDBConfig mongoDbConfig = new MongoDBConfig(pConfig);
            System.out.printf("MongoDBConfig:\n  %s\n", mongoDbConfig);

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(
                    new DUUIGerParCorReader(mongoDbConfig, pFilter));

            DUUIComposer composer = new DUUIComposer()
                    .withSkipVerification(true)
                    .withWorkers(pScale)
                    .withCasPoolsize(pPoolsize)
                    .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                    createEngineDescription(
                            DependencyDistanceEngine.class,
                            DependencyDistanceEngine.PARAM_TARGET_LOCATION, pOutput,
                            DependencyDistanceEngine.PARAM_OVERWRITE, pOverwrite,
                            DependencyDistanceEngine.PARAM_COMPRESSION, pCompression,
                            DependencyDistanceEngine.PARAM_FAIL_ON_ERROR, pFailOnError,
                            DependencyDistanceEngine.PARAM_FIX_DATE_YEAR, pFixDateYear))
                    .withScale(pScale).build();
            composer.add(dependency);

            composer.run(processor, "mDD");
            composer.shutdown();

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    public void GerParCorFile() {
        try {
            String pInput = System.getProperty("input");
            if (Objects.isNull(pInput)) {
                throw new IllegalArgumentException("-Dinput must be given!");
            }
            String pEnding = System.getProperty("ending", ".xmi.gz");

            int pScale = Integer.parseInt(System.getProperty("scale", "8"));
            int pPoolsize = Integer.parseInt(System.getProperty("poolsize", "16"));

            String pOutput = System.getProperty("output", "/tmp/mdd/");
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
                    pInput, pScale, pPoolsize, pOutput, pOverwrite, pCompression, pFailOnError, pFixDateYear, pMkDirs);

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
                            DependencyDistanceEngine.class,
                            DependencyDistanceEngine.PARAM_TARGET_LOCATION, pOutput,
                            DependencyDistanceEngine.PARAM_OVERWRITE, pOverwrite,
                            DependencyDistanceEngine.PARAM_COMPRESSION, pCompression,
                            DependencyDistanceEngine.PARAM_FAIL_ON_ERROR, pFailOnError,
                            DependencyDistanceEngine.PARAM_FIX_DATE_YEAR, pFixDateYear))
                    .withScale(pScale).build();
            composer.add(dependency);

            composer.run(processor, "mDD");
            composer.shutdown();

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }
}
