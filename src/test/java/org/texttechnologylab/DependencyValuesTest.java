package org.texttechnologylab;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.texttechnologylab.engine.DummyEngine;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;

public class DependencyValuesTest {

    static class ExpectedValues {

        public Optional<List<Integer>> dependencyDistances;
        public Optional<Integer> dependencyDistanceSum;
        public Optional<Integer> numberOfSyntacticLinks;
        public Optional<Integer> sentenceLength;
        public Optional<Integer> rootDistance;
        public Optional<Double> mDD;
        public Optional<Double> nDD;
        public Optional<Integer> crossings;
        public Optional<Integer> treeHeight;
        public Optional<Integer> dependencyHeight;
        public Optional<Double> depthMean;
        public Optional<Double> depthVariance;
        public Optional<Integer> leaves;
        public Optional<Integer> treeDegree;
        public Optional<Double> treeDegreeMean;
        public Optional<Double> treeDegreeVariance;
        public Optional<Double> headFinalRatio;
        public Optional<Integer> headFinalDistance;

        public ExpectedValues() {
            this.dependencyDistances = Optional.empty();
            this.dependencyDistanceSum = Optional.empty();
            this.numberOfSyntacticLinks = Optional.empty();
            this.sentenceLength = Optional.empty();
            this.rootDistance = Optional.empty();
            this.mDD = Optional.empty();
            this.nDD = Optional.empty();
            this.crossings = Optional.empty();
            this.treeHeight = Optional.empty();
            this.dependencyHeight = Optional.empty();
            this.depthMean = Optional.empty();
            this.depthVariance = Optional.empty();
            this.leaves = Optional.empty();
            this.treeDegree = Optional.empty();
            this.treeDegreeMean = Optional.empty();
            this.treeDegreeVariance = Optional.empty();
            this.headFinalRatio = Optional.empty();
            this.headFinalDistance = Optional.empty();
        }

        public ExpectedValues(
            Optional<List<Integer>> dependencyDistances,
            Optional<Integer> numberOfSyntacticLinks,
            Optional<Integer> sentenceLength,
            Optional<Integer> rootDistance,
            Optional<Double> mDD,
            Optional<Double> nDD,
            Optional<Integer> crossings,
            Optional<Integer> treeHeight,
            Optional<Integer> dependencyHeight,
            Optional<Double> depthMean,
            Optional<Double> depthVariance,
            Optional<Integer> leaves,
            Optional<Integer> treeDegree,
            Optional<Double> treeDegreeMean,
            Optional<Double> treeDegreeVariance,
            Optional<Double> headFinalRatio,
            Optional<Integer> headFinalDistance
        ) {
            this.dependencyDistances = dependencyDistances;
            this.dependencyDistanceSum =
                dependencyDistances.isEmpty()
                    ? Optional.empty()
                    : Optional.of(dependencyDistances.get().stream().reduce(0, (a, b) -> a + b));
            this.numberOfSyntacticLinks = numberOfSyntacticLinks;
            this.sentenceLength = sentenceLength;
            this.rootDistance = rootDistance;
            this.mDD = mDD;
            this.nDD = nDD;
            this.crossings = crossings;
            this.treeHeight = treeHeight;
            this.dependencyHeight = dependencyHeight;
            this.depthMean = depthMean;
            this.depthVariance = depthVariance;
            this.leaves = leaves;
            this.treeDegree = treeDegree;
            this.treeDegreeMean = treeDegreeMean;
            this.treeDegreeVariance = treeDegreeVariance;
            this.headFinalRatio = headFinalRatio;
            this.headFinalDistance = headFinalDistance;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private ExpectedValues expectedValues;

            public Builder() {
                expectedValues =
                    new ExpectedValues(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    );
            }

            public ExpectedValues build() {
                return expectedValues;
            }

            public Builder expectedDependencyDistances(List<Integer> value) {
                this.expectedValues.dependencyDistances = Optional.of(value);
                this.expectedValues.dependencyDistanceSum = Optional.of(value.stream().reduce(0, (a, b) -> a + b));
                return this;
            }

            public Builder expectedDependencyDistanceSum(Integer value) {
                this.expectedValues.dependencyDistanceSum = Optional.of(value);
                return this;
            }

            public Builder expectedNumberOfSyntacticLinks(Integer value) {
                this.expectedValues.numberOfSyntacticLinks = Optional.of(value);
                return this;
            }

            public Builder expectedSentenceLength(Integer value) {
                this.expectedValues.sentenceLength = Optional.of(value);
                return this;
            }

            public Builder expectedRootDistance(Integer value) {
                this.expectedValues.rootDistance = Optional.of(value);
                return this;
            }

            public Builder expectedMDD(Double value) {
                this.expectedValues.mDD = Optional.of(value);
                return this;
            }

            public Builder expectedNDD(Double value) {
                this.expectedValues.nDD = Optional.of(value);
                return this;
            }

            public Builder expectedCrossings(Integer value) {
                this.expectedValues.crossings = Optional.of(value);
                return this;
            }

            public Builder expectedTreeHeight(Integer value) {
                this.expectedValues.treeHeight = Optional.of(value);
                return this;
            }

            public Builder expectedDependencyHeight(Integer value) {
                this.expectedValues.dependencyHeight = Optional.of(value);
                return this;
            }

            public Builder expectedDepthMean(Double value) {
                this.expectedValues.depthMean = Optional.of(value);
                return this;
            }

            public Builder expectedDepthVariance(Double value) {
                this.expectedValues.depthVariance = Optional.of(value);
                return this;
            }

            public Builder expectedLeaves(Integer value) {
                this.expectedValues.leaves = Optional.of(value);
                return this;
            }

            public Builder expectedTreeDegree(Integer value) {
                this.expectedValues.treeDegree = Optional.of(value);
                return this;
            }

            public Builder expectedTreeDegreeMean(Double value) {
                this.expectedValues.treeDegreeMean = Optional.of(value);
                return this;
            }

            public Builder expectedTreeDegreeVariance(Double value) {
                this.expectedValues.treeDegreeVariance = Optional.of(value);
                return this;
            }

            public Builder expectedHeadFinalRatio(Double value) {
                this.expectedValues.headFinalRatio = Optional.of(value);
                return this;
            }

            public Builder expectedHeadFinalDistance(Integer value) {
                this.expectedValues.headFinalDistance = Optional.of(value);
                return this;
            }
        }
    }

