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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class Runner {

    @Test
    public void GerParCor() {
        try {
            String pMongoDbConfigPath = System.getProperty("config", "src/main/resources/mongodb.ini");
            String pFilter = System.getProperty("filter", "{}");
            int pScale = Integer.parseInt(System.getProperty("scale", "8"));
            int pPoolsize = Integer.parseInt(System.getProperty("poolsize", "16"));

            String pOutput = System.getProperty("output", "/storage/projects/stoeckel/syntactic-language-change/mdd/");
            boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
            CompressionMethod pCompression = CompressionMethod.valueOf(System.getProperty("compression", "NONE"));

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "true"));

            MongoDBConfig mongoDbConfig = new MongoDBConfig(pMongoDbConfigPath);
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
                            DependencyDistanceEngine.class,
                            DependencyDistanceEngine.PARAM_TARGET_LOCATION, pOutput,
                            DependencyDistanceEngine.PARAM_OVERWRITE, pOverwrite,
                            DependencyDistanceEngine.PARAM_COMPRESSION, pCompression,
                            DependencyDistanceEngine.PARAM_FAIL_ON_ERROR, pFailOnError
                    )
            ).withScale(pScale).build();
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
            pInput = "/storage/xmi/GerParCorDownload/Germany/National/Bundestag/";
            if (Objects.isNull(pInput)) {
                throw new IllegalArgumentException("-Dinput must be given!");
            }
            String pEnding = System.getProperty("ending", ".xmi.gz");

            int pScale = Integer.parseInt(System.getProperty("scale", "8"));
            int pPoolsize = Integer.parseInt(System.getProperty("poolsize", "16"));

            String pOutput = System.getProperty("output", "/tmp/mdd/");
            pOutput = "/storage/projects/stoeckel/syntactic-language-change/mdd/";
            boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
            CompressionMethod pCompression = CompressionMethod.valueOf(System.getProperty("compression", "NONE"));

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "true"));

            List<Path> children = Files.list(Path.of(pInput)).map(Path::toFile).filter(File::isDirectory).map(File::toPath).collect(Collectors.toList());

            HashSet<DUUICollectionReader> readers = new HashSet<>();
            for (Path child : children) {
                readers.add(new DUUIFileReader(child.toString(), pEnding));
            }
            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(readers);

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
                            DependencyDistanceEngine.PARAM_FAIL_ON_ERROR, pFailOnError
                    )
            ).withScale(pScale).build();
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