package org.texttechnologylab;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.engine.DummyEngine;
import org.texttechnologylab.mdd.data.DependencyDataPoint;
import org.texttechnologylab.mdd.data.DocumentDataPoint;
import org.texttechnologylab.mdd.engine.DependencyDistanceEngine;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.PUNCT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;

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

    class ExpectedValues implements DependencyDataPoint {

        List<Integer> expectedDistances;
        int expectedDistanceSum;
        int expectedNumberOfSyntacticLinks;
        int expectedSentenceLength;
        int expectedRootDistance;
        double expectedMDD;
        double expectedNDD;
        int expectedCrossings;
        int expectedTreeHeight;
        int expectedDependcyHeight;
        double expectedDepthMean;

        public ExpectedValues(
            List<Integer> expectedDistances,
            int expectedNumberOfSyntacticLinks,
            int expectedSentenceLength,
            int expectedRootDistance,
            double expectedMDD,
            double expectedNDD,
            int expectedCrossings,
            int expectedTreeHeight,
            int expectedDependcyHeight,
            double expectedDepthMean
        ) {
            this.expectedDistances = expectedDistances;
            this.expectedNumberOfSyntacticLinks = expectedNumberOfSyntacticLinks;
            this.expectedSentenceLength = expectedSentenceLength;
            this.expectedRootDistance = expectedRootDistance;
            this.expectedMDD = expectedMDD;
            this.expectedNDD = expectedNDD;
            this.expectedCrossings = expectedCrossings;
            this.expectedTreeHeight = expectedTreeHeight;
            this.expectedDependcyHeight = expectedDependcyHeight;
            this.expectedDepthMean = expectedDepthMean;
        }

        @Override
        public int getSentenceLength() {
            return expectedSentenceLength;
        }

        @Override
        public int getNumberOfSyntacticLinks() {
            return expectedNumberOfSyntacticLinks;
        }

        @Override
        public int rootDistance() {
            return expectedRootDistance;
        }

        @Override
        public ArrayList<Integer> getDependencyDistances() {
            return new ArrayList<>(expectedDistances);
        }

        @Override
        public int getDependencyDistanceSum() {
            return expectedDistances.stream().reduce(0, (a, b) -> a + b);
        }

        @Override
        public double mdd() {
            return expectedMDD;
        }

        @Override
        public double ndd() {
            return expectedNDD;
        }

        @Override
        public int crossings() {
            return expectedCrossings;
        }

        @Override
        public int dependencyHeight() {
            return expectedDependcyHeight;   
        }

        @Override
        public double depthMean() {
            return expectedDepthMean;
        }

        @Override
        public double depthVariance() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'depthVariance'");
        }

        @Override
        public int leaves() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'leaves'");
        }

        @Override
        public int treeHeight() {
            return expectedTreeHeight;
        }

        @Override
        public int treeDegree() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'treeDegree'");
        }

        @Override
        public double treeDegreeMean() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'treeDegreeMean'");
        }

        @Override
        public double treeDegreeVariance() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'treeDegreeVariance'");
        }
    }

    @Test
    public void testJumped() {
        ExpectedValues expected = new ExpectedValues(List.of(3, 2, 1, 1, 3, 2, 1, 4), 8, 9, 5, 2.125, 1.14955944251, 0, 2, 6, -1);
        testWithValue("src/test/resources/test-jumped.xmi", expected);
    }

    @Test
    public void testGeklappt() {
        ExpectedValues expected = new ExpectedValues(List.of(5, 4, 2, 1, 1), 5, 6, 6, 2.6, 0.8362480242, 1, 3, 13, 1.17);
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

                DependencyDataPoint sentenceDataPoint = (DependencyDataPoint) documentDataPoint.getSentences().get(0);

                System.out.println("Tokens:");
                ArrayList<Token> tokens = new ArrayList<>(JCasUtil.select(jCas, Token.class));
                for (int i = 0; i < tokens.size() - 1; i++) {
                    Token token = tokens.get(i);
                    System.out.printf("  %d: '%s' (%d, %d)\n", i, token.getCoveredText(), token.getBegin(), token.getEnd());
                }

                ArrayList<Dependency> dependencies = new ArrayList<>(JCasUtil.select(jCas, Dependency.class));
                dependencies.sort(Comparator.comparingInt(o -> o.getDependent().getBegin()));

                ArrayList<Integer> dependencyDistances = sentenceDataPoint.getDependencyDistances();

                int counter = 0;
                System.out.println("Dependencies:");
                for (Dependency dep : dependencies) {
                    Token dependent = dep.getDependent();
                    Token governor = dep.getGovernor();
                    String dependencyType = dep.getDependencyType();
                    if (dep instanceof PUNCT || dependencyType.equalsIgnoreCase("PUNCT")) continue;

                    System.out.printf(
                        "  %-6s %d -> %-6s %d = %d\n",
                        dependent.getCoveredText(),
                        tokens.indexOf(dependent) + 1,
                        dep instanceof ROOT ? "ROOT" : governor.getCoveredText(),
                        tokens.indexOf(governor) + 1,
                        dep instanceof ROOT ? 0 : dependencyDistances.get(counter++)
                    );
                }

                Assertions.assertEquals(expected.getDependencyDistances(), dependencyDistances);
                Assertions.assertEquals(expected.getDependencyDistanceSum(), sentenceDataPoint.getDependencyDistanceSum());
                Assertions.assertEquals(expected.getSentenceLength(), sentenceDataPoint.getSentenceLength());
                Assertions.assertEquals(expected.getNumberOfSyntacticLinks(), sentenceDataPoint.getNumberOfSyntacticLinks());
                Assertions.assertEquals(expected.rootDistance(), sentenceDataPoint.rootDistance());
                Assertions.assertEquals(expected.dependencyHeight(), sentenceDataPoint.dependencyHeight());
                
                
                Assertions.assertEquals(expected.mdd(), sentenceDataPoint.mdd(), 0.00001);
                Assertions.assertEquals(expected.ndd(), sentenceDataPoint.ndd(), 0.00001);
                
                Assertions.assertEquals(expected.treeHeight(), sentenceDataPoint.treeHeight());
                Assertions.assertEquals(expected.depthMean(), sentenceDataPoint.depthMean(), 0.01);
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertTrue(true);
        } catch (Exception e) {
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
    // " %-6s %d -> %-6s %d = %d\n",
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
