# CLAUDE.md and copilot

This file provides guidance to Claude Code (claude.ai/code) and github copilot when working with code in this repository.

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

| Module                 | Purpose                                                                   |
|------------------------|---------------------------------------------------------------------------|
| **matrix-core**        | Core Matrix/Grid classes, basic statistics, data conversion               |
| **matrix-stats**       | Statistical tests (t-test, correlation, regression) using Smile ML        |
| **matrix-datasets**    | Common datasets (mtcars, iris, diamonds, etc.)                            |
| **matrix-charts**      | Grammar of Graphics (ggplot2-style) charting with SVG output              |
| **matrix-csv**         | Advanced CSV import/export via commons-csv                                |
| **matrix-groovy-ext**  | Groovy extensions to Number and BigDecimal enabling more ideomatic groovy |
| **matrix-json**        | JSON import/export via Jackson                                            |
| **matrix-spreadsheet** | Excel/OpenOffice import/export via Apache POI                             |
| **matrix-sql**         | Database interaction via JDBC                                             |
| **matrix-parquet**     | Parquet format support                                                    |
| **matrix-smile**       | Smile ML library integration                                              |
| **matrix-bom**         | Bill of Materials for dependency management                               |

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

| Module(s)                   | Max JDK | Reason                                                                  |
|-----------------------------|---------|-------------------------------------------------------------------------|
| matrix-parquet, matrix-avro | 21      | Hadoop 3.4.x incompatible with JDK 22+                                  |
| matrix-charts               | 21      | JavaFX 23.x requires JDK 21; 24+ requires JDK 22+                       |
| matrix-smile                | 21      | Smile 4.x used requires a minimum of java 21; Smile 5+ requires Java 25 |

## Code Style

- Use `@CompileStatic` annotation on classes for performance-critical code.
- Java compilation target: release 21
- Groovy compiles both .java and .groovy files (no separate Java srcDir)
- MIT License
- **Always create or modify tests when adding or changing features** - tests go in `src/test/groovy/` using JUnit 5

## Idiomatic Groovy Patterns

### Groovy's Natural Numeric Type: BigDecimal

**IMPORTANT:** In Groovy, `BigDecimal` is the natural/default numeric type for decimal literals. When writing new code, **always use `BigDecimal` as your first choice** for numeric operations unless you have a specific reason to use primitives.

**Default to BigDecimal:**
```groovy
// Good - Natural Groovy style with BigDecimal
private List<BigDecimal> calculateValues(BigDecimal x1, BigDecimal x2) {
  BigDecimal midpoint = (x1 + x2) / 2
  BigDecimal distance = (x2 - x1).abs()
  return [midpoint, distance]
}

// Avoid - Using double when BigDecimal would be natural
private List<Double> calculateValues(double x1, double x2) {
  double midpoint = (x1 + x2) / 2
  double distance = Math.abs(x2 - x1)
  return [midpoint, distance]
}
```

**When methods accept or return numeric types, prefer BigDecimal:**
```groovy
// Good
private BigDecimal computeOffset(BigDecimal value, BigDecimal curvature) {
  return value * curvature * 0.5
}

// Avoid
private double computeOffset(double value, double curvature) {
  return value * curvature * 0.5d
}
```

### BigDecimal vs double
**Primary Goal:** Write beautiful, easy-to-read, idiomatic Groovy code. Readability > precision optimization.

**When to use Number:**
- as parameters in the end user API (numeric return type should primarily be BigDecimal though) 

**When to use BigDecimal:**
- Business logic and data transformations
- Scale operations (ranges, domains, transforms)
- When numeric literals naturally appear in code (`1.0G`, `2.5G`)

**When to use double:**
- For performance critical code pieces e.g. in loops with complex calculations

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
Only add a `G` suffix if the compiler does not understand that its a big decimal and groovys 
natural coercion does not handle it e.g
if you are calling a method that is overloaded to accept both Double and BigDecimal, and you want to be explicitly clear (for code readability) which one you are targeting,
```groovy
someMethod(1.2G) // Explicitly stating "This is a BigDecimal"
```
Otherwise, G suffix is only needed when a BigInteger is needed:
```groovy
def v = 12 // This will be an Integer
def v = 12G // This will be a BigInteger
def v = 12.0 // This will be a BigDecimal, no need for G suffix
```


```groovy
// Good
List<BigDecimal> range = [1.0, 6.0]
BigDecimal half = 0.5

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

// Min/max with chainable syntax - use integer literals for integer values
BigDecimal binIndex = 0.max(value.min(100))  // Clamp to [0, 100]
BigDecimal t = normalized.min(1).max(0)      // Clamp to [0, 1] - use 1 and 0, not 1.0 and 0.0

// Works with mixed Number types
BigDecimal result = someValue.min(breaks.size() - 2)
```

**When to use extension methods:**
- Always preferred over java.lang.Math
- Type conversions don't make it verbose
- **NOT** when working with primitive doubles (avoid boxing overhead) but consider refactoring the code to use BigDecimal instead

