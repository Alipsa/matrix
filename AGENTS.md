# Repository Guidelines

## Project Structure & Module Organization
This is a Gradle multi-module Groovy/Java project. Each module lives in a `matrix-*` directory (for example `matrix-core`, `matrix-stats`, `matrix-csv`, `matrix-charts`, `matrix-sql`). Source code is primarily in `matrix-*/src/main/groovy` (with some `src/main/java`), and tests live in `matrix-*/src/test/groovy` or `matrix-*/src/test/java`. Shared docs are under `docs/` (tutorial and cookbook), while runnable examples live in `matrix-examples/`. Root build configuration is in `build.gradle`, `settings.gradle`, and `dependencies.gradle`.

## Build, Test, and Development Commands
- `./gradlew build`: build all modules.
- `./gradlew :matrix-core:build`: build a single module.
- `./gradlew test`: run all unit tests.
- `./gradlew :matrix-ggplot:test --tests "gg.GgPlotTest"`: run a single gg test class.
- `./gradlew test -PrunSlowTests=true`: include slow integration tests.
- `RUN_EXTERNAL_TESTS=true ./gradlew test`: enable external tests (BigQuery, GSheets).
- `./gradlew publishToMavenLocal`: publish artifacts locally.
- `./gradlew dependencyUpdates`: report newer dependency versions.

## Coding Style & Naming Conventions
Use Groovy 5.0.3 and target Java 21. Follow the existing 2-space indentation and import style in each file. Prefer `@CompileStatic` and only fall back to @CompileDynamic when the static compilation would be significantly more convoluted. Classes are PascalCase, methods/fields are camelCase, and packages follow `se.alipsa.matrix.*`. Test classes are named `*Test.groovy` or `*Test.java` and live in module test directories. Always add GroovyDoc for public classes and public methods. There is no enforced formatter, so match the surrounding file conventions.
Prefer idiomatic groovy constructs.
Bear in mind that Groovy is not Java, and while they interoperate seamlessly, Groovy has its own idioms and best practices that differ from Java. Write code that is idiomatic Groovy rather than Java code written in Groovy syntax. Some examples:
 - Use Groovy's native collection literals and methods e.g. closures, `each`, `collect`, and `findAll` instead of Java-style loops and streams. Use `[]` instead of `new ArrayList<>()` and `[:]` instead of `new LinkedHashMap<>()`/`new HashMap<>()`.
 - Prefer closures and higher-order functions over verbose anonymous classes.
 - == vs .equals(): Use `==` for value equality in Groovy, which handles nulls gracefully. == does NOT mean reference equality like in Java.
 - String interpolation: Use `"${var}"` for building strings instead of concatenation.
 - Default numeric type: Groovy uses BigDecimal as the default for decimal literals. Prefer BigDecimal over double/float unless performance is critical.

## Logging Guidelines

**CRITICAL:** Always use the matrix-core Logger utility instead of System.out, System.err, println, log4j, or SLF4J direct usage.

### Use the Logger Class

The project provides a lightweight logging utility in `se.alipsa.matrix.core.util.Logger` that automatically detects and uses SLF4J if available, otherwise falls back to System.out/err. This supports both project dependency usage (with SLF4J) and Groovy scripting usage (without SLF4J setup).

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

### Replace Existing Logging

When implementing features or modifying code, **always replace** any of the following with Logger:

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

### When to Log

- **INFO**: Important operations, dataset creation, successful completions, user-facing events
- **DEBUG**: Detailed diagnostic information, data that exists checks, intermediate results
- **WARN**: Recoverable errors, fallback mechanisms activated, deprecation warnings
- **ERROR**: Exceptions, failures, unrecoverable errors

### Exceptions

When logging exceptions, use the overload that accepts a Throwable:
```groovy
try {
  // operation
} catch (Exception e) {
  log.error("Operation failed: ${e.message}", e)
  throw new CustomException("...", e)
}
```

## DRY Principle (Don't Repeat Yourself)
**CRITICAL:** Avoid code duplication! Before implementing functionality, check if similar code already exists in the codebase. If you find duplicated code (identical or near-identical methods, constants, or logic in multiple files):

1. **Extract to Utility Class**: Create a shared utility class in an appropriate package to hold the common functionality. For example:
   - Color conversion logic → `ColorSpaceUtil` in the same package
   - Math utilities → `MathUtil` in a common utilities package
   - String processing → `StringUtil` in a common utilities package

