package org.texttechnologylab.mdd.data;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import org.apache.commons.codec.binary.Hex;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.annotation.DocumentAnnotation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DocumentDataPoint {
    protected final Map<String, String> documentAnnotation;
    protected final Map<String, String> documentMetaData;
    protected final List<SentenceDataPoint> sentences;

    public DocumentDataPoint(DocumentAnnotation documentAnnotation, DocumentMetaData documentMetaData) {
        this.documentAnnotation = new TreeMap<>();
        for (Feature feature : documentAnnotation.getType().getFeatures()) {
            try {
                String featureValueAsString = documentAnnotation.getFeatureValueAsString(feature);
                if (Objects.nonNull(featureValueAsString))
                    this.documentAnnotation.put(feature.getShortName(), featureValueAsString);
            } catch (CASRuntimeException ignored) {
            }
        }
        this.documentMetaData = new TreeMap<>();
        for (Feature feature : documentMetaData.getType().getFeatures()) {
            try {
                String featureValueAsString = documentMetaData.getFeatureValueAsString(feature);
                if (Objects.nonNull(featureValueAsString))
                    this.documentMetaData.put(feature.getShortName(), featureValueAsString);
            } catch (CASRuntimeException ignored) {
            }
        }
        this.sentences = new ArrayList<>();
    }

    public static DocumentDataPoint fromJCas(JCas jCas) {
        DocumentAnnotation documentAnnotation = JCasUtil.selectSingle(jCas, DocumentAnnotation.class);
        DocumentMetaData documentMetaData = JCasUtil.selectSingle(jCas, DocumentMetaData.class);
        return new DocumentDataPoint(documentAnnotation, documentMetaData);
    }

    public void add(SentenceDataPoint sentenceDataPoint) {
        this.sentences.add(sentenceDataPoint);
    }

    public String getMetaHash() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        this.documentMetaData.forEach((k, v) -> digest.update(v.getBytes(StandardCharsets.UTF_8)));
        this.documentAnnotation.forEach((k, v) -> digest.update(v.getBytes(StandardCharsets.UTF_8)));
        String metaHash = Hex.encodeHexString(digest.digest());
        return metaHash;
    }

    public Map<String, String> getDocumentAnnotation() {
        return this.documentAnnotation;
    }

    public Map<String, String> getDocumentMetaData() {
        return this.documentMetaData;
    }

    public List<SentenceDataPoint> getSentences() {
        return this.sentences;
    }
}