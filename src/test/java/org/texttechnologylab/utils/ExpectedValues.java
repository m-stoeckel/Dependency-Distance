package org.texttechnologylab.utils;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.texttechnologylab.dependency.data.SentenceDataPoint;

public class ExpectedValues {

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
            dependencyDistances.isEmpty() ? Optional.empty() : Optional.of(dependencyDistances.get().stream().reduce(0, (a, b) -> a + b));
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

    public static ExpectedValues.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ExpectedValues expectedValues;

        public Builder() {
            this.expectedValues = new ExpectedValues();
        }

        public ExpectedValues build() {
            return this.expectedValues;
        }

        public ExpectedValues.Builder expectedDependencyDistances(List<Integer> value) {
            this.expectedValues.dependencyDistances = Optional.of(value);
            this.expectedValues.dependencyDistanceSum = Optional.of(value.stream().reduce(0, (a, b) -> a + b));
            return this;
        }

        public ExpectedValues.Builder expectedDependencyDistanceSum(Integer value) {
            this.expectedValues.dependencyDistanceSum = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedNumberOfSyntacticLinks(Integer value) {
            this.expectedValues.numberOfSyntacticLinks = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedSentenceLength(Integer value) {
            this.expectedValues.sentenceLength = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedRootDistance(Integer value) {
            this.expectedValues.rootDistance = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedMDD(Double value) {
            this.expectedValues.mDD = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedNDD(Double value) {
            this.expectedValues.nDD = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedCrossings(Integer value) {
            this.expectedValues.crossings = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedTreeHeight(Integer value) {
            this.expectedValues.treeHeight = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedDependencyHeight(Integer value) {
            this.expectedValues.dependencyHeight = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedDepthMean(Double value) {
            this.expectedValues.depthMean = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedDepthVariance(Double value) {
            this.expectedValues.depthVariance = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedLeaves(Integer value) {
            this.expectedValues.leaves = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedTreeDegree(Integer value) {
            this.expectedValues.treeDegree = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedTreeDegreeMean(Double value) {
            this.expectedValues.treeDegreeMean = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedTreeDegreeVariance(Double value) {
            this.expectedValues.treeDegreeVariance = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedHeadFinalRatio(Double value) {
            this.expectedValues.headFinalRatio = Optional.of(value);
            return this;
        }

        public ExpectedValues.Builder expectedHeadFinalDistance(Integer value) {
            this.expectedValues.headFinalDistance = Optional.of(value);
            return this;
        }
    }

    public boolean assertEquals(SentenceDataPoint sentenceDataPoint) {
        if (this.dependencyDistances.isPresent()) {
            Assertions.assertEquals(this.dependencyDistances.get(), sentenceDataPoint.dependencyDistances, "dependencyDistances");
            System.out.println("OK: dependencyDistances");
        }
        if (this.dependencyDistanceSum.isPresent()) {
            Assertions.assertEquals(this.dependencyDistanceSum.get(), sentenceDataPoint.dependencyDistanceSum, "dependencyDistanceSum");
            System.out.println("OK: dependencyDistanceSum");
        }
        if (this.sentenceLength.isPresent()) {
            Assertions.assertEquals(this.sentenceLength.get(), sentenceDataPoint.sentenceLength, "sentenceLength");
            System.out.println("OK: sentenceLength");
        }
        if (this.numberOfSyntacticLinks.isPresent()) {
            Assertions.assertEquals(this.numberOfSyntacticLinks.get(), sentenceDataPoint.numberOfSyntacticLinks, "numberOfSyntacticLinks");
            System.out.println("OK: numberOfSyntacticLinks");
        }
        if (this.rootDistance.isPresent()) {
            Assertions.assertEquals(this.rootDistance.get(), sentenceDataPoint.rootDistance, "rootDistance");
            System.out.println("OK: rootDistance");
        }
        if (this.dependencyHeight.isPresent()) {
            Assertions.assertEquals(this.dependencyHeight.get(), sentenceDataPoint.dependencyHeight, "dependencyHeight");
            System.out.println("OK: dependencyHeight");
        }

        if (this.mDD.isPresent()) {
            Assertions.assertEquals(this.mDD.get(), sentenceDataPoint.mdd, 0.00001, "mDD");
            System.out.println("OK: mDD");
        }
        if (this.nDD.isPresent()) {
            Assertions.assertEquals(this.nDD.get(), sentenceDataPoint.ndd, 0.00001, "nDD");
            System.out.println("OK: nDD");
        }

        if (this.treeHeight.isPresent()) {
            Assertions.assertEquals(this.treeHeight.get(), sentenceDataPoint.treeHeight, "treeHeight");
            System.out.println("OK: treeHeight");
        }
        if (this.depthMean.isPresent()) {
            Assertions.assertEquals(this.depthMean.get(), sentenceDataPoint.depthMean, 0.01, "depthMean");
            System.out.println("OK: depthMean");
        }
        if (this.depthVariance.isPresent()) {
            Assertions.assertEquals(this.depthVariance.get(), sentenceDataPoint.depthVariance, 0.01, "depthVariance");
            System.out.println("OK: depthVariance");
        }

        if (this.leaves.isPresent()) {
            Assertions.assertEquals(this.leaves.get(), sentenceDataPoint.leaves, "leaves");
            System.out.println("OK: leaves");
        }

        if (this.treeDegree.isPresent()) {
            Assertions.assertEquals(this.treeDegree.get(), sentenceDataPoint.treeDegree, "treeDegree");
            System.out.println("OK: treeDegree");
        }
        if (this.treeDegreeMean.isPresent()) {
            Assertions.assertEquals(this.treeDegreeMean.get(), sentenceDataPoint.treeDegreeMean, 0.01, "treeDegreeMean");
            System.out.println("OK: treeDegreeMean");
        }
        if (this.treeDegreeVariance.isPresent()) {
            Assertions.assertEquals(this.treeDegreeVariance.get(), sentenceDataPoint.treeDegreeVariance, 0.01, "treeDegreeVariance");
            System.out.println("OK: treeDegreeVariance");
        }

        if (this.headFinalRatio.isPresent()) {
            Assertions.assertEquals(this.headFinalRatio.get(), sentenceDataPoint.headFinalRatio, 0.01, "headFinalRatio");
            System.out.println("OK: headFinalRatio");
        }
        if (this.headFinalDistance.isPresent()) {
            Assertions.assertEquals(this.headFinalDistance.get(), sentenceDataPoint.headFinalDistance, "headFinalDistance");
            System.out.println("OK: headFinalDistance");
        }
        if (this.crossings.isPresent()) {
            Assertions.assertEquals(this.crossings.get(), sentenceDataPoint.crossings, "crossings");
            System.out.println("OK: crossings");
        }
        return true;
    }

    public static ExpectedValues getExpectedForJumped() {
        return ExpectedValues
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
    }

    public static ExpectedValues getExpectedForGeklappt() {
        return ExpectedValues
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
    }
}
