# Pict Package Improvements — IDE Support & Usability

Phased plan for improving IDE auto-complete, type safety, and usability of `se.alipsa.matrix.pict`.

---

## Phase 1 — Type safety foundations

Low-risk changes that don't alter public API semantics. Each change is independently testable.

### 1a. Add `@CompileStatic` to all chart classes and support classes

Add `@CompileStatic` to:

| File | Class |
|------|-------|
| `Chart.groovy` | `Chart`, `ChartBuilder` |
| `AreaChart.groovy` | `AreaChart`, `Builder` |
| `BarChart.groovy` | `BarChart`, `Builder` |
| `BoxChart.groovy` | `BoxChart`, `Builder` |
| `Histogram.groovy` | `Histogram`, `Builder`, `MinMax` |
| `LineChart.groovy` | `LineChart`, `Builder` |
| `PieChart.groovy` | `PieChart`, `Builder` |
| `ScatterChart.groovy` | `ScatterChart`, `Builder` |
| `Style.groovy` | `Style` |
| `AxisScale.groovy` | `AxisScale` |
| `Legend.groovy` | `Legend` |
| `DataType.groovy` | `DataType` |

This will surface any dynamic dispatch issues that need explicit typing.

### 1b. Replace `def` with explicit types in all builder `build()` methods

Every builder's `build()` method uses `def chart = new XyzChart()`. Replace with the concrete chart type so IDEs can infer types downstream.

Example (AreaChart.Builder):
```groovy
// Before
def chart = new AreaChart()
// After
AreaChart chart = new AreaChart()
```

Apply the same pattern to local variables in factory `create()` methods and `Histogram.createRanges()`.

### 1c. Type the untyped collections in `Chart`

```groovy
// Before
protected List categorySeries
protected List<List> valueSeries

// After
protected List<?> categorySeries
protected List<List<? extends Number>> valueSeries
```

Update getters (`getCategorySeries()`, `getValueSeries()`, `getValueSerie()`) to match.

### 1d. Type `Style.yLabels`

```groovy
// Before
Map yLabels = [:]
// After
Map<String, String> yLabels = [:]
```

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

All existing chart tests (ChartBuilderTest, ChartFactoryBaselineTest, ChartsCharmIntegrationTest, BoxChartTest, LineChartTest) must pass.

---

## Phase 2 — Style field naming and typed Position setter

### 2a. Fix `Style` field capitalization

Rename `XAxisVisible` → `xAxisVisible` and `YAxisVisible` → `yAxisVisible`. Since backwards compatibility is not a concern, a direct rename is sufficient.

**File:** `Style.groovy`

### 2b. Add typed `Position` setter to `Style`

Currently only a `setLegendPosition(String)` setter exists. Add a typed overload:

```groovy
void setLegendPosition(Position pos) {
  legendPosition = pos
}
```

This lets users write `chart.style.legendPosition = Position.TOP` directly.

### 2c. Map `Style.legendPosition` to Charm in `CharmBridge`

Currently CharmBridge only checks `legendVisible == false`. Also map `Style.legendPosition` to Charm's `LegendPosition`:

```groovy
if (chart.style?.legendPosition) {
  theme.legendPosition = mapPosition(chart.style.legendPosition)
}
```

Add a private mapping method `Style.Position` → `LegendPosition`.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## Phase 3 — Convert `DataType` constants to enum

### 3a. Convert `DataType` to enum

**File:** `DataType.groovy`

```groovy
// Before
class DataType {
  static final String NUMERIC = "numeric"
  static final String CHARACTER = "character"
  ...
}

// After
enum DataType {
  NUMERIC, CHARACTER

  static DataType of(Class columnType) { ... }
  static boolean isCharacter(Class columnType) { ... }
  ...
}
```

Move the classification logic (`dataType()`, `isCharacter()`, `equals()`, `differs()`) to instance/static methods on the enum. Keep `sqlType()` as a static utility method.

### 3b. Update callers

Search for `DataType.NUMERIC`, `DataType.CHARACTER`, `DataType.dataType()`, `DataType.isCharacter()` across the codebase and update to enum usage.

### Verification

```bash
./gradlew :matrix-charts:test :matrix-ggplot:test -Pheadless=true
```

---

## Phase 4 — Type closure parameters in `CharmBridge`

### 4a. Replace `Object` closure parameters with specific types

**File:** `CharmBridge.groovy`

```groovy
// Before (buildBoxSpec)
chart.categorySeries.eachWithIndex { Object category, int idx ->
  values.each { Object val ->

// After
chart.categorySeries.eachWithIndex { Serializable category, int idx ->
  values.each { Number val ->
```

Since `categorySeries` holds category labels (typically String) and `valueSeries` holds numbers, use the narrowest safe type. With `List<?>` on `categorySeries`, closures can't be tighter than `Object` without a cast — so the practical approach here is to cast the list at the top of the method:

```groovy
List<String> categories = chart.categorySeries.collect { it.toString() }
```

Apply the same pattern in `buildPieSpec`, `buildLongFormatMatrix`.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## Phase 5 — Legend API

### 5a. Implement `Legend` class with typed properties

**File:** `Legend.groovy`

```groovy
@CompileStatic
class Legend {
  /** Legend title text. */
  String title

  /** Whether the legend is visible. */
  boolean visible = true

  /** Legend position. */
  Style.Position position = Style.Position.RIGHT

  /** Legend background color. */
  Color backgroundColor

  /** Legend font. */
  Font font

  /** Legend direction: vertical or horizontal key layout. */
  Direction direction = Direction.VERTICAL

  static enum Direction {
    VERTICAL, HORIZONTAL
  }
}
```

