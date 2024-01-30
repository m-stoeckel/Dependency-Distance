package org.texttechnologylab.mdd.engine;

import com.google.gson.Gson;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import io.azam.ulidj.ULID;
import org.apache.commons.codec.binary.Hex;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.jetbrains.annotations.NotNull;
import org.texttechnologylab.annotation.DocumentAnnotation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DependencyDistanceEngine extends JCasFileWriter_ImplBase {
    public static final String PARAM_FAIL_ON_ERROR = "pFailOnError";
    @ConfigurationParameter(name = PARAM_FAIL_ON_ERROR, mandatory = false, defaultValue = "false")
    protected Boolean pFailOnError;

    public static final String PARAM_ULID_SUFFIX = "pUlidSuffix";
    @ConfigurationParameter(name = PARAM_ULID_SUFFIX, mandatory = false, defaultValue = "false")
    protected Boolean pUlidSuffix;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            DocumentDataPoints documentDataPoints = processCas(jCas);
            save(documentDataPoints);
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            e.printStackTrace();
            if (pFailOnError) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    @NotNull
    protected DocumentDataPoints processCas(JCas jCas) {
        final DocumentDataPoints documentDataPoints = DocumentDataPoints.fromJCas(jCas);

        final ArrayList<Sentence> sentences = new ArrayList<>(new ArrayList<>(JCasUtil.select(jCas, Sentence.class)));
        final HashMap<Sentence, Collection<Token>> tokenMap = new HashMap<>(JCasUtil.indexCovered(jCas, Sentence.class, Token.class));
        final HashMap<Sentence, Collection<Dependency>> dependencyMap = new HashMap<>(JCasUtil.indexCovered(jCas, Sentence.class, Dependency.class));

        for (Sentence sentence : sentences) {
            if (!sentenceIsValid(sentence, tokenMap)) continue;

            final Collection<Dependency> dependencies = dependencyMap.get(sentence);
            if (dependencies == null || dependencies.size() < 2) {
                getLogger().debug(String.format("Skipping due to dependencies: %s", dependencies));
                continue;
            }

            DataPoint dataPoint = processDependencies(dependencies);
            documentDataPoints.add(dataPoint);
        }
        return documentDataPoints;
    }

    private boolean sentenceIsValid(Sentence sentence, HashMap<Sentence, Collection<Token>> tokenMap) {
        if (!tokenMap.containsKey(sentence)) {
            getLogger().debug(String.format("Sentence not in tokenMap: '%s'", sentence.toString()));
            return false;
        }

        final Collection<Token> tokens = tokenMap.get(sentence);
        if (tokens == null) {
            getLogger().debug(String.format("Tokens are null for sentence: '%s'", sentence.toString()));
            return false;
        }

        if (tokens.size() < 3) {
            getLogger().debug(String.format("Sentence too short: '%s'", sentence.toString()));
            return false;
        }
        return true;
    }

    private DataPoint processDependencies(final Collection<Dependency> dependencies) {
        TreeMap<Integer, Token> tokenBeginMap = new TreeMap<>();
        int numberOfSyntacticLinks = 0;
        int rootDistance = -1;
        for (Dependency dependency : dependencies) {
            if (dependency instanceof ROOT) {
                rootDistance = numberOfSyntacticLinks + 1;
                continue;
            }
            if (dependency instanceof PUNCT) {
                continue;
            }
            numberOfSyntacticLinks += 1;

            Token governor = dependency.getGovernor();
            tokenBeginMap.put(governor.getBegin(), governor);
            Token dependent = dependency.getDependent();
            tokenBeginMap.put(dependent.getBegin(), dependent);
        }

        DataPoint dataPoint = new DataPoint(rootDistance, numberOfSyntacticLinks);
        for (Dependency dependency : dependencies) {
            if (dependency instanceof ROOT) {
                continue;
            }
            if (dependency instanceof PUNCT) {
                continue;
            }

            Token governor = dependency.getGovernor();
            Token dependent = dependency.getDependent();

            int governorTailSize = tokenBeginMap.tailMap(governor.getBegin()).size();
            int dependentTailSize = tokenBeginMap.tailMap(dependent.getBegin()).size();
            int dist = Math.abs(governorTailSize - dependentTailSize);

            dataPoint.add(dist);
        }
        return dataPoint;
    }

    protected void save(DocumentDataPoints dataPoints) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            dataPoints.documentMetaData.forEach((k, v) -> digest.update(v.getBytes(StandardCharsets.UTF_8)));
            dataPoints.documentAnnotation.forEach((k, v) -> digest.update(v.getBytes(StandardCharsets.UTF_8)));
            String metaHash = Hex.encodeHexString(digest.digest());

            if (pUlidSuffix) metaHash += "-" + ULID.random();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(metaHash, ".json"), StandardCharsets.UTF_8))) {
                String json = new Gson().toJson(dataPoints);
                writer.write(json);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected static class DataPoint {
        protected final int rootDistance;
        protected final int numberOfSyntacticLinks;
        protected final List<Integer> dependencyDistances;

        public DataPoint(int rootDistance, int numberOfSyntacticLinks) {
            this.rootDistance = rootDistance;
            this.numberOfSyntacticLinks = numberOfSyntacticLinks;
            this.dependencyDistances = new ArrayList<>();
        }

        public void add(int distance) {
            this.dependencyDistances.add(distance);
        }

        public float mdd() {
            float mDD = (float) this.dependencyDistances.stream().reduce(0, Integer::sum);
            return mDD / (float) this.numberOfSyntacticLinks;
        }
    }

    protected static class DocumentDataPoints {
        protected final Map<String, String> documentAnnotation;
        protected final Map<String, String> documentMetaData;
        protected final List<DataPoint> sentences;

        public DocumentDataPoints(DocumentAnnotation documentAnnotation, DocumentMetaData documentMetaData) {
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

        public static DocumentDataPoints fromJCas(JCas jCas) {
            DocumentAnnotation documentAnnotation = JCasUtil.selectSingle(jCas, DocumentAnnotation.class);
            DocumentMetaData documentMetaData = JCasUtil.selectSingle(jCas, DocumentMetaData.class);
            return new DocumentDataPoints(documentAnnotation, documentMetaData);
        }

        public void add(DataPoint dataPoint) {
            this.sentences.add(dataPoint);
        }
    }
}