    @Test
    public void testJumped() {
        ExpectedValues expected = ExpectedValues
            .builder()
            .expectedDependencyDistances(List.of(3, 2, 1, 1, 3, 2, 1, 4))
            .expectedNumberOfSyntacticLinks(8)
            .expectedSentenceLength(9)
            .expectedRootDistance(5)
            .expectedMDD(2.125)
            .expectedNDD(1.14955944251)
            .expectedCrossings(0)
            .expectedTreeHeight(3)
            .build();
        testWithValue("src/test/resources/test-jumped.xmi", expected);
    }

    @Test
    public void testGeklappt() {
        ExpectedValues expected = ExpectedValues
            .builder()
            .expectedDependencyDistances(List.of(5, 4, 2, 1, 1))
            .expectedNumberOfSyntacticLinks(5)
            .expectedSentenceLength(6)
            .expectedRootDistance(6)
            .expectedMDD(2.6)
            .expectedNDD(0.8362480242)
            .expectedCrossings(1)
            .expectedTreeHeight(3)
            .expectedDependencyHeight(13)
            .expectedDepthMean(1.17)
            .expectedDepthVariance(0.47)
            .expectedLeaves(3)
            .expectedTreeDegree(3)
            .expectedTreeDegreeMean(0.83)
            .expectedTreeDegreeVariance(1.139)
            .expectedHeadFinalRatio(0.67)
            .expectedHeadFinalDistance(4)
            .build();
        testWithValue("src/test/resources/test-geklappt.xmi", expected);
    }

    public void testWithValue(String path, ExpectedValues expected) {
        try {
            String pOutput = System.getProperty("output", "target/output/");

            TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                "src/test/resources/TypeSystem.xml"
            );
            JCas jCas = JCasFactory.createJCas(path, typeSystemDescription);

            AnalysisEngine engine = createEngine(
                DummyEngine.class,
                DummyEngine.PARAM_TARGET_LOCATION,
                pOutput,
                DummyEngine.PARAM_OVERWRITE,
                true,
                DummyEngine.PARAM_COMPRESSION,
                CompressionMethod.NONE,
                DummyEngine.PARAM_FAIL_ON_ERROR,
                true,
                DummyEngine.PARAM_FIX_DATE_YEAR,
                true
            );

            engine.process(jCas);

            try {
                Field field = PrimitiveAnalysisEngine_impl.class.getDeclaredField("mAnalysisComponent");
                field.setAccessible(true);

                AnalysisComponent component = (AnalysisComponent) field.get(engine);
                DummyEngine dummyEngine = (DummyEngine) component;

                DocumentDataPoint documentDataPoint = dummyEngine.documentDataPoint;

                Map<String, String> documentAnnotation = documentDataPoint.getDocumentAnnotation();

                System.out.println("DocumentAnnotation");
                documentAnnotation.forEach((k, v) -> System.out.printf("  %s: %s\n", k, v));
                Assertions.assertEquals("2021", documentAnnotation.get("dateYear"));
                Assertions.assertEquals("12", documentAnnotation.get("dateMonth"));
                Assertions.assertEquals("13", documentAnnotation.get("dateDay"));
                Assertions.assertEquals("1639350000000", documentAnnotation.get("timestamp"));

                SentenceDataPoint sentenceDataPoint = (SentenceDataPoint) documentDataPoint.getSentences().get(0);

                System.out.println("Tokens:");
                ArrayList<Token> tokens = new ArrayList<>(JCasUtil.select(jCas, Token.class));
                for (int i = 0; i < tokens.size() - 1; i++) {
                    Token token = tokens.get(i);
                    System.out.printf("  %d: '%s' (%d, %d)\n", i, token.getCoveredText(), token.getBegin(), token.getEnd());
                }

                ArrayList<Dependency> dependencies = new ArrayList<>(JCasUtil.select(jCas, Dependency.class));
                dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));

