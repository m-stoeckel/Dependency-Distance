package org.texttechnologylab.test;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.mdd.data.DocumentDataPoint;
import org.texttechnologylab.mdd.engine.DependencyDistanceEngine;

public class DummyEngine extends DependencyDistanceEngine {
    public DocumentDataPoint documentDataPoint;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            DocumentDataPoint documentDataPoint = processCas(jCas);
            this.documentDataPoint = documentDataPoint;
            save(documentDataPoint);
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            e.printStackTrace();
            throw new AnalysisEngineProcessException(e);
        }
    }
}
