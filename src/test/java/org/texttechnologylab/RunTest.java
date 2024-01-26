package org.texttechnologylab;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.*;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.mdd.DependencyDistanceEngine;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class RunTest {

    @Test
    public void testMDD() {
        try {
            MongoDBConfig pConfig = new MongoDBConfig("src/main/resources/mongodb.ini");
            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIGerParCorReader(pConfig, "{}"));

            int iScale = 8;

            DUUIComposer composer = new DUUIComposer()
                    .withSkipVerification(true)
                    .withWorkers(iScale)
                    .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIDockerDriver dockerDriver = new DUUIDockerDriver();
            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            DUUIRemoteDriver remoteDriver = new DUUIRemoteDriver();
            DUUISwarmDriver swarmDriver = new DUUISwarmDriver();
            composer.addDriver(dockerDriver, remoteDriver, uimaDriver, swarmDriver);

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                    createEngineDescription(
                            DependencyDistanceEngine.class,
                            DependencyDistanceEngine.PARAM_OUTPUT,
//                            "/tmp/mDD/"
                            "/storage/projects/stoeckel/syntactic-language-change/mdd/"
                    )
            ).withScale(iScale).build();

            composer.add(dependency);

            composer.run(processor, "mDD");
//            composer.shutdown();
            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }
}
