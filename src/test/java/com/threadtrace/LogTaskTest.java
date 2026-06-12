package com.threadtrace;
import com.threadtrace.concurrency.LogTask;
import com.threadtrace.log.LogReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogTaskTest
{
    @TempDir
    Path tempDir;

    @Test
    void testLogTaskProcessesFileChunkCorrectly() throws Exception {
        Path testLogFile = tempDir.resolve("test-production.log");
        
        List<String> testLines = List.of(
            "2026-06-12 12:00:00 [ERROR] [com.System] Crash 1",
            "2026-06-12 12:01:00 [INFO] [com.System] Healthy line",
            "2026-06-12 12:02:00 [WARN] [com.System] Warning 1",
            "2026-06-12 12:03:00 [ERROR] [com.System] Crash 2"
        );
        Files.write(testLogFile, testLines);

        String filePath = testLogFile.toString();
        long fileSize = testLogFile.toFile().length();
        
        LogTask task = new LogTask(filePath, 0, fileSize);

        LogReport resultReport = task.call();

        assertAll("Verify file chunk aggregation metrics",
            () -> assertEquals(2, resultReport.getErrors(), "Should find exactly 2 errors"),
            () -> assertEquals(1, resultReport.getWarning(), "Should find exactly 1 warning")
        );
    }
}
