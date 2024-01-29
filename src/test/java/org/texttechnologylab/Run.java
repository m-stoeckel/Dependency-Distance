package org.texttechnologylab;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.mdd.DependencyDistanceEngine;
import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class Run {

    @Test
    public void GerParCor() {
        try {
            String pMongoDbConfigPath = System.getProperty("config", "src/main/resources/mongodb.ini");
            String pFilter = System.getProperty("filter", "{}");

            String pOutput = System.getProperty("output", "/storage/projects/stoeckel/syntactic-language-change/mdd/");
            boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
            CompressionMethod pCompression = CompressionMethod.valueOf(System.getProperty("compression", "NONE"));

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));

            MongoDBConfig mongoDbConfig = new MongoDBConfig(pMongoDbConfigPath);
            DUUIGerParCorReader duuiGerParCorReader = new DUUIGerParCorReader(mongoDbConfig, pFilter);

            AnalysisEngine engine = createEngine(
                    DependencyDistanceEngine.class,
                    DependencyDistanceEngine.PARAM_TARGET_LOCATION, pOutput,
                    DependencyDistanceEngine.PARAM_OVERWRITE, pOverwrite,
                    DependencyDistanceEngine.PARAM_COMPRESSION, pCompression,
                    DependencyDistanceEngine.PARAM_FAIL_ON_ERROR, pFailOnError
            );

            JCas jCas = JCasFactory.createJCas();
            while (duuiGerParCorReader.hasNext()) {
                duuiGerParCorReader.getNextCas(jCas);
                engine.process(jCas);
            }

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }
}
