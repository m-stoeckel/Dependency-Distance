package org.texttechnologylab.utils;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;

public class ExpectedDocumentAnnotations {

    public Optional<String> dateYear = Optional.empty();
    public Optional<String> dateMonth = Optional.empty();
    public Optional<String> dateDay = Optional.empty();
    public Optional<String> timestamp = Optional.empty();

    public static ExpectedDocumentAnnotations.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ExpectedDocumentAnnotations expectedValues;

        public Builder() {
            expectedValues = new ExpectedDocumentAnnotations();
        }

        public ExpectedDocumentAnnotations build() {
            return expectedValues;
        }

        public ExpectedDocumentAnnotations.Builder dateYear(String value) {
            this.expectedValues.dateYear = Optional.of(value);
            return this;
        }

        public ExpectedDocumentAnnotations.Builder dateMonth(String value) {
            this.expectedValues.dateMonth = Optional.of(value);
            return this;
        }

        public ExpectedDocumentAnnotations.Builder dateDay(String value) {
            this.expectedValues.dateDay = Optional.of(value);
            return this;
        }

        public ExpectedDocumentAnnotations.Builder timestamp(String value) {
            this.expectedValues.timestamp = Optional.of(value);
            return this;
        }
    }

    public boolean assertEquals(Map<String, String> documentAnnotation) {
        if (this.dateYear.isPresent()) {
            Assertions.assertEquals(this.dateYear.get(), documentAnnotation.get("dateYear"), "dateYear");
            System.out.println("OK: dateYear");
        }
        if (this.dateMonth.isPresent()) {
            Assertions.assertEquals(this.dateMonth.get(), documentAnnotation.get("dateMonth"), "dateMonth");
            System.out.println("OK: dateMonth");
        }
        if (this.dateDay.isPresent()) {
            Assertions.assertEquals(this.dateDay.get(), documentAnnotation.get("dateDay"), "dateDay");
            System.out.println("OK: dateDay");
        }
        if (this.timestamp.isPresent()) {
            Assertions.assertEquals(this.timestamp.get(), documentAnnotation.get("timestamp"), "timestamp");
            System.out.println("OK: timestamp");
        }
        return true;
    }

    public static ExpectedDocumentAnnotations get20211223() {
        ExpectedDocumentAnnotations expected = ExpectedDocumentAnnotations
            .builder()
            .dateYear("2021")
            .dateMonth("12")
            .dateDay("13")
            .timestamp("1639350000000")
            .build();
        return expected;
    }
}
