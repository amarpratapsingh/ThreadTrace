package com.threadtrace.log;

import java.util.Locale;
import java.util.regex.*;

public class LogParser
{
    private static final String DEFAULT_PATTERN =
        "^(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) " +
        "\\[(?<level>INFO|WARN|ERROR)\\] " +
        "\\[(?<component>[^\\]]+)\\] " +
        "(?<message>.*)$";

    private static Pattern logPattern = Pattern.compile(DEFAULT_PATTERN);

    public static void setFormat(String format)
    {
        if (format == null || format.equals("default")) return;

        String regex;
        if (format.startsWith("custom:"))
            regex = format.substring(7);
        else
        {
            System.err.println("Unknown format: " + format + ". Using default.");
            return;
        }

        try {
            logPattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            System.err.println("Invalid regex in --format: " + e.getMessage());
            System.err.println("Falling back to default format.");
            logPattern = Pattern.compile(DEFAULT_PATTERN);
        }
    }

    private static String levelFilter;
    private static String sinceTimestamp;
    private static String untilTimestamp;
    private static Pattern searchPattern;

    public static void setLevelFilter(String levels) { levelFilter = levels; }
    public static void setSince(String since) { sinceTimestamp = since; }
    public static void setUntil(String until) { untilTimestamp = until; }
    public static void setSearch(String search)
    {
        searchPattern = search == null ? null : Pattern.compile(search, Pattern.CASE_INSENSITIVE);
    }

    public static void parseAndRecord(String rawLine, LogReport report)
    {
        if(rawLine == null || rawLine.isBlank())
        {
            return;
        }

        Matcher match = logPattern.matcher(rawLine);
        if(match.matches())
        {
            String level = match.group("level").toUpperCase(Locale.ROOT);
            String filter = levelFilter == null ? null : levelFilter.toUpperCase(Locale.ROOT);
            String component = match.group("component");

            String timeStamp = match.group("timestamp");
            String timeBucket = timeStamp.substring(0, 16);

            String message = match.group("message");

            if(filter != null && !level.equals(filter)) return;
            if (levelFilter != null && !level.contains(levelFilter)) return;
            if (sinceTimestamp != null && timeStamp.compareTo(sinceTimestamp) < 0) return;
            if (untilTimestamp != null && timeStamp.compareTo(untilTimestamp) > 0) return;
            if (searchPattern != null && searchPattern.matcher(rawLine).find())
                report.addMatchedLine(rawLine);

            switch (level)
            {
                case "ERROR" -> {
                    report.incrementErrors(component);
                    report.incrementErrorsByTime(timeBucket);
                    report.incrementErrorsByMessage(message);
                }
                case "WARN" -> {
                    report.incrementWarnings(component);
                    report.incrementWarningsByTime(timeBucket);
                    report.incrementWarningsByMessage(message);
                }
            }
        }
    }

}