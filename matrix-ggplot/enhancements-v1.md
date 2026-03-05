# GgPlot Enhancements v1 — IDE Support & Usability

Phased plan for improving IDE auto-complete, reducing syntactic friction, and adding convenience APIs to `matrix-ggplot` while maintaining R ggplot2 API compatibility.

---

## Phase 1 — Closure-based `aes` with unquoted column names

The biggest friction point vs R is quoting column names. R uses non-standard evaluation (NSE) so `aes(x = mpg)` captures `mpg` as a symbol. Groovy can achieve the same via a `propertyMissing`-based closure delegate.

**Current:**
```groovy
ggplot(mtcars, aes(x: 'mpg', y: 'wt', color: 'cyl')) + geom_point()
```

**New (additive — existing syntax unchanged):**
```groovy
ggplot(mtcars, aes { x = mpg; y = wt; color = cyl }) + geom_point()
```

### 1.1 [ ] Create `AesDsl` closure delegate

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/aes/AesDsl.groovy`

```groovy
@CompileDynamic
class AesDsl {
  // Known aesthetics — explicit fields for IDE autocomplete
  Object x, y, color, fill, size, shape, alpha, linetype, linewidth
  Object group, label, tooltip, weight, geometry, map_id

  // Alias for British spelling
  void setColour(Object value) { color = value }

  // Any unknown property becomes its own name as a String.
  // So `mpg` resolves to "mpg", `wt` to "wt", etc.
  Object propertyMissing(String name) { name }

  /** Collects non-null fields into an Aes instance. */
  Aes toAes() {
    Aes aes = new Aes()
    if (x != null) aes.x = x
    if (y != null) aes.y = y
    if (color != null) aes.color = color
    if (fill != null) aes.fill = fill
    if (size != null) aes.size = size
    if (shape != null) aes.shape = shape
    if (alpha != null) aes.alpha = alpha
    if (linetype != null) aes.linetype = linetype
    if (linewidth != null) aes.linewidth = linewidth
    if (group != null) aes.group = group
    if (label != null) aes.label = label
    if (tooltip != null) aes.tooltip = tooltip
    if (weight != null) aes.weight = weight
    if (geometry != null) aes.geometry = geometry
    if (map_id != null) aes.map_id = map_id
    aes
  }
}
```

**Design notes:**
- `@CompileDynamic` is required for `propertyMissing` — this is only the DSL delegate, not the hot path.
- `DELEGATE_ONLY` resolution strategy ensures user-scope variables don't leak in, matching R's NSE semantics.
- Column names with spaces/special chars still need quoting: `x = 'Sepal Length'`.
- Special wrappers (`I()`, `factor()`, `expr {}`, `after_stat()`, `after_scale()`, `cut_width()`) work naturally inside the closure since they are resolved via static imports, not property lookup.

### 1.2 [ ] Add `aes(Closure)` overload to `GgPlot`

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

```groovy
/**
 * Create aesthetic mappings using a closure with unquoted column names.
 *
 * <p>Inside the closure, bare identifiers resolve to column name strings
 * via {@code propertyMissing}, matching R's non-standard evaluation:
 * <pre>{@code
 * aes { x = mpg; y = wt; color = cyl }
 * // equivalent to: aes(x: 'mpg', y: 'wt', color: 'cyl')
 * }</pre>
 *
 * <p>Special wrappers work naturally:
 * <pre>{@code
 * aes { x = displ; y = hwy; color = I('red') }
 * aes { x = factor(cyl); y = mpg }
 * }</pre>
 *
 * @param configure closure delegating to {@link AesDsl}
 * @return a new Aes instance
 */
