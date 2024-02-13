package org.texttechnologylab.engine;

import io.azam.ulidj.ULID;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.mdd.data.DocumentDataPoint;
import org.texttechnologylab.mdd.engine.DependencyDistanceEngine;

public class DummyEngine extends DependencyDistanceEngine {

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
