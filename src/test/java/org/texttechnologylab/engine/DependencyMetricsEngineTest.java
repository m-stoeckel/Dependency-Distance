package org.texttechnologylab.engine;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.dependency.engine.DependencyMetricsEngine;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;

public class DependencyMetricsEngineTest {

    @Test
    public void testEngine(@TempDir Path tempDir) {
        testEngine(tempDir, CompressionMethod.NONE);
    }

    @Test
    public void testCompressionGZip(@TempDir Path tempDir) {
        testEngine(tempDir, CompressionMethod.GZIP);
    }

    @Test
    public void testCompressionBZip2(@TempDir Path tempDir) {
        testEngine(tempDir, CompressionMethod.BZIP2);
    }

    @Test
    public void testCompressionXZ(@TempDir Path tempDir) {
        testEngine(tempDir, CompressionMethod.XZ);
    }

    public void testEngine(Path outputDir, CompressionMethod compressionMethod) {
        try {
            String inputDir = DependencyMetricsEngineTest.class.getClassLoader().getResource("xmi/").getPath();
            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIFileReader(inputDir, ".xmi.gz"));

            DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(1)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                createEngineDescription(
                    DependencyMetricsEngine.class,
                    DependencyMetricsEngine.PARAM_TARGET_LOCATION,
                    outputDir.toString(),
                    DependencyMetricsEngine.PARAM_OVERWRITE,
                    true,
                    DependencyMetricsEngine.PARAM_COMPRESSION,
                    compressionMethod,
                    DependencyMetricsEngine.PARAM_FAIL_ON_ERROR,
                    true
                )
            )
                .withScale(1)
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
