package org.texttechnologylab.engine;

import io.azam.ulidj.ULID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.mdd.data.DocumentDataPoint;
import org.texttechnologylab.mdd.engine.DependencyDistanceEngine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DummyEngine extends DependencyDistanceEngine {
    public DocumentDataPoint documentDataPoint;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            final DocumentDataPoint documentDataPoint = DocumentDataPoint.fromJCas(jCas);

            String metaHash = documentDataPoint.getMetaHash();
            if (pUlidSuffix)
                metaHash += "-" + ULID.random();
            try {
                NamedOutputStream outputStream = getOutputStream(metaHash, ".json");

                processDocument(jCas, documentDataPoint);
                this.documentDataPoint = documentDataPoint;

                save(documentDataPoint, outputStream);
            } catch (IOException e) {
                // Expected: getOutputStream() failed, most likely because the target file
                // already exists
                getLogger().error(e.getMessage());
                if (e.getMessage().contains("already exists")) {
                    File f = new File(getTargetLocation() + "/" + metaHash + ".json");
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        char[] cbuf = new char[256];
                        String sbuf = "";
                        while (br.read(cbuf) > 0) {
                            sbuf = sbuf + new String(cbuf);
                            if (sbuf.contains("\"sentences\"")) {
                                break;
                            }
                        }
                        sbuf = sbuf.substring(0, sbuf.indexOf("\"sentences\"") - 1) + "}";
                        DocumentDataPoint oldDataPoint = new Gson().fromJson(sbuf, DocumentDataPoint.class);

                        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
                        getLogger().error(String.format(
                                "Comparison of old and new data points:\nOld:\n%s\n\nNew:\n%s",
                                gson.toJson(oldDataPoint), gson.toJson(documentDataPoint)));
                    }
                }
                if (pFailOnError)
                    throw new AnalysisEngineProcessException(e);
            }
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            e.printStackTrace();
            throw new AnalysisEngineProcessException(e);
        }
    }
}