2. **Update All References**: When extracting shared code, update ALL files that were using the duplicated code to use the new utility class. Don't leave any instances of the old duplicated code behind.

3. **Update Tests**: When moving code to a utility class, update any tests that were testing the old private methods to test the new utility class methods instead.

4. **Common Examples of Duplication to Avoid**:
   - Constants (like color space reference values, magic numbers, default values)
   - Conversion functions (color space, coordinate, unit conversions)
   - Validation logic (input checking, bounds checking, type validation)
   - Mathematical calculations used in multiple places
   - Parsing/formatting logic

5. **Check Before You Copy**: Before copying a method or code block from one file to another, ask: "Should this be shared?" If the answer is yes or maybe, extract it to a utility class instead of duplicating it.

**Example**: If you're implementing HCL color conversion and find similar code already exists in `ScaleColorManual`, don't copy it—extract it to a `ColorSpaceUtil` class that both can use.

## Testing Guidelines
JUnit Jupiter (JUnit 5) is the primary test framework. Always create tests for new features and update tests when behavior changes; place them in the relevant module’s `src/test` tree. Use `-PrunSlowTests=true` and `RUN_EXTERNAL_TESTS=true` only when you intend to run the slow or external suites. For chart rendering tests, prefer headless mode in CI: `./gradlew :matrix-charts:test -Pheadless=true` and `./gradlew :matrix-ggplot:test -Pheadless=true`. When a task is done, run the full test suite to guard against regressions (`./gradlew test`). **Always** run tests after a task is complete to ensure no regressions (except for documentation-only tasks).

## Commit & Pull Request Guidelines
Commit messages in this repo are short, imperative summaries (e.g., “Fix …”, “Update …”, “Add …”), optionally mentioning the module. For pull requests, include a concise summary, list the modules touched, and record the tests you ran (with commands). Link relevant issues and add screenshots for visual/chart output changes.

## Environment & Constraints
JDK 21 is required. Some modules (parquet/avro, charts, smile) enforce a maximum JDK 21 due to upstream dependencies, so keep toolchains aligned with that constraint.

## Planning
When creating plans:
- Always number individual tasks and issues. Use a hierarchical task numbering scheme where the overall feature gets the major number and tasks under it get minor numbers. For example:
```
  2. Unify numeric coercion and NA handling 
  2.1 [ ] Update `ScaleUtils.coerceToNumber` in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleUtils.groovy` to return `BigDecimal` (or `null`), treating `NaN`, `null`, and blank values consistently.
  2.2 [ ] Remove duplicate `coerceToNumber` implementations in `ScaleContinuous`, `ScaleXLog10`, `ScaleXSqrt`, `ScaleXReverse` and route all conversions through `ScaleUtils`.
```
- Use checkboxes `[ ]` for tasks that need to be done and `[x]` for completed tasks. A task is only done when the checkbox is marked as done. A checkbox is not marked as done until tests have been run successfully **and the specific test commands used (for example, `./gradlew :matrix-charts:test` or `./gradlew test`) have been recorded in the plan or PR description**.

## Claude and Copilot Guidance

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
./gradlew :matrix-ggplot:test --tests "gg.GgPlotTest"

# Run a single test method
./gradlew :matrix-ggplot:test --tests "gg.GgPlotTest.testPointChartRender"

# Include slow integration tests
./gradlew test -PrunSlowTests=true

# Enable external tests (BigQuery, GSheets)
RUN_EXTERNAL_TESTS=true ./gradlew test

# Run GUI tests in headless mode (for CI)
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true

# Fast unit tests only (no chart rendering) — use for quick dev-cycle feedback
./gradlew :matrix-ggplot:testFast

# Full test suite (always run before merge)
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
```

## Testing Patterns

### Testing SVG Chart Output (matrix-charts)

**IMPORTANT:** When testing chart rendering in matrix-charts, you MUST use `SvgWriter.toXml()` to convert SVG objects to strings for assertions. **DO NOT use `svg.toString()`** - it returns the Java object representation (`"se.alipsa.groovy.svg.Svg@hashcode"`), not the SVG XML content.

#### Correct Pattern