static Aes aes(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = AesDsl) Closure configure) {
  AesDsl dsl = new AesDsl()
  Closure body = configure.rehydrate(dsl, configure.owner, configure.thisObject)
  body.resolveStrategy = Closure.DELEGATE_ONLY
  body.call()
  dsl.toAes()
}
```

### 1.3 [ ] Add tests for closure-based `aes`

**File:** `matrix-ggplot/src/test/groovy/gg/AesDslTest.groovy`

Test cases:
- Basic unquoted column names: `aes { x = mpg; y = wt }` produces `Aes(x: 'mpg', y: 'wt')`
- Quoted column names with spaces: `aes { x = 'Sepal Length'; y = 'Petal Width' }`
- Mixed quoted and unquoted: `aes { x = displ; y = 'Highway MPG' }`
- Colour alias: `aes { colour = species }` sets `color`
- Identity wrapper: `aes { x = mpg; color = I('red') }` — `color` is `Identity('red')`
- Factor wrapper: `aes { x = factor(cyl); y = mpg }`
- All aesthetics: verify each known field (fill, size, shape, alpha, linetype, linewidth, group, label, tooltip, weight, geometry, map_id)
- Full render test: `ggplot(data, aes { x = x; y = y }) + geom_point()` renders SVG with circles
- Layer-level aes: `geom_point(mapping: aes { color = species })` works correctly

### 1.4 [ ] Update GroovyDoc on existing `aes()` overloads

Add a cross-reference to the closure variant in the GroovyDoc of the Map-based `aes(Map)` and positional `aes(Object, Object)` overloads, so users discover the new syntax via IDE hover.

### Verification

```bash
./gradlew :matrix-ggplot:test -Pheadless=true
```

---

## Phase 2 — `qplot()` quick plot

R's `qplot()` is a convenience function for rapid exploratory analysis. It infers the geom from data types and accepts column names directly.

**R:**
```r
qplot(mpg, wt, data = mtcars, color = cyl)
qplot(factor(cyl), data = mtcars, geom = "bar")
```

**Groovy (map-based):**
```groovy
qplot(data: mtcars, x: 'mpg', y: 'wt', color: 'cyl')
qplot(data: mtcars, x: 'cyl', geom: 'bar')
qplot(data: mtcars, x: 'mpg', geom: 'histogram', bins: 15)
```

**Groovy (closure-based, if Phase 1 is done):**
```groovy
qplot(mtcars) { x = mpg; y = wt; color = cyl }
```

### 2.1 [x] Implement `qplot()` in `GgPlot`

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

Add two overloads:

```groovy
/**
 * Quick plot — convenience function for rapid exploratory charting.
 *
 * <p>Infers geom from data and aesthetics:
 * <ul>
 *   <li>x + y numeric → {@code geom_point()}</li>
 *   <li>x discrete, no y → {@code geom_bar()}</li>
 *   <li>x numeric, no y → {@code geom_histogram()}</li>
 * </ul>
 *
 * <p>Override with {@code geom: 'line'}, {@code geom: 'boxplot'}, etc.
 *
 * @param params named parameters (data, x, y, color, fill, geom, title, bins, ...)
 * @return a renderable GgChart
 */
static GgChart qplot(Map params) { ... }

/**
 * Quick plot with closure-based aesthetics.
 *
 * <pre>{@code
 * qplot(mtcars) { x = mpg; y = wt; color = cyl }
 * }</pre>
 *
 * @param data the Matrix
 * @param configure closure delegating to AesDsl
 * @return a renderable GgChart
 */
static GgChart qplot(Matrix data, @DelegatesTo(AesDsl) Closure configure) { ... }
```

**Geom inference logic:**
1. If `geom` is explicitly specified → use that
2. If both x and y are provided and both columns are numeric → `geom_point()`
3. If only x is provided and column is numeric → `geom_histogram()`
4. If only x is provided and column is non-numeric → `geom_bar()`
5. If x is discrete and y is numeric → `geom_col()` (column chart)
6. Default → `geom_point()`

### 2.2 [x] Add tests for `qplot()`

**File:** `matrix-ggplot/src/test/groovy/gg/QplotTest.groovy`

Test cases:
- Scatter: `qplot(data: data, x: 'x', y: 'y')` renders circles
- Histogram: `qplot(data: data, x: 'value')` with numeric column renders histogram bars
- Bar: `qplot(data: data, x: 'category')` with string column renders bars
- Explicit geom override: `qplot(data: data, x: 'x', y: 'y', geom: 'line')` renders lines
- Color aesthetic: `qplot(data: data, x: 'x', y: 'y', color: 'group')` renders colored points
- Title: `qplot(data: data, x: 'x', y: 'y', title: 'My Plot')` sets chart title
- Closure-based: `qplot(data) { x = xCol; y = yCol }` renders circles (requires Phase 1)

### Verification

```bash
./gradlew :matrix-ggplot:test -Pheadless=true
```

---

## Phase 3 — Typed Aes fields

Currently all `Aes` fields are `def` (effectively `Object`), providing no IDE type hints. Since 90% of usage passes column name strings, typing the fields as `String` with overloaded setters for special wrappers improves the IDE experience significantly.

### 3.1 [ ] Add typed setter overloads to `Aes`

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/aes/Aes.groovy`

