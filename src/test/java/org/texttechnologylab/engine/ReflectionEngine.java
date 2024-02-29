package org.texttechnologylab.engine;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.engine.DependencyMetricsEngine;

import io.azam.ulidj.ULID;

public class ReflectionEngine extends DependencyMetricsEngine {

    public DocumentDataPoint documentDataPoint;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            final DocumentDataPoint documentDataPoint = DocumentDataPoint.fromJCas(jCas);

            String dateYear = getDateYear(documentDataPoint);

            String metaHash = documentDataPoint.getMetaHash();
            if (pUlidSuffix) metaHash += "-" + ULID.random();

            NamedOutputStream outputStream = getOutputStream(metaHash, ".json");

            processDocument(jCas, documentDataPoint);
            this.documentDataPoint = documentDataPoint;

            save(documentDataPoint, outputStream);
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            e.printStackTrace();
            throw new AnalysisEngineProcessException(e);
        }
    }
}
