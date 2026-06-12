package com.threadtrace;

import com.threadtrace.concurrency.*;
import com.threadtrace.log.LogParser;
import com.threadtrace.log.LogReport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Tracer {
    public static void main(String[] args)
    {
        CliOptions options = CliOptions.parse(args);
        if (options == null) return;

        File file = new File(options.filePath);
        if (!file.exists())
        {
            System.out.println("File not found: " + options.filePath);
            return;
        }

        long fileSize = file.length();
        long chunks = fileSize / options.threadCount;

        LogParser.setFormat(options.format);
        if (options.levelFilter != null) LogParser.setLevelFilter(options.levelFilter);
        if (options.since != null) LogParser.setSince(options.since);
        if (options.until != null) LogParser.setUntil(options.until);
        if (options.search != null) LogParser.setSearch(options.search);

        System.out.println("Processing file with " + options.threadCount + " parallel worker threads...");

        try (ExecutorService executor = Executors.newFixedThreadPool(options.threadCount))
        {
            List<LogTask> tasks = new ArrayList<>();

            for (int i = 0; i < options.threadCount; i++)
            {
                long startByte = i * chunks;
                long endByte = (i == options.threadCount - 1) ? fileSize : (startByte + chunks);
                tasks.add(new LogTask(options.filePath, startByte, endByte));
            }

            try {
                List<Future<LogReport>> futures = executor.invokeAll(tasks);
                LogReport masterLogReport = new LogReport();

                for (Future<LogReport> f : futures)
                    masterLogReport.Merge(f.get());

                System.out.println("\n===== THREADTRACE ANALYSIS REPORT =====");
                System.out.println("Total Critical Errors: " + masterLogReport.getErrors());
                System.out.println("Total System Warnings: " + masterLogReport.getWarning());

                if (options.showComponent) printComponentBreakdown(masterLogReport);
                if (options.showMessage) printMessageGrouping(masterLogReport);
                if (options.showTime) printTimeHistogram(masterLogReport);
                if (options.search != null) printSearchResults(masterLogReport, options.search);

                System.out.println("=======================================");
            }
            catch (InterruptedException | ExecutionException e)
            {
                e.printStackTrace();
            }
        }
        catch(Exception e)
        {
            System.err.println("Execution Failed: " + e.getMessage());
        }
    }

    private static void printComponentBreakdown(LogReport report)
    {
        System.out.println("\n----- COMPONENT BREAKDOWN -----");
        System.out.printf("%-40s %8s %8s%n", "Component", "ERRORS", "WARNS");
        System.out.println("-".repeat(58));
        var breakdown = report.getComponent();
        for (var entry : breakdown.entrySet())
        {
            String name = entry.getKey().replace("com.threadtrace.", "");
            System.out.printf("%-40s %8d %8d%n",
                name, entry.getValue()[0], entry.getValue()[1]);
        }
    }

    private static void printMessageGrouping(LogReport report)
    {
        System.out.println("\n----- MESSAGE GROUPING -----");
        System.out.printf("%-85s %8s %8s%n", "Message", "ERRORS", "WARNS");
        System.out.println("-".repeat(103));
        for (var entry : report.getMessage().entrySet())
        {
            System.out.printf("%-85s %8d %8d%n",
                entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
        }
    }

    private static void printTimeHistogram(LogReport report)
    {
        System.out.println("\n----- TIME HISTOGRAM -----");
        System.out.printf("%-20s %8s %8s%n", "Time", "ERRORS", "WARNS");
        System.out.println("-".repeat(38));
        for (var entry : report.getTimeBucket().entrySet())
        {
            System.out.printf("%-20s %8d %8d%n",
                entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
        }
    }

    private static void printSearchResults(LogReport report, String pattern)
    {
        var matches = report.getMatchedLines();
        System.out.println("\n----- SEARCH RESULTS (\"" + pattern + "\") -----");
        if (matches.isEmpty())
        {
            System.out.println("No matches found.");
            return;
        }
        for (int i = 0; i < matches.size(); i++)
            System.out.println((i + 1) + ": " + matches.get(i));
    }

    private static class CliOptions
    {
        String filePath = "test.log";
        int threadCount = Runtime.getRuntime().availableProcessors();
        boolean showComponent = false;
        boolean showMessage = false;
        boolean showTime = false;
        String levelFilter;
        String since;
        String until;
        String search;
        String format;

        static CliOptions parse(String[] args)
        {
            var opts = new CliOptions();

            for (int i = 0; i < args.length; i++)
            {
                switch (args[i])
                {
                    case "--file" -> {
                        if(++i >= args.length)
                        {
                            error("--file requires a path"); return null;
                        }
                        opts.filePath = args[i];
                    }
                    case "--threads" -> {
                        if(++i >= args.length)
                        {
                            error("--threads requires a number"); return null;
                        }
                        opts.threadCount = Integer.parseInt(args[i]);
                    }
                    case "--by-component" -> opts.showComponent = true;
                    case "--by-message" -> opts.showMessage = true;
                    case "--by-time" -> opts.showTime = true;
                    case "--level" -> {
                        if(++i >= args.length)
                        {
                            error("--level requires a value"); return null;
                        }
                        opts.levelFilter = args[i].toUpperCase();
                    }
                    case "--since" -> {
                        if(++i >= args.length)
                        {
                            error("--since requires a timestamp"); return null;
                        }
                        opts.since = args[i];
                    }
                    case "--until" -> {
                        if(++i >= args.length)
                        {
                            error("--until requires a timestamp"); return null;
                        }
                        opts.until = args[i];
                    }
                    case "--search" -> {
                        if(++i >= args.length)
                        {
                            error("--search requires a pattern"); return null;
                        }
                        opts.search = args[i];
                    }
                    case "--format" -> {
                        if(++i >= args.length)
                        {
                            error("--format requires a value"); return null;
                        }
                        opts.format = args[i];
                    }
                    case "--help" -> {
                        printHelp(); return null;
                    }
                    default -> {
                        if (!args[i].startsWith("--"))
                            opts.filePath = args[i];
                    }
                }
            }

            boolean anyFlag = opts.showComponent || opts.showMessage || opts.showTime;
            opts.showComponent = opts.showComponent || !anyFlag;
            opts.showMessage = opts.showMessage || !anyFlag;
            opts.showTime = opts.showTime || !anyFlag;

            return opts;
        }

        private static void error(String msg)
        {
            System.out.println("Error: " + msg);
            printHelp();
        }

        private static void printHelp()
        {
            System.out.println("Usage: threadtrace [options] [file]");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --file <path>         Log file to analyze (default: test.log)");
            System.out.println("  --threads <n>         Number of worker threads (default: all CPUs)");
            System.out.println("  --by-component        Show errors/warnings per component");
            System.out.println("  --by-message          Show errors/warnings per message");
            System.out.println("  --by-time             Show errors/warnings per minute");
            System.out.println("  --level <lvl>         Filter by level (ERROR, WARN, or both)");
            System.out.println("  --since <time>        Only entries after this time (yyyy-MM-dd HH:mm:ss)");
            System.out.println("  --until <time>        Only entries before this time");
            System.out.println("  --search <pattern>    Search for matching log lines");
            System.out.println("  --format <fmt>        Log format: default or custom:<regex>");
            System.out.println("  --help                Print this help");
        }
    }
}