For each aesthetic field, keep the `def` field (needed for polymorphism) but add typed setter overloads so IDEs show helpful signatures:

```groovy
/** X position — column name. */
void setX(String columnName) { this.x = columnName }
/** X position — constant value. */
void setX(Identity constant) { this.x = constant }
/** X position — factor. */
void setX(Factor factor) { this.x = factor }
/** X position — computed expression. */
void setX(Expression expr) { this.x = expr }
/** X position — after_stat reference. */
void setX(AfterStat stat) { this.x = stat }
/** X position — after_scale reference. */
void setX(AfterScale scale) { this.x = scale }
/** X position — cut_width binning. */
void setX(CutWidth cut) { this.x = cut }
/** X position — closure expression. */
void setX(Closure<Number> expr) { this.x = new Expression(expr) }
```

Apply the same pattern to `y`, `color`, `fill`, `size`, `shape`, `alpha`, `linetype`, `linewidth`, `group`, `label`, `tooltip`, `weight`, `geometry`, `map_id`.

**Rationale:** IDEs will show `setX(String)` as the primary completion, making it obvious that a column name string is expected. The overloads for `Identity`, `Factor`, etc. appear as alternative signatures, documenting the full API surface without `Object`.

### 3.2 [ ] Update `Aes(Map)` constructor to use typed setters

Route the map constructor through the typed setters so the same validation/wrapping logic applies:

```groovy
Aes(Map params) {
  if (params.x != null) setX(params.x)
  if (params.y != null) setY(params.y)
  // ... etc
}
```

This requires a single untyped dispatch method that delegates to the correct overload based on runtime type. Or keep the current direct field assignment (which is simpler). Evaluate during implementation.

### 3.3 [ ] Add tests verifying typed setter behaviour

**File:** existing `AesDslTest.groovy` or `AesTest.groovy`

Test cases:
- `aes.x = 'mpg'` → `aes.x == 'mpg'` (String)
- `aes.x = I(42)` → `aes.isConstant('x')` is true
- `aes.x = factor(cyl)` → `aes.isFactor('x')` is true
- `aes.x = { it.mpg * 2 }` → `aes.isExpression('x')` is true
- Map constructor still works: `aes(x: 'mpg', color: I('red'))`

### Verification

```bash
./gradlew :matrix-ggplot:test -Pheadless=true
```

---

## Phase 4 — `cols()` column reference helper

A `cols()` helper generates a validation proxy from a Matrix. Property access returns column names as strings but validates that the column actually exists — catching typos at construction time.

```groovy
def c = cols(mtcars)
ggplot(mtcars, aes(x: c.mpg, y: c.wt))     // works
ggplot(mtcars, aes(x: c.mpgg, y: c.wt))    // throws: "Column 'mpgg' not found. Available: [mpg, cyl, disp, ...]"
```

### 4.1 [ ] Create `ColumnRef` proxy class

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/aes/ColumnRef.groovy`

```groovy
@CompileDynamic
class ColumnRef {
  private final Set<String> columns

  ColumnRef(Matrix data) {
    this.columns = data.columnNames().toSet()
  }

