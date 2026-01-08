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

## Idiomatic Groovy Patterns

### BigDecimal vs double
**Primary Goal:** Write beautiful, easy-to-read, idiomatic Groovy code. Readability > precision optimization.

**When to use BigDecimal:**
- Business logic and data transformations
- Scale operations (ranges, domains, transforms)
- When numeric literals naturally appear in code (`1.0G`, `2.5G`)

**When to use double:**
- Geometric/coordinate calculations (pixels, angles, radii)
- Rendering pipeline (SVG coordinates)
- Math operations that naturally use primitives (sin, cos, atan2, sqrt on doubles)

### Numeric Operations

**Prefer idiomatic Groovy operators:**
```groovy
// Good - Natural Groovy operators
BigDecimal result = (rMin + rMax) / 2
BigDecimal t = i / (n - 1)
BigDecimal power = 10 ** exponent

// Avoid - Verbose Java-style
BigDecimal result = (rMin + rMax).divide(TWO, MATH_CONTEXT)
BigDecimal t = BigDecimal.valueOf(i).divide(BigDecimal.valueOf(n - 1), MATH_CONTEXT)
BigDecimal power = BigDecimal.TEN.pow(exponent)
```

**Use BigDecimal literals with `G` suffix:**
```groovy
// Good
List<BigDecimal> range = [1.0G, 6.0G]
BigDecimal half = 0.5G

// Avoid
List<BigDecimal> range = [new BigDecimal('1.0'), new BigDecimal('6.0')]
BigDecimal half = BigDecimal.valueOf(0.5)
```

**Use clean type coercion:**
```groovy
// Good - Idiomatic Groovy
double x = value as double
BigDecimal bd = value as BigDecimal

// Avoid - Code smell
double x = (value as Number).doubleValue()
```

### BigDecimalExtension Methods

The `matrix-groovy-ext` module provides extension methods for BigDecimal:

```groovy
// Floor and ceiling (returns BigDecimal)
BigDecimal x = 3.7G
x.floor()  // → 3.0
x.ceil()   // → 4.0

// Logarithm base 10
BigDecimal value = 100G
value.log10()  // → 2.0

// Square root with default precision
BigDecimal area = 25.0G
area.sqrt()  // → 5.0 (uses MathContext.DECIMAL64)

// Unit in last place (for epsilon calculations)
BigDecimal epsilon = value.ulp() * 10

// Min/max with chainable syntax
BigDecimal binIndex = 0.max(value.min(100))  // Clamp to [0, 100]

// Works with mixed Number types
BigDecimal result = someValue.min(breaks.size() - 2)
```

**When to use extension methods:**
- Pattern appears 3+ times in a file
- Extension is shorter AND clearer than original
- Type conversions don't make it verbose
- **NOT** when working with primitive doubles (avoid boxing overhead)

**Example - Good use of extensions:**
```groovy
// Before - verbose
double epsilon = Math.max(Math.ulp(boundaryPoint), Math.ulp(d)) * 10.0d

// After - idiomatic with extensions
BigDecimal epsilon = [boundaryPoint.ulp(), d.ulp()].max() * 10
```

**Example - Avoid extensions for doubles:**
```groovy
// Good - Math methods are clearer for double operations
double normalized = Math.max(0, Math.min(1, value))

// Avoid - verbose type conversions
double normalized = (0G).max((value as BigDecimal).min(1G)) as double
```

### Anti-Patterns to Avoid

**❌ Don't create unnecessary constants:**
```groovy
// Avoid
static final BigDecimal TWO = 2.0G
result = value.divide(TWO, MATH_CONTEXT)

// Prefer
result = value / 2
```

**❌ Don't use `.doubleValue()` (code smell):**
```groovy
// Avoid
double x = (value as Number).doubleValue()

// Prefer
double x = value as double
```

**❌ Don't force BigDecimal where double is clearer:**
```groovy
// For coordinate/geometry operations, double is natural
double px = cx + radius * Math.sin(angle)  // ✓ Clear
double px = cx + (radius * (angle as BigDecimal).sin()) as double  // ✗ Verbose
```

### When in Doubt

Ask yourself:
1. **Does this make the code easier to read?** → Do it
2. **Does this make the code harder to read?** → Don't do it
3. **Is it about the same?** → Leave it alone

The goal is beautiful Groovy code, not BigDecimal everywhere.

## Key Dependencies

- **Groovy**: 5.0.3 (groovy, groovy-sql, groovy-ginq)
- **Testing**: JUnit Jupiter 6.0.1
- **Charting**: gsvg 0.3.0, JFreeChart 1.5.6, JavaFX 23.0.2
- **Statistics**: Smile 4.4.2, commons-math3
- **Data formats**: Jackson, Apache POI, commons-csv