```groovy
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import static se.alipsa.matrix.gg.GgPlot.*

@Test
void testChartRendering() {
  def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[1, 2], [2, 4], [3, 6]])
      .build()

  def chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_line()
  def svg = chart.render()

  // Correct - Use SvgWriter.toXml() to get SVG XML content
  String svgContent = SvgWriter.toXml(svg)
  assertTrue(svgContent.contains('<svg'))
  assertTrue(svgContent.contains('</svg>'))
  assertTrue(svgContent.contains('<line'))
}
```

#### Incorrect Pattern (DO NOT USE)

```groovy
@Test
void testChartRendering() {
  def chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_line()
  def svg = chart.render()

  // Wrong - svg.toString() returns "se.alipsa.groovy.svg.Svg@66933239"
  assertTrue(svg.toString().contains('<svg'))  // Will FAIL!
}
```

#### Why Not Navigate the SVG Object Model?

While it's theoretically possible to navigate the SVG object model directly (accessing children, elements, attributes), **string-based assertions are preferred** for these reasons:

1. **Simplicity**: String assertions are straightforward and easy to read
2. **Completeness**: Testing the XML ensures the entire rendering pipeline works end-to-end
3. **Regression detection**: String content catches formatting and structure changes
4. **Real-world validation**: Tests verify what would actually be output to users
5. **Library independence**: Tests don't depend on gsvg's internal object structure

However, if you need to test specific SVG elements or attributes programmatically, you can use `SvgReader.fromXml()` to parse the XML and navigate the resulting object model.

#### Required Imports for Chart Tests

```groovy
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter  // <- Always include for chart tests
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*
```

## Module Structure

| Module                 | Purpose                                                                   |
|------------------------|---------------------------------------------------------------------------|
| **matrix-core**        | Core Matrix/Grid classes, basic statistics, data conversion               |
| **matrix-stats**       | Statistical tests (t-test, correlation, regression) using Smile ML        |
| **matrix-datasets**    | Common datasets (mtcars, iris, diamonds, etc.)                            |
| **matrix-charts**      | Charm rendering engine and chart-type-first API with SVG output           |
| **matrix-ggplot**    | GGPlot2-style charting API, delegates to Charm in matrix-charts           |
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

### matrix-charts (Charm Rendering Engine)
The charting module provides the Charm rendering engine and a chart-type-first API:

```
Charts.groovy     -> Charm DSL entry point (plot, aes, geoms, scales, themes)
CharmRenderer     -> Core rendering pipeline
chartexport/      -> Export to PNG, JPEG, Swing, JavaFX, BufferedImage
charts/           -> Chart-type-first API (AreaChart, BarChart, PieChart, etc.)
```

### matrix-ggplot (GGPlot2-style API)
Provides a ggplot2-compatible API that delegates to Charm in matrix-charts:

```
GgPlot.groovy     -> Static factory methods (ggplot, aes, geom_*, scale_*, theme_*)
GgChart.groovy    -> Chart specification container with plus() operators
gg/bridge/        -> Bridge converting gg specs to Charm model
gg/export/        -> GgExport convenience wrapper for chart export
```

**Data flow** (spans both modules): Data + Aes -> GgChart (matrix-ggplot) -> GgCharmCompiler bridge -> Charm model (matrix-charts) -> Stat transformation -> Position adjustment -> Scale computation -> Coord transformation -> Geom rendering -> Theme styling -> SVG output

**Key patterns**:
- Deferred rendering: collect specifications, render on `chart.render()`
- `@CompileStatic` annotations for performance
- Scale auto-detection: numeric data -> continuous scales, string data -> discrete scales

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
  [midpoint, distance]
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
  value * curvature * 0.5
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

**Prefer operators over Math methods:**
```groovy
// Good - Idiomatic Groovy with operators
BigDecimal power = x ** exponent
BigDecimal squared = value ** 2
BigDecimal sqrt = value ** 0.5
BigDecimal reciprocal = 1 / value

// Avoid - Java Math methods with unnecessary type conversions
BigDecimal power = Math.pow(x as double, exponent as double) as BigDecimal
BigDecimal squared = Math.pow(value as double, 2.0) as BigDecimal
BigDecimal sqrt = Math.sqrt(value as double) as BigDecimal
BigDecimal reciprocal = (1.0 / (value as double)) as BigDecimal
```

