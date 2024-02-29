package org.texttechnologylab.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.texttechnologylab.dependency.app.Run;
import org.texttechnologylab.dependency.data.DocumentDataPoint;
import org.texttechnologylab.json.GraphIteratorTest;
import org.texttechnologylab.utils.ExpectedValues;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RunIntegrationTest {


    @Test
    @Tag("integration")
    public void testRun(@TempDir Path tempDir) throws JsonSyntaxException, IOException {
        Run.main(
            new String[] { GraphIteratorTest.class.getClassLoader().getResource("json/test-geklappt.json").getPath(), tempDir.toString() }
        );

        Path outputFile = tempDir.resolve("test-geklappt.json");
        DocumentDataPoint dp = new Gson().fromJson(Files.readString(outputFile), DocumentDataPoint.class);

        ExpectedValues.getExpectedForGeklappt().assertEquals(dp.getSentences().get(0));

        Assertions.assertTrue(true);
    }
}
