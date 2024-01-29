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
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.internal.ExtendedLogger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.annotation.DocumentAnnotation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DependencyDistanceEngine extends JCasFileWriter_ImplBase {

    public static final String PARAM_OUTPUT = "outputPath";
    @ConfigurationParameter(name = PARAM_OUTPUT, mandatory = true)
    protected String outputPath;

    protected ExtendedLogger logger;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        this.logger = getLogger();

        File outputPath = Path.of(this.outputPath).toFile();
        if (!outputPath.exists()) {
            throw new ResourceInitializationException(new InvalidPathException(this.outputPath, "The given output path does not exist!"));
        } else if (!outputPath.isDirectory()) {
            throw new ResourceInitializationException(new InvalidPathException(this.outputPath, "The given output path is not a directory!"));
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

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                digest.update(documentMetaData.getDocumentUri().getBytes(StandardCharsets.UTF_8));
                digest.update(documentMetaData.getDocumentId().getBytes(StandardCharsets.UTF_8));
                digest.update(documentMetaData.getDocumentTitle().getBytes(StandardCharsets.UTF_8));
                String metaHash = Hex.encodeHexString(digest.digest());

                String json = new Gson().toJson(documentDataPoint);
                File outputFile = Path.of(this.outputPath, metaHash + ".json").toFile();
                try (BufferedWriter writer = Files.newWriter(outputFile, StandardCharsets.UTF_8)) {
                    writer.write(json);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        } catch (IllegalArgumentException e) {
            this.logger.error(Arrays.toString(e.getStackTrace()));
        }
    }

    private static class SentenceDataPoint {
        int rootDistance;
        int numberOfSyntacticLinks;
        ArrayList<Integer> dependencyDistances;

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
        String author;
        String publisher;
        Integer dateDay;
        String subtitle;
        Integer dateMonth;
        Integer dateYear;
        Long timestamp;
        String place;

        String documentUri;

        String documentTitle;

        String documentId;

        ArrayList<SentenceDataPoint> sentences;

        public DocumentDataPoint(DocumentAnnotation documentAnnotation, DocumentMetaData documentMetaData) {
            this.author = documentAnnotation.getAuthor();
            this.publisher = documentAnnotation.getPublisher();
            this.dateDay = documentAnnotation.getDateDay();
            this.subtitle = documentAnnotation.getSubtitle();
            this.dateMonth = documentAnnotation.getDateMonth();
            this.dateYear = documentAnnotation.getDateYear();
            this.timestamp = documentAnnotation.getTimestamp();
            this.place = documentAnnotation.getPlace();

            this.documentUri = documentMetaData.getDocumentUri();
            this.documentTitle = documentMetaData.getDocumentTitle();
            this.documentId = documentMetaData.getDocumentId();

            this.sentences = new ArrayList<>();
        }

        public void add(SentenceDataPoint dataPoint) {
            this.sentences.add(dataPoint);
        }
    }
}