**Always prefer BigDecimal extension methods over Math methods:**
```groovy
// Good - Use extension methods from matrix-groovy-ext
BigDecimal naturalLog = value.log()
BigDecimal log10 = value.log10()
BigDecimal exp = value.exp()
BigDecimal squareRoot = value.sqrt()
BigDecimal radians = degrees.toRadians()
BigDecimal degrees = radians.toDegrees()
BigDecimal sine = angle.sin()
BigDecimal cosine = angle.cos()

// Avoid - Verbose Math methods with type conversions
BigDecimal naturalLog = Math.log(value as double) as BigDecimal
BigDecimal radians = Math.toRadians(degrees as double) as BigDecimal
BigDecimal sine = Math.sin(angle as double) as BigDecimal
```

**If you need a Math operation not yet in NumberExtension:**
- First, add it to `matrix-groovy-ext/src/main/groovy/se/alipsa/matrix/ext/NumberExtension.groovy`
- Add corresponding tests to `NumberExtensionTest.groovy`
- Update AGENTS.md to document the new extension
- Then use it in your code

This keeps the codebase consistent and improves readability for future code.

### NumberExtension Methods

The `matrix-groovy-ext` module provides extension methods for Number types:

```groovy
// Mathematical constants (already BigDecimal, use these instead of Math.PI/Math.E)
import static se.alipsa.matrix.ext.NumberExtension.PI
import static se.alipsa.matrix.ext.NumberExtension.E

BigDecimal circumference = 2 * PI * radius
BigDecimal naturalLog = E.log()  // -> 1.0

// Floor and ceiling (returns BigDecimal)
BigDecimal x = 3.7G
x.floor()  // -> 3.0
x.ceil()   // -> 4.0

// Natural logarithm (ln)
E.log()  // -> 1.0
BigDecimal value = 10.0
value.log()  // -> 2.302585...

// Logarithm base 10
BigDecimal value2 = 100.0
value2.log10()  // -> 2.0

// Exponential function (e^x)
BigDecimal x = 1.0
x.exp()  // -> 2.718281828... (E)

// Square root with default precision
BigDecimal area = 25.0G
area.sqrt()  // -> 5.0 (uses MathContext.DECIMAL64)

// Trigonometric functions (angles in radians)
import static se.alipsa.matrix.ext.NumberExtension.PI

BigDecimal angle = PI / 2
angle.sin()  // -> 1.0
angle.cos()  // -> 0.0

// Angle conversions
BigDecimal degrees = 180.0
degrees.toRadians()  // -> 3.14159... (PI)
BigDecimal radians = PI
radians.toDegrees()  // -> 180.0

// Tangent and arctangent functions
BigDecimal x = PI / 4
x.tan()  // -> 1.0 (tan of 45°)
x.atan()  // -> arctangent

// Two-argument arctangent (atan2) - angle from rectangular to polar coordinates
BigDecimal dy = 3.0
BigDecimal dx = 4.0
BigDecimal angle = dy.atan2(dx)  // -> angle in radians
// Instead of: Math.atan2(dy, dx)

// Inverse operations demonstrate composability
BigDecimal testValue = 5.0
testValue.log().exp()  // -> 5.0 (log and exp are inverses)

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
- All extension methods accept `Number` parameter, enabling seamless use with any numeric type (Integer, Long, Double, BigDecimal, etc.)
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

**Don't create unnecessary constants:**
```groovy
// Avoid
static final BigDecimal TWO = 2.0G
result = value.divide(TWO, MATH_CONTEXT)

// Prefer
result = value / 2
```

**Don't use `.doubleValue()` (code smell):**
```groovy
// Avoid
double x = (value as Number).doubleValue()

// Prefer
BigDecimal bd = value as BigDecimal
// or if double is needed
double x = value as double
```

**Don't force BigDecimal where double is clearer:**
```groovy
// where double is required
double px = cx + radius * Math.sin(angle)  // Clear
double px = cx + (radius * (angle as BigDecimal).sin()) as double  // Verbose
// However, prefer this
BigDecimal px = cx + radius * angle.sin()  // Clear and idiomatic
```

**Don't use switch statements for simple value mappings:**
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

**Don't use unnecessary type casts in comparisons:**
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

**Don't cast Number properties unnecessarily:**
```groovy
// Avoid - unnecessary casts
BigDecimal result = i * dotDiameter * (stackratio as BigDecimal)
BigDecimal width = hexWidth * (dotsize as BigDecimal) * 0.9

