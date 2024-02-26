package org.texttechnologylab;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.dependency.engine.DependencyDistanceEngine;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;

public class DependencyDistanceEngineTest {

    @Test
    public void testProcess() {
        try {
            String pInput = System.getProperty("input", "src/test/resources");
            String pEnding = System.getProperty("ending", ".xmi.gz");
            int pScale = Integer.parseInt(System.getProperty("scale", "1"));

            String pOutput = System.getProperty("output", "target/output/");

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "true"));

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIFileReader(pInput, pEnding));

            DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(pScale)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            Path path = Path.of(pOutput);
            path.toFile().mkdir();

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                createEngineDescription(
                    DependencyDistanceEngine.class,
                    DependencyDistanceEngine.PARAM_TARGET_LOCATION,
                    pOutput,
                    DependencyDistanceEngine.PARAM_OVERWRITE,
                    true,
                    DependencyDistanceEngine.PARAM_FAIL_ON_ERROR,
                    pFailOnError
                )
            )
                .withScale(pScale)
                .build();

            composer.add(dependency);
            composer.run(processor, "mDD");
            composer.shutdown();

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCompression() {
        try {
            String pInput = System.getProperty("input", "src/test/resources");
            String pEnding = System.getProperty("ending", ".xmi.gz");
            int pScale = Integer.parseInt(System.getProperty("scale", "1"));

            String pOutput = System.getProperty("output", "target/output/");

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "true"));

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIFileReader(pInput, pEnding));

            DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(pScale)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            Path path = Path.of(pOutput);
            path.toFile().mkdir();

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                createEngineDescription(
                    DependencyDistanceEngine.class,
                    DependencyDistanceEngine.PARAM_TARGET_LOCATION,
                    pOutput,
                    DependencyDistanceEngine.PARAM_OVERWRITE,
                    true,
                    DependencyDistanceEngine.PARAM_COMPRESSION,
                    CompressionMethod.BZIP2,
                    DependencyDistanceEngine.PARAM_FAIL_ON_ERROR,
                    pFailOnError
                )
            )
                .withScale(pScale)
                .build();

            composer.add(dependency);
            composer.run(processor, "mDD");
            composer.shutdown();

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