**Example - Good use of extensions:**
```groovy
// Before - verbose
double epsilon = Math.max(Math.ulp(boundaryPoint), Math.ulp(d)) * 10.0d

// After - idiomatic with extensions
BigDecimal epsilon = [boundaryPoint.ulp(), d.ulp()].max() * 10
```

**Example - Avoid extensions for doubles:**
```groovy
// May be problematic, OK only if normalized must be used as a double
double normalized = Math.max(0, Math.min(1, value))

// Good - Math methods are clearer for BigDecimal operations
BigDecimal normalized = 0.max(1.min(value))

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
BigDecimal bd = value as BigDecimal
// or if double is needed
double x = value as double
```

**❌ Don't force BigDecimal where double is clearer:**
```groovy
// where double is required
double px = cx + radius * Math.sin(angle)  // ✓ Clear
double px = cx + (radius * (angle as BigDecimal).sin()) as double  // ✗ Verbose
// However, prefer this
BigDecimal px = cx + radius * angle.sin()  // ✓ Clear and idiomatic
```

**❌ Don't use switch statements for simple value mappings:**
```groovy
// Avoid - verbose switch statement
private String getDashArray(String type) {
  switch (type?.toLowerCase()) {
    case 'dashed': return '5,5'
    case 'dotted': return '2,2'
    case 'longdash': return '10,5'
    case 'twodash': return '10,5,2,5'
    case 'solid':
    default: return null
  }
}

// Prefer - idiomatic Map lookup
private String getDashArray(String type) {
  final Map<String, String> dashArray = [
      dashed: '5,5',
      dotted: '2,2',
      longdash: '10,5',
      twodash: '10,5,2,5'
  ]
  dashArray[type?.toLowerCase()]
}
```

**❌ Don't use unnecessary type casts in comparisons:**
```groovy
// Avoid
if ((alpha as double) < 1.0) {
  rect.addAttribute('opacity', alpha)
}

// Prefer - Groovy handles numeric comparisons naturally
if (alpha < 1.0) {
  rect.addAttribute('opacity', alpha)
}
```

**❌ Don't cast Number properties unnecessarily:**
```groovy
// Avoid - unnecessary casts
BigDecimal result = i * dotDiameter * (stackratio as BigDecimal)
BigDecimal width = hexWidth * (dotsize as BigDecimal) * 0.9

// Prefer - let Groovy's type coercion work
BigDecimal result = i * dotDiameter * stackratio
BigDecimal width = hexWidth * dotsize * 0.9
```

**❌ Don't calculate unused variables:**
```groovy
// Avoid - yRange is calculated but never used
BigDecimal xRange = xMax - xMin
BigDecimal yRange = yMax - yMin  // Not used anywhere
BigDecimal hexWidth = xRange / bins

// Prefer - only calculate what you need
BigDecimal xRange = xMax - xMin
BigDecimal hexWidth = xRange / bins
```

**❌ Don't use Math.max/Math.min for array index clamping:**
```groovy
// Avoid - verbose with Math methods
int rawIdx = (ratio * (fillColors.size() - 1)) as int
int colorIdx = Math.max(0, Math.min(rawIdx, fillColors.size() - 1))

// Prefer - idiomatic BigDecimal chaining
BigDecimal rawIdx = ratio * (fillColors.size() - 1)
BigDecimal colorIdx = 0.max(rawIdx.min(fillColors.size() - 1))
```

**✅ Use explicit types when needed for type checker:**
```groovy
// When calling methods on transformed values, provide explicit types
BigDecimal p1 = binScale?.transform(sampleBinCenter) as BigDecimal
BigDecimal p2 = binScale?.transform(sampleBinCenter + bw) as BigDecimal
BigDecimal binWidthPx = (p2 - p1).abs()  // Now type checker understands p1 and p2

// Avoid - type checker can't infer types
def p1 = binScale?.transform(sampleBinCenter)
def p2 = binScale?.transform(sampleBinCenter + bw)
BigDecimal binWidthPx = (p2 - p1).abs()  // Error: cannot find method minus
```

**✅ Use .length for arrays, .size() for collections:**
```groovy
// Good - arrays use .length
double[][] points = createPoints()
for (int i = 1; i < points.length; i++) { }

// Good - collections use .size()
List<String> items = []
for (int i = 0; i < items.size(); i++) { }
```

**✅ Prefer Number type in method signatures for flexibility:**
```groovy
// Good - accepts any Number type
private static String createHexagonPath(Number cx, Number cy, Number width, Number height) {
  BigDecimal w = width / 2
  BigDecimal h = height / 2
  // ...
}

// Avoid - too specific
private static String createHexagonPath(double cx, double cy, double width, double height) {
  // Forces caller to cast or convert
}
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
- **Charting**: gsvg 0.4.0, JFreeChart 1.5.6, JavaFX 23.0.2
- **Statistics**: Smile 4.4.2, commons-math3
- **Data formats**: Jackson, Apache POI, commons-csv