// Prefer - let Groovy's type coercion work
BigDecimal result = i * dotDiameter * stackratio
BigDecimal width = hexWidth * dotsize * 0.9
```

**Don't calculate unused variables:**
```groovy
// Avoid - yRange is calculated but never used
BigDecimal xRange = xMax - xMin
BigDecimal yRange = yMax - yMin  // Not used anywhere
BigDecimal hexWidth = xRange / bins

// Prefer - only calculate what you need
BigDecimal xRange = xMax - xMin
BigDecimal hexWidth = xRange / bins
```

**Don't use Math.max/Math.min for array index clamping:**
```groovy
// Avoid - verbose with Math methods
int rawIdx = (ratio * (fillColors.size() - 1)) as int
int colorIdx = Math.max(0, Math.min(rawIdx, fillColors.size() - 1))

// Prefer - idiomatic BigDecimal chaining
BigDecimal rawIdx = ratio * (fillColors.size() - 1)
BigDecimal colorIdx = 0.max(rawIdx.min(fillColors.size() - 1))
```

**Declare types at definition to avoid repeated casts:**
```groovy
// Good - type declared once at definition, no casts needed later
Number x1Px = xScale.transform(seg.x) as Number
Number y1Px = yScale.transform(seg.y) as Number
line.x1(x1Px).y1(y1Px)  // Clean, no casts needed

// Avoid - repeated casts throughout the code
def x1Px = xScale.transform(seg.x)
def y1Px = yScale.transform(seg.y)
line.x1(x1Px as Number).y1(y1Px as Number)  // Repetitive
```

**Use explicit types when needed for type checker:**
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

**Use .length for arrays, .size() for collections:**
```groovy
// Good - arrays use .length
double[][] points = createPoints()
for (int i = 1; i < points.length; i++) { }

// Good - collections use .size()
List<String> items = []
for (int i = 0; i < items.size(); i++) { }
```

**Prefer Number type in method signatures for flexibility:**
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

**Warning: @CompileStatic type checking with `**` operator:**
When using `@CompileStatic`, the `**` operator may return `Number` instead of `BigDecimal`, causing type checking errors with typed collections. Use explicit variable assignment:
```groovy
// CompileStatic error - type inference fails
List<BigDecimal> result = []
for (int i = minExp; i <= maxExp; i++) {
  result << (10 ** i)  // Error: Cannot add Number to List<BigDecimal>
}

// Solution - assign to typed variable first
List<BigDecimal> result = []
for (int i = minExp; i <= maxExp; i++) {
  BigDecimal value = (10 ** i) as BigDecimal
  result << value
}
```
### Use of the return statement
Use the `return` keyword only when we need to return early, otherwise use implicit return.

| Feature    | Idiomatic Groovy (good)   | Java-Style Groovy (bad) |
|------------|---------------------------|-------------------------|
| Last Line  | Implicit (No return)      | Explicit return         |
| Early Exit | Explicit return           | Explicit return         |
| Closures   | Implicit                  | Implicit                |
| Vibe       | Clean, functional, modern | Verbose, traditional    |

### Modern Switch Expressions (JDK 14+ / Groovy 5+)

**IMPORTANT:** With Groovy 5.0.3 and JDK 21, always use modern switch expression syntax with arrow (`->`) instead of old-style colon (`:`) with `break` statements.

**Use modern switch expressions:**
```groovy
// Good - Modern arrow syntax (JDK 14+)
switch (shape?.toLowerCase()) {
  case 'square' -> {
    group.addRect(size, size)
        .x(x).y(y)
        .fill(color)
  }
  case 'plus', 'cross' -> {  // Multiple cases combined
    group.addLine(x1, y1, x2, y2).stroke(color)
    group.addLine(x3, y3, x4, y4).stroke(color)
  }
  case 'circle' -> {
    group.addCircle()
        .cx(cx).cy(cy)
        .r(radius)
  }
  default -> {
    // Default case
    group.addCircle().cx(cx).cy(cy).r(5)
  }
}
```

**Avoid old-style switch with break:**
```groovy
// Avoid - Old colon syntax with break statements
switch (shape?.toLowerCase()) {
  case 'square':
    group.addRect(size, size)
        .x(x).y(y)
        .fill(color)
    break
  case 'plus':
  case 'cross':
    group.addLine(x1, y1, x2, y2).stroke(color)
    group.addLine(x3, y3, x4, y4).stroke(color)
    break
  case 'circle':
  default:
    group.addCircle().cx(cx).cy(cy).r(5)
    break
}
```

**Benefits of modern switch expressions:**
- No `break` statements needed (eliminates fallthrough bugs)
- Multiple cases can be combined on one line with commas
- Clearer intent with arrow syntax
- More concise and readable
- Compiler enforces exhaustiveness

**When refactoring existing code:**
When you encounter old-style switch statements during code modifications:
1. **Always modernize them** to use arrow syntax
2. Combine multiple cases using comma separation (`case 'a', 'b' ->`)
3. Remove all `break` statements
4. Ensure each case block is wrapped in `{ }` for multi-statement blocks

This applies to any switch statement you touch, even if the primary task is something else. Keeping the codebase modern and consistent is a continuous improvement goal.

### When in Doubt

Ask yourself:
1. **Does this make the code easier to read?** -> Do it
2. **Does this make the code harder to read?** -> Don't do it
3. **Is it about the same?** -> Leave it alone

The goal is beautiful Groovy code, not BigDecimal everywhere.

## Testing SVG Visualizations with Direct Object Access

**Performance Best Practice:** When testing SVG chart rendering, use direct object access instead of serialization for assertions. This is significantly faster and more reliable.

### Pattern

```groovy
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Text

