package org.texttechnologylab.dependency.engine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.data.SentenceDataPoint;
import org.texttechnologylab.dependency.graph.InvalidDependencyGraphException;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.ImmutableGraph.Builder;
import com.google.gson.Gson;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import io.azam.ulidj.ULID;

public class DependencyMetricsEngine extends JCasFileWriter_ImplBase {

    public static final String PARAM_FAIL_ON_ERROR = "pFailOnError";

    @ConfigurationParameter(name = PARAM_FAIL_ON_ERROR, mandatory = false, defaultValue = "false")
    protected Boolean pFailOnError;

    public static final String PARAM_ULID_SUFFIX = "pUlidSuffix";

    @ConfigurationParameter(name = PARAM_ULID_SUFFIX, mandatory = false, defaultValue = "false")
    protected Boolean pUlidSuffix;

    public static final String PARAM_FIX_DATE_YEAR = "pFixDateYear";

    @ConfigurationParameter(name = PARAM_FIX_DATE_YEAR, mandatory = false, defaultValue = "true")
    protected Boolean pFixDateYear;

    public static final String PARAM_FIX_DATE_YEAR_VALID_FROM = "pFixDateYearValidFrom";

    @ConfigurationParameter(name = PARAM_FIX_DATE_YEAR_VALID_FROM, mandatory = false, defaultValue = "1700")
    protected int pFixDateYearValidFrom;

    public static final String PARAM_FIX_DATE_YEAR_VALID_TO = "pFixDateYearValidTo";

    @ConfigurationParameter(name = PARAM_FIX_DATE_YEAR_VALID_TO, mandatory = false, defaultValue = "2024")
    protected int pFixDateYearValidTo;

