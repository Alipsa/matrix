# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Matrix is a Groovy library for working with tabular (2D) data. It provides Matrix and Grid classes along with specialized modules for statistics, visualization, and data I/O formats. The project targets **JDK 21** and uses **Groovy 5.0.3**.

## Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :matrix-core:build
./gradlew :matrix-charts:build

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Check for dependency updates
./gradlew dependencyUpdates
```

## Test Commands

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :matrix-core:test
./gradlew :matrix-charts:test

# Run a single test class
./gradlew :matrix-charts:test --tests "gg.GgPlotTest"

# Run a single test method
./gradlew :matrix-charts:test --tests "gg.GgPlotTest.testPointChartRender"

# Include slow integration tests
./gradlew test -PrunSlowTests=true

# Enable external tests (BigQuery, GSheets)
RUN_EXTERNAL_TESTS=true ./gradlew test

# Run GUI tests in headless mode (for CI)
./gradlew :matrix-charts:test -Pheadless=true
```

## Module Structure

| Module | Purpose |
|--------|---------|
| **matrix-core** | Core Matrix/Grid classes, basic statistics, data conversion |
| **matrix-stats** | Statistical tests (t-test, correlation, regression) using Smile ML |
| **matrix-datasets** | Common datasets (mtcars, iris, diamonds, etc.) |
| **matrix-charts** | Grammar of Graphics (ggplot2-style) charting with SVG output |
| **matrix-csv** | Advanced CSV import/export via commons-csv |
| **matrix-json** | JSON import/export via Jackson |
| **matrix-spreadsheet** | Excel/OpenOffice import/export via Apache POI |
| **matrix-sql** | Database interaction via JDBC |
| **matrix-parquet** | Parquet format support |
| **matrix-smile** | Smile ML library integration |
| **matrix-bom** | Bill of Materials for dependency management |

## Architecture

### matrix-core
- `Matrix`: Primary tabular data structure with typed columns
- `Grid`: 2D array-like structure for homogeneous data
- `Stat`: Basic statistics (sum, mean, median, sd, frequency, groupBy)
- `ListConverter`: Type conversion utilities

### matrix-charts (Grammar of Graphics)
The charting module implements a ggplot2-like API using gsvg for SVG rendering:

```
GgPlot.groovy     → Static factory methods (ggplot, aes, geom_*, scale_*, theme_*)
GgChart.groovy    → Chart specification container with plus() operators
GgRenderer.groovy → Rendering pipeline orchestrator
```

**Data flow**: Data + Aes → Stat transformation → Position adjustment → Scale computation → Coord transformation → Geom rendering → Theme styling → SVG output

**Key patterns**:
- Deferred rendering: collect specifications, render on `chart.render()`
- `@CompileStatic` annotations for performance
- Scale auto-detection: numeric data → continuous scales, string data → discrete scales

### Extension Modules
Modules like matrix-smile use Groovy extension methods registered via `META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule` to add methods like `matrix.toSmileDataFrame()`.

## JDK Constraints

| Module(s) | Max JDK | Reason |
|-----------|---------|--------|
| matrix-parquet, matrix-avro | 21 | Hadoop 3.4.x incompatible with JDK 22+ |
| matrix-charts | 21 | JavaFX 23.x requires JDK 21; 24+ requires JDK 22+ |
| matrix-smile | 21 | Smile 4.x used; Smile 5+ requires Java 25 |

## Code Style

- Use `@CompileStatic` annotation on classes for performance-critical code. 
- Java compilation target: release 21
- Groovy compiles both .java and .groovy files (no separate Java srcDir)
- MIT License
- **Always create or modify tests when adding or changing features** - tests go in `src/test/groovy/` using JUnit 5

## Key Dependencies

- **Groovy**: 5.0.3 (groovy, groovy-sql, groovy-ginq)
- **Testing**: JUnit Jupiter 6.0.1
- **Charting**: gsvg 0.2.0, JFreeChart 1.5.6, JavaFX 23.0.2
- **Statistics**: Smile 4.4.2, commons-math3
- **Data formats**: Jackson, Apache POI, commons-csv
