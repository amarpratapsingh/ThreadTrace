package com.threadtrace;
import com.threadtrace.log.LogParser;
import com.threadtrace.log.LogReport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class TracerTest 
{
    private LogReport report;

    @BeforeEach
    void SetUp()
    {
        report = new LogReport();
    }

    @Test
    void testValidErrorLog()
    {
        String validLine = "2026-06-12 08:41:20 [ERROR] [com.payment.Processor] Payment failed.";
        LogParser.parseAndRecord(validLine, report);
        
        assertEquals(1, report.getErrors(), "Should record exactly 1 error");
        assertEquals(0, report.getWarning(), "Warnings should remain 0");
    }

    @Test
    void testCorruptedOrMalformedLog()
    {
        String junkLine = "This is a text with ERROR in it";
        LogParser.parseAndRecord(junkLine, report);

        assertAll("Ensure malformed strings are ignored",
            () -> assertEquals(0, report.getErrors()),
            () -> assertEquals(0, report.getWarning())
        );
    }
}