    protected final Pattern[] allPatterns = new Pattern[] {
        Pattern.compile("(?!vom |am )(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})"),
        Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})"),
        Pattern.compile("(?!vom |am )(\\d{1,2})\\.?\\s*(\\p{L}+)\\s*(\\d{4})"),
        Pattern.compile("(\\d{1,2})\\.?\\s*(\\p{L}+)\\s*(\\d{4})")
    };

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            final DocumentDataPoint documentDataPoint = DocumentDataPoint.fromJCas(jCas);

            String dateYear = getDateYear(documentDataPoint);

            String metaHash = documentDataPoint.getMetaHash();

            String outputFile = String.join("/", dateYear, metaHash);
            if (pUlidSuffix) {
                outputFile = String.join("-", outputFile, ULID.random());
            }

            try {
                // Try to get the output stream _before_ processing the document
                // as we will get an IOException if the target file already exists
                NamedOutputStream outputStream = getOutputStream(outputFile, ".json");

                try {
                    processDocument(jCas, documentDataPoint);
                } catch (Exception e) {
                    // Unexpected: processDocument() is pretty safe, so something bad happened
                    throw new AnalysisEngineProcessException("Error while processing the document. This should not happen!", null, e);
                }

                try {
                    save(documentDataPoint, outputStream);
                } catch (IOException e) {
                    // Unexpected: We could not write to the output stream?
                    throw new AnalysisEngineProcessException("Could not save document data point to output stream.", null, e);
                }
            } catch (IOException e) {
                // Expected: getOutputStream() failed, most likely because the target file
                // already exists
                getLogger().error(e.getMessage());
                if (pFailOnError) throw new AnalysisEngineProcessException(e);
            }
        } catch (AnalysisEngineProcessException e) {
            // Something unexpected happened or an execption was passed on because
            // pFailOnError is true
            getLogger().error(e.getMessage());
            e.printStackTrace();
            if (pFailOnError) {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void processDocument(JCas jCas, final DocumentDataPoint documentDataPoint) {
        final ArrayList<Sentence> sentences = new ArrayList<>(new ArrayList<>(JCasUtil.select(jCas, Sentence.class)));
        final HashMap<Sentence, Collection<Token>> tokenMap = new HashMap<>(JCasUtil.indexCovered(jCas, Sentence.class, Token.class));
        final HashMap<Sentence, Collection<Dependency>> dependencyMap = new HashMap<>(
            JCasUtil.indexCovered(jCas, Sentence.class, Dependency.class)
        );

        for (Sentence sentence : sentences) {
            if (!sentenceIsValid(sentence, tokenMap)) continue;

            final Collection<Dependency> dependencies = dependencyMap.get(sentence);
            if (dependencies == null || dependencies.size() < 2) {
                getLogger().debug(String.format("Skipping due to dependencies: %s", dependencies));
                continue;
            }

            try {
                documentDataPoint.add(processDependencies(new ArrayList<>(dependencyMap.get(sentence))));
            } catch (InvalidDependencyGraphException ignored) {
                // Catch exception for invalid sentences
                getLogger().error("%s\n%s\n", ignored.getMessage(), ignored.getCause());
            }
        }
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

    private SentenceDataPoint processDependencies(final ArrayList<Dependency> dependencies) throws InvalidDependencyGraphException {
        dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));
        ArrayList<Token> tokens = dependencies
            .stream()
            .flatMap(d -> Stream.of(d.getGovernor(), d.getDependent()))
            .distinct()
            .sorted(Comparator.comparingInt(Annotation::getBegin))
            .collect(Collectors.toCollection(ArrayList::new));

        Builder<Integer> graphBuilder = GraphBuilder.directed().<Integer>immutable().addNode(0);
        ArrayList<EndpointPair<Integer>> edges = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            Token governor = dependency.getGovernor();
            Token dependent = dependency.getDependent();

            int govenorIndex = tokens.indexOf(governor) + 1;
            int dependentIndex = tokens.indexOf(dependent) + 1;

            String dependencyType = dependency.getDependencyType();
            if (dependency instanceof PUNCT || dependencyType.equalsIgnoreCase("PUNCT")) {
                edges.add(EndpointPair.ordered(govenorIndex, dependentIndex));
            } else if (dependency instanceof ROOT || dependencyType.equalsIgnoreCase("ROOT") || governor == dependent) {
                graphBuilder.putEdge(0, dependentIndex);
            } else {
                graphBuilder.putEdge(govenorIndex, dependentIndex);
            }
        }

        ImmutableGraph<Integer> dependencyGraph = graphBuilder.build();

        edges.forEach(edge -> graphBuilder.putEdge(edge.source(), edge.target()));
        ImmutableGraph<Integer> dependencyGraphWithPunct = graphBuilder.build();

        return new SentenceDataPoint(dependencyGraph, dependencyGraphWithPunct);
    }

    protected static void save(DocumentDataPoint dataPoints, OutputStream outputStream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            String json = new Gson().toJson(dataPoints);
            writer.write(json);
        }
    }

    protected static boolean isNumeric(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected Matcher anyMatch(String... strings) {
        for (Pattern pattern : allPatterns) {
            for (String s : strings) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.find()) {
                    return matcher;
                }
            }
        }
        return null;
    }

    protected String getDateYear(DocumentDataPoint documentDataPoint) throws AnalysisEngineProcessException {
        String dateYear = documentDataPoint.getDocumentAnnotation().getOrDefault("dateYear", "0000");
        if (pFixDateYear) {
            try {
                dateYear = fixDateYear(documentDataPoint.getDocumentAnnotation(), documentDataPoint.getDocumentMetaData());
                documentDataPoint.getDocumentAnnotation().put("dateYear", dateYear);
                return dateYear;
            } catch (NumberFormatException e) {
                getLogger().error(String.format("Could not parse dateYear '%s': %s", dateYear, e.getMessage()));
                if (pFailOnError) throw new AnalysisEngineProcessException(e);
            }
        }
        return dateYear;
    }

    protected String fixDateYear(Map<String, String> documentAnnotation, Map<String, String> documentMetaData) {
        String dateDay = documentAnnotation.get("dateDay");
        String dateMonth = documentAnnotation.get("dateMonth");
        String dateYear = documentAnnotation.get("dateYear");

        String documentTitle = documentMetaData.get("documentTitle");
        String documentId = documentMetaData.get("documentId");
        String subtitle = documentAnnotation.getOrDefault("subtitle", "");
        try {
            if (!checkDateYear(dateYear)) {
                Matcher mtch = anyMatch(documentTitle, documentId, subtitle);
                if (Objects.nonNull(mtch)) {
                    dateDay = mtch.group(1);
                    dateMonth = mtch.group(2);
                    dateYear = mtch.group(3);
                } else {
                    return dateYear;
                }

                if (!checkDateYear(dateYear)) {
                    throw new Exception(String.format("Year %s is not valid from match: %s", dateYear, mtch));
                }
            }

            String timestamp = "";
            if (isNumeric(dateMonth)) {
                SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss", Locale.GERMANY);
                Date date = df.parse(String.format("%s.%s.%s 00:00:00", dateDay, dateMonth, dateYear));

                timestamp = String.valueOf(date.getTime());
            } else {
                SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy hh:mm:ss", Locale.GERMAN);
                Date date = df.parse(String.format("%s %s %s 00:00:0", dateDay, dateMonth, dateYear));

                Calendar calendar = df.getCalendar();
                calendar.setTime(date);
                dateMonth = String.valueOf(calendar.get(Calendar.MONTH));

                timestamp = String.valueOf(date.getTime());
            }

            documentAnnotation.put("dateDay", dateDay);
            documentAnnotation.put("dateMonth", dateMonth);
            documentAnnotation.put("dateYear", dateYear);
            documentAnnotation.put("timestamp", timestamp);

            return dateYear;
        } catch (Exception e) {
            return dateYear;
        }
    }

    protected boolean checkDateYear(String dateYear) {
        return (
            isNumeric(dateYear) &&
            (this.pFixDateYearValidFrom <= Integer.parseInt(dateYear)) &&
            (Integer.parseInt(dateYear) <= this.pFixDateYearValidTo)
        );
    }
}
