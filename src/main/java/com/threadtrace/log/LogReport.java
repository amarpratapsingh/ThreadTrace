package com.threadtrace.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LogReport
{
    private final Map<String, ComponentStats> components = new TreeMap<>();
    private final Map<String, int[]> timeBuckets = new TreeMap<>();
    private final Map<String, int[]> messageGroups = new TreeMap<>();
    private final List<String> matchedLines = new ArrayList<>();
    
    public void addMatchedLine(String line)
    {
        matchedLines.add(line);
    }
    
    public List<String> getMatchedLines()
    {
        return matchedLines;
    }
    
    private static class ComponentStats
    {
        int errorCount = 0;
        int warningCount = 0;
    }
    //Errors
    public void incrementErrors(String component)
    {
        components.computeIfAbsent(component, k -> new ComponentStats()).errorCount++;
    }

    public void incrementErrorsByTime(String timeBucket)
    {
        timeBuckets.computeIfAbsent(timeBucket, k -> new int[2])[0]++;
    }

    public void incrementErrorsByMessage(String message)
    {
        messageGroups.computeIfAbsent(message, k -> new int[2])[0]++;
    }

    //Warnings
    public void incrementWarnings(String component)
    {
        components.computeIfAbsent(component, k -> new ComponentStats()).warningCount++;
    }

    public void incrementWarningsByTime(String timeBucket)
    {
        timeBuckets.computeIfAbsent(timeBucket, k -> new int[2])[1]++;
    }

    public void incrementWarningsByMessage(String message)
    {
        messageGroups.computeIfAbsent(message, k -> new int[2])[1]++;
    }

    public Map<String, int[]> getComponent()
    {
        Map<String, int[]> resMap = new TreeMap<>();
        for(var entry : components.entrySet())
        {
            resMap.put(entry.getKey(), new int[]{entry.getValue().errorCount, entry.getValue().warningCount});
        }
        return resMap;
    }

    public Map<String, int[]> getTimeBucket()
    {
        return timeBuckets;
    }

    public Map<String, int[]> getMessage()
    {
        return messageGroups;
    }

    public void Merge(LogReport other)
    {
        for(var entry : other.components.entrySet())
        {
            String component = entry.getKey();
            ComponentStats stats = entry.getValue();

            for(int i=0; i<stats.errorCount; i++)
            {
                incrementErrors(component);
            }
            for(int i=0; i<stats.warningCount; i++)
            {
                incrementWarnings(component);   
            }
        }

        for (var entry : other.timeBuckets.entrySet())
        {
            String bucket = entry.getKey();
            int[] counts = entry.getValue();
            for (int i=0; i<counts[0]; i++)
            {
                incrementErrorsByTime(bucket);
            }
            for (int i=0; i<counts[1]; i++)
            {
                incrementWarningsByTime(bucket);
            }
        }

        for(var entry : other.messageGroups.entrySet())
        {
            int[] count = entry.getValue();
            for(int i=0; i<count[0]; i++)
            {
                incrementErrorsByMessage(entry.getKey());
            }

            for(int i=0; i<count[1]; i++)
            {
                incrementWarningsByMessage(entry.getKey());
            }
        }

        matchedLines.addAll(other.matchedLines);
    }

    public int getErrors()
    {
        return components.values().stream().mapToInt(c -> c.errorCount).sum();
    }
    
    public int getWarning()
    {
        return components.values().stream().mapToInt(c -> c.warningCount).sum();
    }
}
