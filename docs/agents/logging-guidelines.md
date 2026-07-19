# Logging Guidelines

Companion to [AGENTS.md](../../AGENTS.md). For configuring a logging backend in applications that
*use* Matrix, see [docs/logging.md](../logging.md) — this document covers logging *inside* the
Matrix codebase itself.

**CRITICAL:** In production library code under non-example `src/main`, use the matrix-core Logger utility instead of System.out, System.err, println, log4j, or SLF4J direct usage. Console output is acceptable in examples, command-line demos, benchmarks, tests that explicitly inspect output, and Gradle task hooks where the output is the user-facing behavior.

## Use the Logger Class

The project provides a lightweight logging utility in `se.alipsa.matrix.core.util.Logger` backed by the JDK `System.Logger` facade. This keeps Matrix free of logging-framework dependencies while still allowing applications to route logs through the JVM's configured logger backend.

**Pattern:**
```groovy
import se.alipsa.matrix.core.util.Logger

@CompileStatic
class MyClass {
  private static final Logger log = Logger.getLogger(MyClass)

  void myMethod() {
    log.info("Starting process")
    log.debug("Dataset $name already exists")
    log.warn("Falling back to alternative approach")
    log.error("Operation failed: ${exception.message}", exception)
  }
}
```

**Available log levels:** DEBUG, INFO, WARN, ERROR

**String interpolation:** Use Groovy string interpolation in log messages:
```groovy
// Good - Idiomatic Groovy
log.info("Table $datasetName.$tableName created successfully")
log.debug("Processing ${count} items")

// Avoid - Java/SLF4J style (not necessary in Groovy)
log.info("Table %s.%s created successfully", datasetName, tableName)
```

## Replace Existing Logging

When implementing features or modifying production library code, **always replace** any of the following with Logger unless the file is an example, benchmark, test, or build script where console output is intentional:

**Replace System.out/System.err:**
```groovy
// Before
System.out.println("Processing started")
println("Dataset created")

// After
log.info("Processing started")
log.info("Dataset created")
```

**Replace direct SLF4J usage:**
```groovy
// Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private static final Logger logger = LoggerFactory.getLogger(MyClass)

// After
import se.alipsa.matrix.core.util.Logger

private static final Logger log = Logger.getLogger(MyClass)
```

**Replace log4j:**
```groovy
// Before
import org.apache.log4j.Logger

private static final Logger logger = Logger.getLogger(MyClass)

// After
import se.alipsa.matrix.core.util.Logger

private static final Logger log = Logger.getLogger(MyClass)
```

## When to Log

- **INFO**: Important operations, dataset creation, successful completions, user-facing events
- **DEBUG**: Detailed diagnostic information, data that exists checks, intermediate results
- **WARN**: Recoverable errors, fallback mechanisms activated, deprecation warnings
- **ERROR**: Exceptions, failures, unrecoverable errors

## Exceptions

When logging exceptions, use the overload that accepts a Throwable:
```groovy
try {
  // operation
} catch (Exception e) {
  log.error("Operation failed: ${e.message}", e)
  throw new CustomException("...", e)
}
```