  /** Returns the column name if it exists, throws otherwise. */
  String propertyMissing(String name) {
    if (!columns.contains(name)) {
      throw new IllegalArgumentException(
          "Column '$name' not found in matrix. Available: ${columns.sort()}")
    }
    name
  }
}
```

### 4.2 [ ] Add `cols()` factory to `GgPlot`

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

```groovy
/**
 * Creates a column reference proxy for validated, unquoted column names.
 *
 * <pre>{@code
 * def c = cols(mtcars)
 * ggplot(mtcars, aes(x: c.mpg, y: c.wt))  // validated at call time
 * }</pre>
 *
 * @param data the Matrix whose column names to expose
 * @return a proxy that converts property access to validated column name strings
 */
static ColumnRef cols(Matrix data) { new ColumnRef(data) }
```

### 4.3 [ ] Add tests for `cols()`

**File:** `matrix-ggplot/src/test/groovy/gg/ColumnRefTest.groovy`

Test cases:
- Valid column: `cols(data).x` returns `'x'`
- Invalid column: `cols(data).nonexistent` throws `IllegalArgumentException` with helpful message
- Integration: `ggplot(data, aes(x: cols(data).x, y: cols(data).y)) + geom_point()` renders correctly
- Error message includes available column names

### Verification

```bash
./gradlew :matrix-ggplot:test -Pheadless=true
```

---

## Phase 5 — Documentation updates

Update all relevant documentation to cover the features added in Phases 1–4. No code changes — documentation only.

### 5.1 [ ] Update `matrix-ggplot/README.md`

Add sections or examples covering:
- Closure-based `aes` syntax with unquoted column names (Phase 1)
- `qplot()` quick plot with geom inference (Phase 2)
- `cols()` validation helper (Phase 4, if implemented)
- Show both map-based and closure-based styles side by side

### 5.2 [ ] Update `matrix-ggplot/docs/ggPlot.md`

This is the main ggplot API reference. Add:
- **Aesthetic mappings** section: document the closure-based `aes` syntax alongside the existing map-based syntax, including `propertyMissing` behavior, quoted names for columns with spaces, and compatibility with `I()`, `factor()`, `after_stat()`, etc.
- **Quick plot** section: document `qplot()` with all three overloads (`Map`, `Matrix + Closure`, `Map + Matrix + Closure`), geom inference rules, supported parameters, and examples
- **Typed Aes fields** section (Phase 3, if implemented): document the typed setter overloads and IDE benefits
- **Column references** section (Phase 4, if implemented): document `cols()` with validation behavior and error messages

### 5.3 [ ] Update `docs/R-comparison.md`

Update the R vs Matrix comparison table and examples to show:
- Closure-based `aes` — demonstrate how close it is to R's NSE (`aes(x = mpg)` vs `aes { x = mpg }`)
- `qplot()` — show R's `qplot()` alongside Matrix's `qplot()` to highlight API parity
- Update the feature comparison table if it lists limitations that are now resolved

### 5.4 [ ] Update `docs/tutorial/13-matrix-charts.md`

Add a note or brief subsection in the charts tutorial pointing to:
- The closure-based `aes` as the preferred syntax for new code
- `qplot()` for quick exploratory plots
- Link to the full ggPlot docs for details

### 5.5 [ ] Update `docs/cookbook/matrix-charts.md`

Add cookbook-style recipes demonstrating:
- Quick scatter plot with `qplot()`
- Quick histogram with `qplot()`
- Closure-based `aes` in a typical ggplot pipeline

### Verification

Documentation-only phase — no test run required.

---

## Summary

| Phase | Scope | Effort | Impact | R compat |
|-------|-------|--------|--------|----------|
| 1 | Closure-based `aes` (unquoted columns) | Medium | High — removes most string quoting | Closest to R's NSE |
| 2 | `qplot()` quick plot | Low | Medium — great for exploration | Direct R equivalent |
| 3 | Typed `Aes` fields | Medium | Medium — better IDE hints | No R impact |
| 4 | `cols()` validation helper | Low | Low-Medium — catches typos early | No R equivalent |
| 5 | Documentation updates | Low | High — makes features discoverable | Improves R comparison |
