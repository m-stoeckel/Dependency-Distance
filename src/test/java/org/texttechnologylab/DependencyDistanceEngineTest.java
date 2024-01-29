package org.texttechnologylab;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.*;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.mdd.DependencyDistanceEngine;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;

import java.nio.file.Path;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class DependencyDistanceEngineTest {

    @Test
    public void runGerParCor() {
        try {
            int pScale = Integer.parseInt(System.getProperty("scale", "8"));
            String pMongoDbConfigPath = System.getProperty("config", "src/main/resources/mongodb.ini");
            String pFilter = System.getProperty("filter", "{}");
            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));
            String pOutput = System.getProperty("output", "/storage/projects/stoeckel/syntactic-language-change/mdd/");

            MongoDBConfig mongoDbConfig = new MongoDBConfig(pMongoDbConfigPath);
            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIGerParCorReader(mongoDbConfig, pFilter));

            DUUIComposer composer = new DUUIComposer()
                    .withSkipVerification(true)
                    .withWorkers(pScale)
                    .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIDockerDriver dockerDriver = new DUUIDockerDriver();
            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            DUUIRemoteDriver remoteDriver = new DUUIRemoteDriver();
            DUUISwarmDriver swarmDriver = new DUUISwarmDriver();
            composer.addDriver(dockerDriver, remoteDriver, uimaDriver, swarmDriver);
            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                    createEngineDescription(
                            DependencyDistanceEngine.class,
                            DependencyDistanceEngine.PARAM_OUTPUT, pOutput,
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
    public void testFolder() {
        try {
            int pScale = Integer.parseInt(System.getProperty("scale", "1"));
            String pInput = System.getProperty("input", "src/test/resources");
            String pEnding = System.getProperty("ending", ".xmi.gz");
            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));
            String pOutput = System.getProperty("output", "target/output/");

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIFileReader(pInput, pEnding));

            DUUIComposer composer = new DUUIComposer()
                    .withSkipVerification(true)
                    .withWorkers(pScale)
                    .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIDockerDriver dockerDriver = new DUUIDockerDriver();
            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            DUUIRemoteDriver remoteDriver = new DUUIRemoteDriver();
            DUUISwarmDriver swarmDriver = new DUUISwarmDriver();
            composer.addDriver(dockerDriver, remoteDriver, uimaDriver, swarmDriver);

            Path path = Path.of(pOutput);
            path.toFile().mkdir();

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                    createEngineDescription(
                            DependencyDistanceEngine.class,
                            DependencyDistanceEngine.PARAM_OUTPUT, pOutput,
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
