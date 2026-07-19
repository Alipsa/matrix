# Groovy Style Guide

Detailed coding-style rules for this repository, companion to [AGENTS.md](../../AGENTS.md).
The core rules are summarized in AGENTS.md; this guide carries the full rationale and examples.

## Prefer Idiomatic Groovy Constructs

Bear in mind that Groovy is not Java, and while they interoperate seamlessly, Groovy has its own idioms and best practices that **differ** from Java. Write code that is idiomatic Groovy rather than Java code written in Groovy syntax. Some examples:
 - Use Groovy's native collection literals and methods e.g. closures, `each`, `collect`, and `findAll` instead of Java-style loops and streams. Use `[]` instead of `new ArrayList<>()` and `[:]` instead of `new LinkedHashMap<>()`/`new HashMap<>()`.
 - Prefer closures and higher-order functions over verbose anonymous classes.
 - == vs .equals(): Use `==` for value equality in Groovy, which handles nulls gracefully. == does NOT mean reference equality like in Java.
 - String interpolation: Use `"${var}"` for building strings instead of concatenation.
 - Default numeric type: Groovy uses BigDecimal as the default for decimal literals. Strongly prefer BigDecimal over double/float unless performance is critical.

## Avoid `Object` Parameters — Use Typed Overloads

**CRITICAL:** Never use `Object` as a method parameter or field type when specific types are known. `Object` parameters are not type-safe and provide no IDE assistance. Instead, use method overloads with concrete types:

```groovy
// Good - typed overloads, clear API
void setLegendPosition(LegendPosition value) { ... }
void setLegendPosition(List<Number> value) { ... }

PlotSpec legendPosition(LegendPosition value) { ... }
PlotSpec legendPosition(List<Number> value) { ... }

PointBuilder shape(ShapeName value) { ... }
LineBuilder linetype(LinetypeName value) { ... }

// Bad - Object provides no type safety or IDE help
void setLegendPosition(Object value) { ... }
PointBuilder shape(Object value) { ... }
```

**Exception — Object fallback dispatch methods:** An `Object` parameter is acceptable in a **fallback method** that dispatches via `instanceof` to existing typed overloads. This allows dynamically-typed callers (e.g. `def chart = createChart()`) to use the API without compile-time type knowledge. The typed overloads must always exist alongside the fallback — the `Object` method is never the only entry point.

```groovy
// Good - Object fallback alongside typed overloads
static SvgPanel export(CharmChart chart) { ... }
static SvgPanel export(Svg chart) { ... }
static SvgPanel export(PlotGrid grid) { ... }

@CompileDynamic
static SvgPanel export(Object chart) {
  if (chart instanceof CharmChart) export((CharmChart) chart)
  else if (chart instanceof Svg) export((Svg) chart)
  else if (chart instanceof PlotGrid) export((PlotGrid) chart)
  else throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
}
```

When a value can legitimately be one of several types (e.g. an enum or a list), provide a separate overload for each type. Use enums instead of strings for fixed sets of values (e.g. positions, directions, shape names, line types) and do any string-to-enum conversion at API boundaries (bridges, adapters).

## Flow Typing after `instanceof`

Under `@CompileStatic` (enabled globally in this project), Groovy narrows the type of a local variable after an `instanceof` check — explicit casts are redundant in those cases:

```groovy
// Good — flow typing narrows value to Number
if (value instanceof Number) return value.intValue()

// Bad — redundant cast
if (value instanceof Number) return ((Number) value).intValue()
if (value instanceof Number) return (value as Number).intValue()
```

**When flow typing does NOT apply** (cast is required):

- **Property access chains** — the compiler doesn't narrow a property type on re-access, even if the `instanceof` guard covers the same expression:
  ```groovy
  // Cast required — datum.meta.__row is re-evaluated, not narrowed
  if (datum.meta?.__row instanceof Map) {
    Map<String, Object> row = datum.meta.__row as Map<String, Object>
  }
  ```

