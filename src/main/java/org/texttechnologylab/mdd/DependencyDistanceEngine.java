package org.texttechnologylab.mdd;

import com.google.common.io.Files;
import com.google.gson.Gson;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import org.apache.commons.codec.binary.Hex;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.internal.ExtendedLogger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.annotation.DocumentAnnotation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

public class DependencyDistanceEngine extends JCasFileWriter_ImplBase {

    public static final String PARAM_OUTPUT = "pOutputPath";
    @ConfigurationParameter(name = PARAM_OUTPUT, mandatory = true)
    protected String pOutputPath;

    public static final String PARAM_FAIL_ON_ERROR = "pFailOnError";
    @ConfigurationParameter(name = PARAM_FAIL_ON_ERROR, mandatory = false, defaultValue = "false")
    protected Boolean pFailOnError;

    protected ExtendedLogger logger;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        this.logger = getLogger();

        File outputPath = Path.of(this.pOutputPath).toFile();
        if (!outputPath.exists()) {
            throw new ResourceInitializationException(new InvalidPathException(this.pOutputPath, "The given output path does not exist!"));
        } else if (!outputPath.isDirectory()) {
            throw new ResourceInitializationException(new InvalidPathException(this.pOutputPath, "The given output path is not a directory!"));
        }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            DocumentAnnotation documentAnnotation = JCasUtil.selectSingle(jCas, DocumentAnnotation.class);
            DocumentMetaData documentMetaData = JCasUtil.selectSingle(jCas, DocumentMetaData.class);
            DocumentDataPoint documentDataPoint = new DocumentDataPoint(documentAnnotation, documentMetaData);

            ArrayList<Sentence> sentences = new ArrayList<Sentence>(new ArrayList<>(JCasUtil.select(jCas, Sentence.class)));
            HashMap<Sentence, Collection<Token>> tokenMap = new HashMap<Sentence, Collection<Token>>(JCasUtil.indexCovered(jCas, Sentence.class, Token.class));
            HashMap<Sentence, Collection<Dependency>> dependencyMap = new HashMap<Sentence, Collection<Dependency>>(JCasUtil.indexCovered(jCas, Sentence.class, Dependency.class));


            TreeMap<Integer, Token> tokenBeginMap = new TreeMap<>();
            for (Sentence sentence : sentences) {
                if (!tokenMap.containsKey(sentence)) {
                    this.logger.debug(String.format("Sentence not in tokenMap: '%s'", sentence.toString()));
                    continue;
                }

                Collection<Token> tokens = tokenMap.get(sentence);
                if (tokens == null) {
                    this.logger.debug(String.format("Tokens are null for sentence: '%s'", sentence.toString()));
                    continue;
                }

                if (tokens.size() < 3) {
                    this.logger.debug(String.format("Sentence too short: '%s'", sentence.toString()));
                    continue;
                }

                Collection<Dependency> dependencies = dependencyMap.get(sentence);
                if (dependencies == null || dependencies.size() < 2) {
                    this.logger.debug(String.format("Skipping due to dependencies: %s", dependencies));
                    continue;
                }

                tokenBeginMap.clear();
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

                SentenceDataPoint dataPoint = new SentenceDataPoint(rootDistance, numberOfSyntacticLinks);
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
                documentDataPoint.add(dataPoint);
            }

            documentDataPoint.save(this.pOutputPath);
        } catch (Exception e) {
            this.logger.error(e.getMessage());
            e.printStackTrace();
            if (pFailOnError) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    private static class SentenceDataPoint {
        int rootDistance;
        int numberOfSyntacticLinks;
        List<Integer> dependencyDistances;

        public SentenceDataPoint(int rootDistance, int numberOfSyntacticLinks) {
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

    private class DocumentDataPoint {
        Map<String, String> documentAnnotation;
        Map<String, String> documentMetaData;
        List<SentenceDataPoint> sentences;

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

        public void add(SentenceDataPoint dataPoint) {
            this.sentences.add(dataPoint);
        }

        public void save(String path) throws IOException {
            MessageDigest digest = Util.getSha1Digest();
            this.documentMetaData.forEach((k, v) -> digest.update(v.getBytes(StandardCharsets.UTF_8)));
            String metaHash = Hex.encodeHexString(digest.digest());

            String json = new Gson().toJson(this);

            File outputFile = Path.of(path, metaHash + ".json").toFile();

            logger.info(String.format("Writing DocumentDataPoint to %s", outputFile.getAbsolutePath()));
            try (BufferedWriter writer = Files.newWriter(outputFile, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        }
    }
}