                ArrayList<Integer> dependencyDistances = sentenceDataPoint.dependencyDistances;

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
                        dep instanceof ROOT ? 0 : dependencyDistances.get(counter++)
                    );
                }

                if (expected.dependencyDistances.isPresent()) {
                    Assertions.assertEquals(expected.dependencyDistances.get(), dependencyDistances, "dependencyDistances");
                }
                if (expected.dependencyDistanceSum.isPresent()) {
                    Assertions.assertEquals(
                        expected.dependencyDistanceSum.get(),
                        sentenceDataPoint.dependencyDistanceSum,
                        "dependencyDistanceSum"
                    );
                }
                if (expected.sentenceLength.isPresent()) {
                    Assertions.assertEquals(expected.sentenceLength.get(), sentenceDataPoint.sentenceLength, "sentenceLength");
                }
                if (expected.numberOfSyntacticLinks.isPresent()) {
                    Assertions.assertEquals(
                        expected.numberOfSyntacticLinks.get(),
                        sentenceDataPoint.numberOfSyntacticLinks,
                        "numberOfSyntacticLinks"
                    );
                }
                if (expected.rootDistance.isPresent()) {
                    Assertions.assertEquals(expected.rootDistance.get(), sentenceDataPoint.rootDistance, "rootDistance");
                }
                if (expected.dependencyHeight.isPresent()) {
                    Assertions.assertEquals(expected.dependencyHeight.get(), sentenceDataPoint.dependencyHeight, "dependencyHeight");
                }

                if (expected.mDD.isPresent()) {
                    Assertions.assertEquals(expected.mDD.get(), sentenceDataPoint.mdd, 0.00001, "mDD");
                }
                if (expected.nDD.isPresent()) {
                    Assertions.assertEquals(expected.nDD.get(), sentenceDataPoint.ndd, 0.00001, "nDD");
                }

                if (expected.treeHeight.isPresent()) {
                    Assertions.assertEquals(expected.treeHeight.get(), sentenceDataPoint.treeHeight, "treeHeight");
                }
                if (expected.depthMean.isPresent()) {
                    Assertions.assertEquals(expected.depthMean.get(), sentenceDataPoint.depthMean, 0.01, "depthMean");
                }
                if (expected.depthVariance.isPresent()) {
                    Assertions.assertEquals(expected.depthVariance.get(), sentenceDataPoint.depthVariance, 0.01, "depthVariance");
                }

                if (expected.leaves.isPresent()) {
                    Assertions.assertEquals(expected.leaves.get(), sentenceDataPoint.leaves, "leaves");
                }

                if (expected.treeDegree.isPresent()) {
                    Assertions.assertEquals(expected.treeDegree.get(), sentenceDataPoint.treeDegree, "treeDegree");
                }
                if (expected.treeDegreeMean.isPresent()) {
                    Assertions.assertEquals(expected.treeDegreeMean.get(), sentenceDataPoint.treeDegreeMean, 0.01, "treeDegreeMean");
                }
                if (expected.treeDegreeVariance.isPresent()) {
                    Assertions.assertEquals(
                        expected.treeDegreeVariance.get(),
                        sentenceDataPoint.treeDegreeVariance,
                        0.01,
                        "treeDegreeVariance"
                    );
                }

                if (expected.headFinalRatio.isPresent()) {
                    Assertions.assertEquals(expected.headFinalRatio.get(), sentenceDataPoint.headFinalRatio, 0.01, "headFinalRatio");
                }
                if (expected.headFinalDistance.isPresent()) {
                    Assertions.assertEquals(expected.headFinalDistance.get(), sentenceDataPoint.headFinalDistance, "headFinalDistance");
                }
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
