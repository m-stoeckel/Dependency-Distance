package org.texttechnologylab.mdd.engine;

import com.google.gson.Gson;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import io.azam.ulidj.ULID;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.jetbrains.annotations.NotNull;
import org.texttechnologylab.mdd.data.DocumentDataPoint;
import org.texttechnologylab.mdd.data.SentenceDataPoint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

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
            DocumentDataPoint documentDataPoint = processCas(jCas);
            save(documentDataPoint);
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            e.printStackTrace();
            if (pFailOnError) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    @NotNull
    protected DocumentDataPoint processCas(JCas jCas) {
        final DocumentDataPoint documentDataPoint = DocumentDataPoint.fromJCas(jCas);

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

            SentenceDataPoint sentenceDataPoint = processDependencies(new ArrayList<>(dependencyMap.get(sentence)));
            documentDataPoint.add(sentenceDataPoint);
        }
        return documentDataPoint;
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

    private SentenceDataPoint processDependencies(final ArrayList<Dependency> dependencies) {
        dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));
        ArrayList<Token> tokens = dependencies.stream()
                .flatMap(d -> Arrays.stream(new Token[]{d.getGovernor(), d.getDependent()}))
                .distinct()
                .sorted(Comparator.comparingInt(Annotation::getBegin))
                .collect(Collectors.toCollection(ArrayList::new));


        int rootDistance = -1;
        int numberOfSyntacticLinks = 0;
        SentenceDataPoint sentenceDataPoint = new SentenceDataPoint();
        for (Dependency dependency : dependencies) {
            if (dependency instanceof PUNCT || dependency.getDependencyType().equalsIgnoreCase("PUNCT")) {
                continue;
            }

            numberOfSyntacticLinks++;

            if (dependency instanceof ROOT) {
                rootDistance = numberOfSyntacticLinks;
                continue;
            }

            int dist = Math.abs(tokens.indexOf(dependency.getGovernor()) - tokens.indexOf(dependency.getDependent()));

            sentenceDataPoint.add(dist);
        }
        sentenceDataPoint.rootDistance = rootDistance;
        sentenceDataPoint.numberOfSyntacticLinks = numberOfSyntacticLinks;
        return sentenceDataPoint;
    }

    protected void save(DocumentDataPoint dataPoints) throws IOException {
        try {
            String metaHash = dataPoints.getMetaHash();

            if (pUlidSuffix) metaHash += "-" + ULID.random();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(metaHash, ".json"), StandardCharsets.UTF_8))) {
                String json = new Gson().toJson(dataPoints);
                writer.write(json);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}