### 5b. Add builder support for Legend

**File:** `Chart.groovy` (ChartBuilder)

Add individual fluent methods so users don't have to construct a Legend object:

```groovy
B legendTitle(String title) { ensureLegend(); legend.title = title; this as B }
B legendVisible(boolean visible) { ensureLegend(); legend.visible = visible; this as B }
B legendPosition(Style.Position position) { ensureLegend(); legend.position = position; this as B }
B legendFont(Font font) { ensureLegend(); legend.font = font; this as B }
B legendBackgroundColor(Color color) { ensureLegend(); legend.backgroundColor = color; this as B }
B legendDirection(Legend.Direction direction) { ensureLegend(); legend.direction = direction; this as B }

private void ensureLegend() {
  if (legend == null) legend = new Legend()
}
```

### 5c. Bridge Legend properties to Charm in `CharmBridge`

**File:** `CharmBridge.groovy` (in `applyLabelsAndTheme`)

Map Legend fields to Charm theme properties:

```groovy
Legend legend = chart.legend
if (legend != null) {
  if (!legend.visible) {
    theme.legendPosition = LegendPosition.NONE
  } else if (legend.position) {
    theme.legendPosition = mapPosition(legend.position)
  }
  if (legend.direction) {
    theme.legendDirection = mapDirection(legend.direction)
  }
  if (legend.backgroundColor) {
    theme.legendBackground = new ElementRect(fill: colorToHex(legend.backgroundColor))
  }
  if (legend.title) {
    labels.guides['color'] = legend.title
    labels.guides['fill'] = legend.title
  }
}
```

### 5d. Migrate `Style` legend fields to `Legend`

Move legend-related fields from `Style` to `Legend`:
- `Style.legendVisible` → `Legend.visible`
- `Style.legendPosition` → `Legend.position`
- `Style.legendFont` → `Legend.font`
- `Style.legendBackgroundColor` → `Legend.backgroundColor`

Keep the `Style` fields as deprecated delegates that forward to the chart's `Legend` instance for a transition period, or remove them outright since backwards compatibility is not a concern.

Update `CharmBridge.applyLabelsAndTheme()` to read from `Legend` instead of `Style`.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

Add tests to `ChartBuilderTest` verifying legend configuration via the builder and SVG output.

---

## Phase 6 — Fluent Style configuration in builder

### 6a. Add individual style methods to `ChartBuilder`

**File:** `Chart.groovy` (ChartBuilder)

```groovy
B plotBackgroundColor(Color color) { ensureStyle(); style.plotBackgroundColor = color; this as B }
B chartBackgroundColor(Color color) { ensureStyle(); style.chartBackgroundColor = color; this as B }
B titleVisible(boolean visible) { ensureStyle(); style.titleVisible = visible; this as B }
B xAxisVisible(boolean visible) { ensureStyle(); style.xAxisVisible = visible; this as B }
B yAxisVisible(boolean visible) { ensureStyle(); style.yAxisVisible = visible; this as B }
B css(String css) { ensureStyle(); style.css = css; this as B }

private void ensureStyle() {
  if (style == null) style = new Style()
}
```

This allows:
```groovy
BarChart.builder(data)
    .title('Sales')
    .x('product')
    .y('revenue')
    .plotBackgroundColor(Color.WHITE)
    .legendVisible(false)
    .build()
```

### 6b. Bridge new Style fields to Charm

Extend `CharmBridge.applyLabelsAndTheme()` to handle the axis visibility and CSS fields:

- `Style.xAxisVisible == false` → set Charm `axisLineX` / `axisTextX` / `axisTicksX` to element_blank
- `Style.yAxisVisible == false` → same for Y axis
- `Style.css` → inject into SVG `<style>` block (if supported by renderer)

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

Add tests to `ChartBuilderTest` verifying fluent style configuration.

---

## Phase 7 — AxisScale validation

### 7a. Add validation to `AxisScale`

**File:** `AxisScale.groovy`

Validate in the constructor and setters:

```groovy
@CompileStatic
class AxisScale {
  private final BigDecimal start
  private final BigDecimal end
  private final BigDecimal step

  AxisScale(BigDecimal start, BigDecimal end, BigDecimal step) {
    if (start >= end) {
      throw new IllegalArgumentException("start ($start) must be less than end ($end)")
    }
    if (step <= 0) {
      throw new IllegalArgumentException("step ($step) must be positive")
    }
    this.start = start
    this.end = end
    this.step = step
  }
}
```

Make fields `final` (immutable after construction). Remove the no-arg constructor and setters — the builder already creates `AxisScale` via the 3-arg constructor.

### 7b. Check for callers using the no-arg constructor

Search for `new AxisScale()` (no-arg). If any exist, replace with 3-arg construction.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

Add a test to verify that invalid AxisScale values throw `IllegalArgumentException`.

---

## Summary

| Phase | Scope | Risk | Files changed |
|-------|-------|------|---------------|
| 1 | `@CompileStatic`, explicit types, typed collections | Low | All chart classes, `Style`, `AxisScale`, `Legend`, `DataType` |
| 2 | Style field naming, typed Position setter, bridge mapping | Low | `Style`, `CharmBridge` |
| 3 | DataType enum conversion | Low | `DataType`, callers |
| 4 | Typed closure params in CharmBridge | Low | `CharmBridge` |
| 5 | Legend API + builder + bridge | Medium | `Legend`, `Chart`, `CharmBridge`, `Style` |
| 6 | Fluent style in builder + bridge | Medium | `Chart`, `CharmBridge` |
| 7 | AxisScale validation + immutability | Low | `AxisScale` |