@Test
void testChartRendering() {
    def chart = ggplot(data, aes(x: 'col1', y: 'col2')) + geom_point()
    Svg svg = chart.render()
    assertNotNull(svg)

    // ✅ GOOD: Direct object access (1.3x faster, no serialization)
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, "Should render points")

    // ❌ BAD: Serialization-based (slower, unnecessary I/O)
    // String svgContent = SvgWriter.toXml(svg)
    // assertTrue(svgContent.contains('<circle'))
}
```

### Available Methods

**Tree Navigation:**
- `svg.descendants()` - Get all nested elements recursively (most common)
- `svg.getChildren()` - Get only direct children
- `element.parent` - Navigate up the tree

**Element Filtering:**
```groovy
def descendants = svg.descendants()

// Filter by type
def circles = descendants.findAll { it instanceof Circle }
def rects = descendants.findAll { it instanceof Rect }
def lines = descendants.findAll { it instanceof Line }
def paths = descendants.findAll { it instanceof Path }
def textElements = descendants.findAll { it instanceof Text }

// Multiple types
assertTrue(circles.size() > 0 || paths.size() > 0, "Should contain elements")
```

**Text Content:**
```groovy
def textElements = svg.descendants().findAll { it instanceof Text }
def allText = textElements.collect { it.content }.join(' ')
assertTrue(allText.contains('Chart Title'))
```

**SVG Properties:**
```groovy
// Direct access to SVG attributes
int width = svg.width as int
int height = svg.height as int
assertTrue(width >= 800)
assertEquals(600, height)
```

### When to Keep File Writes

Keep file I/O for **visual regression testing** (5-10% of tests):
- One test per major geom type
- Complex multi-element charts
- Coordinate system examples

```groovy
@Test
void testComplexVisualization() {
    Svg svg = chart.render()

    // Use direct access for assertions
    def paths = svg.descendants().findAll { it instanceof Path }
    assertTrue(paths.size() > 0)

    // Keep file write for manual inspection
    File outputFile = new File('build/visual_regression_test.svg')
    write(svg, outputFile)
}
```

### Performance Impact

Direct object access vs serialization:
- **Speed**: 1.3x faster
- **Memory**: No string allocation for large SVGs
- **Reliability**: Type-safe, no string parsing

**Benchmark Results:**
```
Direct access:     3ms per test
Serialization:     4ms per test
File I/O:         15ms+ per test
```

For a test suite with 200+ tests, this optimization saves ~30 seconds per run.

## Key Dependencies

- **Groovy**: 5.0.3 (groovy, groovy-sql, groovy-ginq)
- **Testing**: JUnit Jupiter 6.0.1
- **Charting**: gsvg 0.4.0, JFreeChart 1.5.6, JavaFX 23.0.2
- **Statistics**: Smile 4.4.2, commons-math3
- **Data formats**: Jackson, Apache POI, commons-csv