- **Wildcard or raw generics** — narrowing `first` doesn't change the container's generic type:
  ```groovy
  List<?> list = ...
  Object first = list.first()
  // Cast required — list is still List<?>, not List<Map>
  if (first instanceof Map) {
    return Matrix.builder().mapList(list as List<Map>).build()
  }
  ```

- **Ternary expressions** — flow typing doesn't apply inside ternary branches:
  ```groovy
  // Cast required
  datum.meta.__data instanceof Matrix ? datum.meta.__data as Matrix : null
  ```

- **Mutable class fields** — the compiler can't guarantee a field hasn't been reassigned by another thread between the check and the usage:
  ```groovy
  class Renderer {
    Object data
    void render() {
      // Cast required — data is a mutable field, not a local variable
      if (data instanceof Matrix) {
        int rows = (data as Matrix).rowCount()
      }
    }
  }
  ```

- **Closures and anonymous inner classes** — a local variable checked with `instanceof` loses its narrowed type inside a closure or anonymous class, because the variable could be reassigned before the closure executes:
  ```groovy
  if (value instanceof Number) {
    // Cast required — flow typing doesn't carry into the closure
    list.collect { (value as Number).intValue() }
  }
  ```

- **Logical OR (`||`) chains** — flow typing doesn't narrow across `||` because either branch could be true, so the right-hand side can't assume the `instanceof` passed. (`&&` chains work fine since both sides must be true):
  ```groovy
  // COMPILER ERROR — v is still Object when evaluating toUpperCase()
  if (v instanceof String || v.toUpperCase() == 'DEFAULT') { ... }

  // Fix — split into separate checks
  if (v instanceof String) {
    if (v.toUpperCase() == 'DEFAULT') { ... }
  }

  // OK — && works because instanceof must be true for the right side to evaluate
  if (v instanceof String && v.toUpperCase() == 'DEFAULT') { ... }
  ```

- **Overloaded methods with supertypes** — when multiple overloads match the narrowed type (e.g. `String` is also a `CharSequence`), the compiler may report an ambiguous call or fall back to `Object`:
  ```groovy
  void log(CharSequence cs) { ... }
  void log(String s) { ... }

  void handle(Object obj) {
    if (obj instanceof String) {
      // COMPILER ERROR — ambiguous between log(CharSequence) and log(String)
      log(obj)
      // Fix — explicit cast to pick the intended overload
      log((String) obj)
    }
  }
  ```

## Numeric Types

- Use Number in method parameters and use BigDecimal when returning numeric values
- In the case that there is proven performance issue with this, create a small java utility that does the calculation in double and returns it as BigDecimal. The groovy code should be free of non-idiomatic groovy and use of double is a strong code smell.
- Prefer `List<Number>` or `List<BigDecimal>` over `double[]`/`float[]` in public Groovy-facing APIs. If generic type erasure is a problem, use Number[] or BigDecimal[] instead.
- `double[]` and `double[][]` are acceptable for internal or low-level numeric computations where performance matters, for example interpolation and linear algebra but should be handled by java classes rather than groovy. In those cases, an idiomatic groovy alternative should exist alongside the java style construct.
- Prefer `Grid<BigDecimal>` or `Grid<Number>` over `double[][]` in public Groovy-facing APIs.

Examples
```groovy
double evaluate(double u) // BAD
BigDecimal evaluate(Number u) // GOOD

// DON'T DO THIS
List<double[]> categoryData = []
data.each { String key, List<? extends Number> values ->
   categoryData.add(ListConverter.toDoubleArray(values))
}

// DO THIS
Grid<Number> categoryData = new Grid<>()
data.each { String key, List<? extends Number> values ->
   categoryData << values
}
```

## Groovy's Natural Numeric Type: BigDecimal

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

## BigDecimal vs double

**Primary Goal:** Write beautiful, easy-to-read, idiomatic Groovy code. Readability > precision optimization.

**When to use Number:**
- as parameters in the end user API (numeric return type should primarily be BigDecimal though)

**When to use BigDecimal:**
- Business logic and data transformations
- Scale operations (ranges, domains, transforms)
- When numeric literals naturally appear in code (`1.0G`, `2.5G`)

**When to use double:**
- For performance critical code pieces e.g. in loops with complex calculations

