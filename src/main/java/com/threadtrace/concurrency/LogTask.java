package com.threadtrace.concurrency;

import com.threadtrace.log.LogParser;
import com.threadtrace.log.LogReport;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

public class LogTask implements Callable<LogReport>
{
    private final String filePath;
    private final long startByte;
    private final long endByte;

    public LogTask(String filepath, long startByte, long endByte)
    {
        this.filePath = filepath;
        this.startByte = startByte;
        this.endByte = endByte;
    }

    @Override
    public LogReport call() throws Exception {
        LogReport report = new LogReport();
        try(RandomAccessFile file = new RandomAccessFile(filePath, "r"))
        {
            file.seek(startByte);
            if(startByte != 0)
            {
                file.readLine();
            }

            String line;
            while ((file.getFilePointer()) <= endByte && (line = file.readLine()) != null)
            {
                LogParser.parseAndRecord(line, report);
            }
        }
        return report;
    }
}