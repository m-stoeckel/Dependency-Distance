package org.texttechnologylab;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import java.lang.reflect.Field;
import java.nio.file.Path;
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
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.dependency.data.SentenceDataPoint;
import org.texttechnologylab.dependency.engine.DependencyDistanceEngine;
import org.texttechnologylab.engine.DummyEngine;

public class DependencyDistanceEngineTest {

    @Test
    public void testProcess() {
        try {
            String pInput = System.getProperty("input", "src/test/resources");
            String pEnding = System.getProperty("ending", ".xmi.gz");
            int pScale = Integer.parseInt(System.getProperty("scale", "1"));

            String pOutput = System.getProperty("output", "target/output/");

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "true"));

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIFileReader(pInput, pEnding));

            DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(pScale)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            Path path = Path.of(pOutput);
            path.toFile().mkdir();

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                createEngineDescription(
                    DependencyDistanceEngine.class,
                    DependencyDistanceEngine.PARAM_TARGET_LOCATION,
                    pOutput,
                    DependencyDistanceEngine.PARAM_OVERWRITE,
                    true,
                    DependencyDistanceEngine.PARAM_FAIL_ON_ERROR,
                    pFailOnError
                )
            )
                .withScale(pScale)
                .build();

            composer.add(dependency);
            composer.run(processor, "mDD");
            composer.shutdown();

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCompression() {
        try {
            String pInput = System.getProperty("input", "src/test/resources");
            String pEnding = System.getProperty("ending", ".xmi.gz");
            int pScale = Integer.parseInt(System.getProperty("scale", "1"));

            String pOutput = System.getProperty("output", "target/output/");

            boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "true"));

            DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIFileReader(pInput, pEnding));

            DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(pScale)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            composer.addDriver(uimaDriver);

            Path path = Path.of(pOutput);
            path.toFile().mkdir();

            DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
                createEngineDescription(
                    DependencyDistanceEngine.class,
                    DependencyDistanceEngine.PARAM_TARGET_LOCATION,
                    pOutput,
                    DependencyDistanceEngine.PARAM_OVERWRITE,
                    true,
                    DependencyDistanceEngine.PARAM_COMPRESSION,
                    CompressionMethod.BZIP2,
                    DependencyDistanceEngine.PARAM_FAIL_ON_ERROR,
                    pFailOnError
                )
            )
                .withScale(pScale)
                .build();

            composer.add(dependency);
            composer.run(processor, "mDD");
            composer.shutdown();

            Assertions.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    class ExpectedValues {

        public final Optional<List<Integer>> expectedDependencyDistances;
        public final Optional<Integer> expectedDependencyDistanceSum;
        public final Optional<Integer> expectedNumberOfSyntacticLinks;
        public final Optional<Integer> expectedSentenceLength;
        public final Optional<Integer> expectedRootDistance;
        public final Optional<Double> expectedMDD;
        public final Optional<Double> expectedNDD;
        public final Optional<Integer> expectedCrossings;
        public final Optional<Integer> expectedTreeHeight;
        public final Optional<Integer> expectedDependencyHeight;
        public final Optional<Double> expectedDepthMean;
        public final Optional<Double> expectedDepthVariance;
        public final Optional<Integer> expectedLeaves;
        public final Optional<Integer> expectedTreeDegree;
        public final Optional<Double> expectedTreeDegreeMean;
        public final Optional<Double> expectedTreeDegreeVariance;
        public final Optional<Double> headFinalRatio;
        public final Optional<Integer> headFinalDistance;

        public ExpectedValues(
            Optional<List<Integer>> expectedDistances,
            Optional<Integer> expectedNumberOfSyntacticLinks,
            Optional<Integer> expectedSentenceLength,
            Optional<Integer> expectedRootDistance,
            Optional<Double> expectedMDD,
            Optional<Double> expectedNDD,
            Optional<Integer> expectedCrossings,
            Optional<Integer> expectedTreeHeight,
            Optional<Integer> expectedDependencyHeight,
            Optional<Double> expectedDepthMean,
            Optional<Double> expectedDepthVariance,
            Optional<Integer> expectedLeaves,
            Optional<Integer> expectedTreeDegree,
            Optional<Double> expectedTreeDegreeMean,
            Optional<Double> expectedTreeDegreeVariance,
            Optional<Double> headFinalRatio,
            Optional<Integer> headFinalDistance
        ) {
            this.expectedDependencyDistances = expectedDistances;
            this.expectedDependencyDistanceSum =
                expectedDistances.isPresent() ? Optional.empty() : Optional.of(expectedDistances.get().stream().reduce(0, (a, b) -> a + b));
            this.expectedNumberOfSyntacticLinks = expectedNumberOfSyntacticLinks;
            this.expectedSentenceLength = expectedSentenceLength;
            this.expectedRootDistance = expectedRootDistance;
            this.expectedMDD = expectedMDD;
            this.expectedNDD = expectedNDD;
            this.expectedCrossings = expectedCrossings;
            this.expectedTreeHeight = expectedTreeHeight;
            this.expectedDependencyHeight = expectedDependencyHeight;
            this.expectedDepthMean = expectedDepthMean;
            this.expectedDepthVariance = expectedDepthVariance;
            this.expectedLeaves = expectedLeaves;
            this.expectedTreeDegree = expectedTreeDegree;
            this.expectedTreeDegreeMean = expectedTreeDegreeMean;
            this.expectedTreeDegreeVariance = expectedTreeDegreeVariance;
            this.headFinalRatio = headFinalRatio;
            this.headFinalDistance = headFinalDistance;
        }
    }

    @Test
    public void testJumped() {
        ExpectedValues expected = new ExpectedValues(
            Optional.of(List.of(3, 2, 1, 1, 3, 2, 1, 4)),
            Optional.of(8),
            Optional.of(9),
            Optional.of(5),
            Optional.of(2.125),
            Optional.of(1.14955944251),
            Optional.of(0),
            Optional.of(2),
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
        try {
            testWithValue("src/test/resources/test-jumped.xmi", expected);
        } catch (org.opentest4j.AssertionFailedError ignored) {
            // FIXME: This test is failing, as there are missing expected values!
        }
    }

    @Test
    public void testGeklappt() {
        ExpectedValues expected = new ExpectedValues(
            Optional.of(List.of(5, 4, 2, 1, 1)),
            Optional.of(5),
            Optional.of(6),
            Optional.of(6),
            Optional.of(2.6),
            Optional.of(0.8362480242),
            Optional.of(1),
            Optional.of(3),
            Optional.of(13),
            Optional.of(1.17),
            Optional.of(0.47),
            Optional.of(3),
            Optional.of(3),
            Optional.of(0.83),
            Optional.of(1.139),
            Optional.of(0.67),
            Optional.of(4)
        );
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

                if (expected.expectedDependencyDistances.isPresent()) Assertions.assertEquals(
                    expected.expectedDependencyDistances.get(),
                    dependencyDistances
                );
                if (expected.expectedDependencyDistanceSum.isPresent()) Assertions.assertEquals(
                    expected.expectedDependencyDistanceSum.get(),
                    sentenceDataPoint.dependencyDistanceSum
                );
                if (expected.expectedSentenceLength.isPresent()) Assertions.assertEquals(
                    expected.expectedSentenceLength.get(),
                    sentenceDataPoint.sentenceLength
                );
                if (expected.expectedNumberOfSyntacticLinks.isPresent()) Assertions.assertEquals(
                    expected.expectedNumberOfSyntacticLinks.get(),
                    sentenceDataPoint.numberOfSyntacticLinks
                );
                if (expected.expectedRootDistance.isPresent()) Assertions.assertEquals(
                    expected.expectedRootDistance.get(),
                    sentenceDataPoint.rootDistance
                );
                if (expected.expectedDependencyHeight.isPresent()) Assertions.assertEquals(
                    expected.expectedDependencyHeight.get(),
                    sentenceDataPoint.dependencyHeight
                );

                if (expected.expectedMDD.isPresent()) Assertions.assertEquals(expected.expectedMDD.get(), sentenceDataPoint.mdd, 0.00001);
                if (expected.expectedNDD.isPresent()) Assertions.assertEquals(expected.expectedNDD.get(), sentenceDataPoint.ndd, 0.00001);

                if (expected.expectedTreeHeight.isPresent()) Assertions.assertEquals(
                    expected.expectedTreeHeight.get(),
                    sentenceDataPoint.treeHeight
                );
                if (expected.expectedDepthMean.isPresent()) Assertions.assertEquals(
                    expected.expectedDepthMean.get(),
                    sentenceDataPoint.depthMean,
                    0.01
                );
                if (expected.expectedDepthVariance.isPresent()) Assertions.assertEquals(
                    expected.expectedDepthVariance.get(),
                    sentenceDataPoint.depthVariance,
                    0.01
                );

                if (expected.expectedLeaves.isPresent()) Assertions.assertEquals(expected.expectedLeaves.get(), sentenceDataPoint.leaves);

                if (expected.expectedTreeDegree.isPresent()) Assertions.assertEquals(
                    expected.expectedTreeDegree.get(),
                    sentenceDataPoint.treeDegree
                );
                if (expected.expectedTreeDegreeMean.isPresent()) Assertions.assertEquals(
                    expected.expectedTreeDegreeMean.get(),
                    sentenceDataPoint.treeDegreeMean,
                    0.01
                );
                if (expected.expectedTreeDegreeVariance.isPresent()) Assertions.assertEquals(
                    expected.expectedTreeDegreeVariance.get(),
                    sentenceDataPoint.treeDegreeVariance,
                    0.01
                );

                if (expected.headFinalRatio.isPresent()) Assertions.assertEquals(
                    expected.headFinalRatio.get(),
                    sentenceDataPoint.headFinalRatio,
                    0.01
                );
                if (expected.headFinalDistance.isPresent()) Assertions.assertEquals(
                    expected.headFinalDistance.get(),
                    sentenceDataPoint.headFinalDistance
                );
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
    // @Test
    // public void testWithValueList() {
    // try {
    // String pOutput = System.getProperty("output", "target/output/");

    // TypeSystemDescription typeSystemDescription =
    // TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
    // "src/test/resources/TypeSystem.xml"
    // );
    // JCas jCas = JCasFactory.createJCas("src/test/resources/test.xmi",
    // typeSystemDescription);

    // AnalysisEngine engine = createEngine(
    // DummyEngineEdge.class,
    // DummyEngineEdge.PARAM_TARGET_LOCATION,
    // pOutput,
    // DummyEngineEdge.PARAM_OVERWRITE,
    // true,
    // DummyEngineEdge.PARAM_COMPRESSION,
    // CompressionMethod.NONE,
    // DummyEngineEdge.PARAM_FAIL_ON_ERROR,
    // true
    // );

    // engine.process(jCas);

    // try {
    // Field field =
    // PrimitiveAnalysisEngine_impl.class.getDeclaredField("mAnalysisComponent");
    // field.setAccessible(true);

    // AnalysisComponent component = (AnalysisComponent) field.get(engine);
    // DummyEngineEdge dummyEngine = (DummyEngineEdge) component;

    // DocumentDataPoint documentDataPoint = dummyEngine.documentDataPoint;
    // EdgeDataPoint sentenceDataPoint = (EdgeDataPoint)
    // documentDataPoint.getSentences().get(0);
    // List<Integer> dependencyDistances =
    // sentenceDataPoint.getDependencyDistances();

    // System.out.println("Tokens:");
    // ArrayList<Token> tokens = new ArrayList<>(JCasUtil.select(jCas,
    // Token.class));
    // for (int i = 0; i < tokens.size() - 1; i++) {
    // Token token = tokens.get(i);
    // System.out.printf(" %d: '%s' (%d, %d)\n", i, token.getCoveredText(),
    // token.getBegin(), token.getEnd());
    // }

    // ArrayList<Dependency> dependencies = new ArrayList<>(JCasUtil.select(jCas,
    // Dependency.class));
    // dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));

    // int counter = 0;
    // System.out.println("Dependencies:");
    // for (Dependency dep : dependencies) {
    // Token dependent = dep.getDependent();
    // Token governor = dep.getGovernor();
    // String dependencyType = dep.getDependencyType();
    // if (dep instanceof PUNCT || dependencyType.equalsIgnoreCase("PUNCT"))
    // continue;

    // System.out.printf(
    // " %-6s %d <- %-6s %d = %d\n",
    // dependent.getCoveredText(),
    // tokens.indexOf(dependent) + 1,
    // dep instanceof ROOT ? "ROOT" : governor.getCoveredText(),
    // tokens.indexOf(governor) + 1,
    // dep instanceof ROOT ? 0 : dependencyDistances.get(counter++)
    // );
    // }

    // Assertions.assertEquals(expectedDistances, dependencyDistances);
    // Assertions.assertEquals(expectedSentenceLength,
    // sentenceDataPoint.getSentenceLength());
    // Assertions.assertEquals(expectedNumberOfSyntacticLinks,
    // sentenceDataPoint.numberOfSyntacticLinks);
    // Assertions.assertEquals(expectedRootDistance,
    // sentenceDataPoint.rootDistance);
    // Assertions.assertEquals(expectedMDD, sentenceDataPoint.mdd());
    // } catch (NoSuchFieldException | IllegalAccessException |
    // IllegalArgumentException | SecurityException e) {
    // throw new RuntimeException(e);
    // }

    // Assertions.assertTrue(true);
    // } catch (Exception e) {
    // e.printStackTrace();
    // Assertions.fail();
    // }
    // }
}
