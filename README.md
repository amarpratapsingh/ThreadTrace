# ThreadTrace

Multi-threaded CLI log analyzer. Parses server logs in parallel, breaks down errors and warnings by component, time, and message.

## Requirements

- Java 21+

## Quick start

```bash
# Download the JAR
curl -L -o threadtrace.jar https://github.com/user/threadtrace/releases/latest/download/threadtrace.jar

# Analyze a log file
java -jar threadtrace.jar app.log
```

## Build from source

```bash
git clone https://github.com/user/threadtrace.git
cd threadtrace
mvn package
java -jar target/threadtrace.jar app.log
```

## Usage

```
Usage: threadtrace [options] [file]

Options:
  --file <path>         Log file to analyze (default: test.log)
  --threads <n>         Number of worker threads (default: all CPUs)
  --by-component        Show errors/warnings per component
  --by-message          Show errors/warnings per message
  --by-time             Show errors/warnings per minute
  --level <lvl>         Filter by level (ERROR, WARN, or both)
  --since <time>        Only entries after this time (yyyy-MM-dd HH:mm:ss)
  --until <time>        Only entries before this time
  --search <pattern>    Search for matching log lines
  --format <fmt>        Log format: default or custom:<regex>
  --help                Print this help
```

### Examples

```bash
# Show all analysis sections
java -jar threadtrace.jar app.log

# Show only time histogram
java -jar threadtrace.jar --by-time app.log

# Filter to ERROR level, search for timeout
java -jar threadtrace.jar --level ERROR --search "timeout" app.log

# Custom thread count
java -jar threadtrace.jar --threads 4 app.log

# Custom log format
java -jar threadtrace.jar --format 'custom:^(?<timestamp>...)' app.log
```

## Log format

Default pattern:

```
2026-06-12 08:40:01 [ERROR] [com.example.Service] Something broke
```

Supports custom regex via `--format custom:<regex>`. The regex must include named capture groups: `(?<timestamp>)`, `(?<level>)`, `(?<component>)`, `(?<message>)`.

## How it works

Splits the log file into equal byte-sized chunks, assigns each to a worker thread, parses lines in parallel using `RandomAccessFile`, then merges results into a unified report i.e the Master Report

## License

MIT