## Numeric Operations

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

**Scientific notation literals are also BigDecimal** — `1e-10`, `2.5e3`, `1E10` are all `BigDecimal` in Groovy, not `Double`. A `d`/`D` suffix is required to get a `Double` from a decimal or scientific-notation literal:
```groovy
def a = 1e-10    // BigDecimal
def b = 1e-10d   // Double
def c = 2.5e3    // BigDecimal
def d = 2.5e3d   // Double
```
Do NOT flag `1e-10` as a double literal or suggest replacing it with `new BigDecimal("1e-10")` — it is already a BigDecimal.


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
- Update this guide ([NumberExtension Methods](#numberextension-methods)) to document the new extension
- Then use it in your code

This keeps the codebase consistent and improves readability for future code.

## NumberExtension Methods

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

// Natural logarithm of (1 + x)
BigDecimal small = 0.001
small.log1p()  // -> 0.0009995...
(0.0).log1p()  // -> 0.0

// Exponential function (e^x)
BigDecimal x = 1.0
x.exp()  // -> 2.718281828... (E)

// Square root with default precision
BigDecimal area = 25.0G
area.sqrt()  // -> 5.0 (uses MathContext.DECIMAL64)

// Cube root with default precision
BigDecimal volume = 27.0G
volume.cbrt()  // -> 3.0

// Hypotenuse - sqrt(x² + y²) without overflow/underflow
BigDecimal dx = 3.0
BigDecimal dy = 4.0
BigDecimal distance = dx.hypot(dy)  // -> 5.0
// Instead of: Math.hypot(dx, dy)

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

// Arcsine and arccosine (inverse trigonometric functions)
BigDecimal half = 0.5G
half.asin()  // -> 0.52359... (π/6)
half.acos()  // -> 1.04719... (π/3)

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

## Anti-Patterns to Avoid

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

## Use of the return Statement

Use the `return` keyword only when we need to return early, otherwise use implicit return.

| Feature    | Idiomatic Groovy (good)   | Java-Style Groovy (bad) |
|------------|---------------------------|-------------------------|
| Last Line  | Implicit (No return)      | Explicit return         |
| Early Exit | Explicit return           | Explicit return         |
| Closures   | Implicit                  | Implicit                |
| Vibe       | Clean, functional, modern | Verbose, traditional    |

## Modern Switch Expressions (JDK 14+ / Groovy 5+)

**IMPORTANT:** With Groovy 5.0.6 and JDK 21, always use modern switch expression syntax with arrow (`->`) instead of old-style colon (`:`) with `break` statements.

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

**Exception — `return` inside `@CompileStatic` switch arms:**
Groovy 5.0.x treats arrow switch as an *expression*, which does not support `return` to exit the enclosing method.
When a case arm needs to `return` from the method (common in type-dispatch methods like `toAvroValue` or `isCompatible`),
keep the old colon-style switch. Do **not** attempt to convert these to arrow syntax — the compiler either rejects it or generates invalid bytecode.

```groovy
// Old-style is correct here — return exits the method, not the switch
switch (schema.getType()) {
  case Schema.Type.STRING: return v.toString()
  case Schema.Type.INT:
    if (v instanceof Number) return v.intValue()
    break
  case Schema.Type.ARRAY:
    // ... loop and return ...
    return out
}
```

**When refactoring existing code:**
When you encounter old-style switch statements during code modifications:
1. **Modernize them** to use arrow syntax unless case arms must use `return` to exit the method (see exception above)
2. Combine multiple cases using comma separation (`case 'a', 'b' ->`)
3. Remove all `break` statements
4. Ensure each case block is wrapped in `{ }` for multi-statement blocks (but don't use them for single statements)

This applies to any switch statement you touch, even if the primary task is something else. Keeping the codebase modern and consistent is a continuous improvement goal.

## When in Doubt

Ask yourself:
1. **Does this make the code easier to read?** -> Do it
2. **Does this make the code harder to read?** -> Don't do it
3. **Is it about the same?** -> Leave it alone

The goal is beautiful Groovy code, not BigDecimal everywhere.
