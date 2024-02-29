package org.texttechnologylab.engine;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.data.SentenceDataPoint;
import org.texttechnologylab.utils.ExpectedDocumentAnnotations;
import org.texttechnologylab.utils.ExpectedValues;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;

public class DependencyMetricsValueTest {

    @Test
    public void testJumped() {
        URL resource = DependencyMetricsValueTest.class.getClassLoader().getResource("xmi/test-jumped.xmi");
        testWithValue(resource.getPath(), ExpectedValues.getExpectedForJumped(), ExpectedDocumentAnnotations.get20211223());
    }

    @Test
    public void testGeklappt() {
        URL resource = DependencyMetricsValueTest.class.getClassLoader().getResource("xmi/test-geklappt.xmi");
        testWithValue(resource.getPath(), ExpectedValues.getExpectedForGeklappt(), ExpectedDocumentAnnotations.get20211223());
    }

    public void testWithValue(String path, ExpectedValues expectedValues, ExpectedDocumentAnnotations expectedAnnotations) {
        try {
            String pOutput = System.getProperty("output", "target/output/");

            TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                DependencyMetricsValueTest.class.getClassLoader().getResource("TypeSystem.xml").getPath()
            );
            JCas jCas = JCasFactory.createJCas(path, typeSystemDescription);

            AnalysisEngine engine = createEngine(
                ReflectionEngine.class,
                ReflectionEngine.PARAM_TARGET_LOCATION,
                pOutput,
                ReflectionEngine.PARAM_OVERWRITE,
                true,
                ReflectionEngine.PARAM_COMPRESSION,
                CompressionMethod.NONE,
                ReflectionEngine.PARAM_FAIL_ON_ERROR,
                true,
                ReflectionEngine.PARAM_FIX_DATE_YEAR,
                true
            );

            engine.process(jCas);

            try {
                Field field = PrimitiveAnalysisEngine_impl.class.getDeclaredField("mAnalysisComponent");
                field.setAccessible(true);

                AnalysisComponent component = (AnalysisComponent) field.get(engine);
                ReflectionEngine dummyEngine = (ReflectionEngine) component;

                DocumentDataPoint documentDataPoint = dummyEngine.documentDataPoint;

                Map<String, String> documentAnnotation = documentDataPoint.getDocumentAnnotation();

                System.out.println("DocumentAnnotation");
                documentAnnotation.forEach((k, v) -> System.out.printf("  %s: %s\n", k, v));
                expectedAnnotations.assertEquals(documentAnnotation);

                SentenceDataPoint sentenceDataPoint = (SentenceDataPoint) documentDataPoint.getSentences().get(0);

                System.out.println("Tokens:");
                ArrayList<Token> tokens = new ArrayList<>(JCasUtil.select(jCas, Token.class));
                for (int i = 0; i < tokens.size() - 1; i++) {
                    Token token = tokens.get(i);
                    System.out.printf("  %d: '%s' (%d, %d)\n", i, token.getCoveredText(), token.getBegin(), token.getEnd());
                }

                ArrayList<Dependency> dependencies = new ArrayList<>(JCasUtil.select(jCas, Dependency.class));
                dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));

                int counter = 0;
                System.out.println("Dependencies:");
                for (Dependency dep : dependencies) {
                    Token dependent = dep.getDependent();
                    Token governor = dep.getGovernor();
                    String dependencyType = dep.getDependencyType();
                    if (dep instanceof PUNCT || dependencyType.equalsIgnoreCase("PUNCT")) continue;

                    System.out.printf(
                        "  %-6s %d <- %-6s %d = %d\n",
                        dependent.getCoveredText(),
                        tokens.indexOf(dependent) + 1,
                        dep instanceof ROOT ? "ROOT" : governor.getCoveredText(),
                        tokens.indexOf(governor) + 1,
                        dep instanceof ROOT ? 0 : sentenceDataPoint.dependencyDistances.get(counter++)
                    );
                }

                expectedValues.assertEquals(sentenceDataPoint);
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertTrue(true);
        } catch (Exception e) {
            System.err.printf("[EXCEPTION] ");
            e.printStackTrace();
            Assertions.fail();
        }
    }
}
