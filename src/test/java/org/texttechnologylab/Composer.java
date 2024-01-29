package org.texttechnologylab;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.mdd.DependencyDistanceEngine;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class Composer {

    @Test
    public void GerParCor() {
        try {
            String pMongoDbConfigPath = System.getProperty("config", "src/main/resources/mongodb.ini");
            String pFilter = System.getProperty("filter", "{}");
            int pScale = Integer.parseInt(System.getProperty("scale", "8"));

            String pOutput = System.getProperty("output", "/storage/projects/stoeckel/syntactic-language-change/mdd/");
            boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
            CompressionMethod pCompression = CompressionMethod.valueOf(System.getProperty("compression", "NONE"));

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));

            MongoDBConfig mongoDbConfig = new MongoDBConfig(pMongoDbConfigPath);
            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIGerParCorReader(mongoDbConfig, pFilter));

            DUUIComposer composer = new DUUIComposer()
                    .withSkipVerification(true)
                    .withWorkers(pScale)
                    .withCasPoolsize(4 * pScale)
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
