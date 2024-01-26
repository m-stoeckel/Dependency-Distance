package org.texttechnologylab.mdd;

import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.*;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.mdd.DependencyDistanceEngine;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;

import java.util.Arrays;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class Run {
    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(String.format("Expected 3 arguments, got: %s", Arrays.toString(args)));
        }
        try {
            int iScale = Integer.parseInt(args[0]);
            String configPath = args[1];
            String outputPath = args[2];

            MongoDBConfig pConfig = new MongoDBConfig(configPath);
            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIGerParCorReader(pConfig, "{}"));

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
                            DependencyDistanceEngine.PARAM_OUTPUT, outputPath
                    )
            ).withScale(iScale).build();

            composer.add(dependency);

            composer.run(processor, "mDD");

            composer.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
