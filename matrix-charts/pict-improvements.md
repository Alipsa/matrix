# Pict Package Improvements — IDE Support & Usability

Phased plan for improving IDE auto-complete, type safety, and usability of `se.alipsa.matrix.pict`.

---

## Phase 1 — Type safety foundations

Low-risk changes that don't alter public API semantics. Each change is independently testable.

### 1a. Add `@CompileStatic` to chart classes and support classes

Add `@CompileStatic` to all chart and support classes in the pict package:

| File | Class |
|------|-------|
| `Chart.groovy` | `Chart`, `ChartBuilder` |
| `AreaChart.groovy` | `AreaChart`, `Builder` |
| `BarChart.groovy` | `BarChart`, `Builder` |
| `BoxChart.groovy` | `BoxChart`, `Builder` |
| `BubbleChart.groovy` | `BubbleChart` |
| `Histogram.groovy` | `Histogram`, `Builder`, `MinMax` |
| `LineChart.groovy` | `LineChart`, `Builder` |
| `PieChart.groovy` | `PieChart`, `Builder` |
| `ScatterChart.groovy` | `ScatterChart`, `Builder` |
| `Style.groovy` | `Style` |
| `AxisScale.groovy` | `AxisScale` |
| `Legend.groovy` | `Legend` |
| `DataType.groovy` | `DataType` |
| `Plot.groovy` | `Plot` |
| `InitializationException.groovy` | `InitializationException` |

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
protected List<List<?>> valueSeries
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

### 2a. [x] Fix `Style` field capitalization

Rename `XAxisVisible` → `xAxisVisible` and `YAxisVisible` → `yAxisVisible`. Since backwards compatibility is not a concern, a direct rename is sufficient.

**File:** `Style.groovy`

Completed in Phase 1 — fields were already named correctly.

### 2b. [x] Add typed `Position` setter to `Style`

Currently only a `setLegendPosition(String)` setter exists. Add a typed overload:

```groovy
void setLegendPosition(Position pos) {
  legendPosition = pos
}
```

This lets users write `chart.style.legendPosition = Position.TOP` directly.

Completed in Phase 1 — both `setLegendPosition(String)` and `setLegendPosition(Position)` overloads already exist.

### 2c. [x] Map `Style.legendPosition` to Charm in `CharmBridge`

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
# Results: SUCCESS
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

## Phase 8 — Implement BubbleChart

Charm's POINT geom already supports size aesthetic mapping, so the BubbleChart stub can be fully implemented. A bubble chart is a scatter plot where each point's radius encodes a third numeric variable.

### 8a. Promote `BubbleChart` from stub to full chart class

**File:** `BubbleChart.groovy`

Remove `@Deprecated` and the stub `create()` method. Make `BubbleChart` extend `Chart<BubbleChart>` and add a `sizeSeries` field to hold the size column data:

```groovy
@CompileStatic
class BubbleChart extends Chart<BubbleChart> {

  /** Size values — one per data point, mapped to point radius. */
  List<? extends Number> sizeSeries = []

  /** Optional grouping column name. */
  String groupColumn
}
```

### 8b. Add factory methods

**File:** `BubbleChart.groovy`

Provide two `create()` overloads matching the original stub signature (with optional `groupCol`):

```groovy
static BubbleChart create(String title, Matrix data, String xCol, String yCol, String sizeCol) {
  BubbleChart chart = new BubbleChart()
  chart.title = title
  chart.categorySeries = data.column(xCol) as List<?>
  chart.valueSeries = [data.column(yCol) as List<?>]
  chart.sizeSeries = data.column(sizeCol) as List<? extends Number>
  chart.xAxisTitle = xCol
  chart.yAxisTitle = yCol
  return chart
}

static BubbleChart create(String title, Matrix data, String xCol, String yCol, String sizeCol, String groupCol) {
  BubbleChart chart = create(title, data, xCol, yCol, sizeCol)
  if (groupCol) {
    chart.groupColumn = groupCol
    List<?> groups = data.column(groupCol) as List<?>
    // Re-structure as grouped series — store group names and rebuild value/size per group
    // For simplicity, store groupCol data alongside and let CharmBridge handle grouping via aesthetic mapping
    chart.valueSeriesNames = [yCol]
  }
  return chart
}
```

The grouped variant stores the group column name and lets CharmBridge map it to the `color` aesthetic, matching how other chart types handle multi-series.

### 8c. Add fluent builder

**File:** `BubbleChart.groovy`

```groovy
static Builder builder(Matrix data) { new Builder(data) }

@CompileStatic
static class Builder extends Chart.ChartBuilder<Builder, BubbleChart> {

  private String sizeCol
  private String groupCol

  Builder(Matrix data) { super(data) }

  /** Sets the column mapped to point size. */
  Builder size(String col) { this.sizeCol = col; this }

  /** Sets the column used for grouping (color aesthetic). */
  Builder group(String col) { this.groupCol = col; this }

  BubbleChart build() {
    BubbleChart chart = groupCol
        ? BubbleChart.create(this.@title, data, xCol, yCols[0], sizeCol, groupCol)
        : BubbleChart.create(this.@title, data, xCol, yCols[0], sizeCol)
    applyTo(chart)
    chart
  }
}
```

### 8d. Add CharmBridge conversion

**File:** `CharmBridge.groovy`

Add a case in `buildSpec()`:

```groovy
case BubbleChart -> buildBubbleSpec(chart as BubbleChart)
```

Implement `buildBubbleSpec()`. Unlike other chart types, BubbleChart cannot reuse `buildLongFormatMatrix()` because it needs a `size` column. Build a dedicated long-format matrix:

```groovy
private static PlotSpec buildBubbleSpec(BubbleChart chart) {
  List<?> xValues = chart.categorySeries
  List<?> yValues = chart.valueSeries[0]
  List<? extends Number> sizeValues = chart.sizeSeries

  List<List> rows = []
  if (chart.groupColumn) {
    // Group column is stored in the original data — rebuild from chart fields
    // For grouped bubble charts, include a 'group' column for color mapping
    for (int i = 0; i < xValues.size(); i++) {
      rows.add([xValues[i], yValues[i], sizeValues[i]] as List)
    }
    // Note: grouping via color aesthetic requires the group data to be passed through
  } else {
    for (int i = 0; i < xValues.size(); i++) {
      rows.add([xValues[i], yValues[i], sizeValues[i]] as List)
    }
  }

  Matrix data = Matrix.builder()
      .columnNames('x', 'y', 'size')
      .rows(rows)
      .build()

  PlotSpec spec = Charts.plot(data)
  spec.mapping([x: 'x', y: 'y', size: 'size'])
  spec.addLayer(new PointBuilder())
  applyLabelsAndTheme(spec, chart)
  spec
}
```

For grouped bubble charts, the group column data needs to be carried through to the long-format matrix and mapped to the `color` aesthetic. This requires storing the raw group column in BubbleChart (or accepting the full Matrix). Refine the approach:

- Store the group column values as a `List<?> groupSeries` field on BubbleChart.
- In `buildBubbleSpec()`, if `groupSeries` is non-empty, add a `'group'` column to the matrix and map `color: 'group'`.

### 8e. Handle grouped bubble charts

**File:** `BubbleChart.groovy`

Add a field for group series data:

```groovy
List<?> groupSeries = []
```

Update the grouped `create()` to populate it:

```groovy
chart.groupSeries = data.column(groupCol) as List<?>
```

**File:** `CharmBridge.groovy`

In `buildBubbleSpec()`, branch on whether `groupSeries` is populated:

```groovy
if (chart.groupSeries) {
  for (int i = 0; i < xValues.size(); i++) {
    rows.add([xValues[i], yValues[i], sizeValues[i], chart.groupSeries[i]] as List)
  }
  Matrix data = Matrix.builder()
      .columnNames('x', 'y', 'size', 'group')
      .rows(rows)
      .build()
  PlotSpec spec = Charts.plot(data)
  spec.mapping([x: 'x', y: 'y', size: 'size', color: 'group'])
  spec.addLayer(new PointBuilder())
  applyLabelsAndTheme(spec, chart)
  return spec
}
```

### 8f. Add tests

**File:** `ChartsCharmIntegrationTest.groovy` (or a new `BubbleChartTest.groovy`)

```groovy
@Test
void testBubbleChartRendersScaledCircles() {
  Matrix data = Matrix.builder()
      .matrixName('BubbleData')
      .columns([x: [1, 2, 3, 4], y: [10, 20, 15, 25], size: [5, 15, 10, 20]])
      .types([Number, Number, Number])
      .build()

  BubbleChart chart = BubbleChart.create('Bubble Test', data, 'x', 'y', 'size')
  Chart charmChart = CharmBridge.convert(chart)
  Svg svg = charmChart.render()
  assertNotNull(svg)

  def circles = svg.descendants().findAll { it instanceof Circle }
  assertEquals(4, circles.size())

  // Verify circles have varying radii (size aesthetic applied)
  def radii = circles.collect { it.r as BigDecimal }.toSet()
  assertTrue(radii.size() > 1, 'Bubble chart circles should have varying radii')
}

@Test
void testBubbleChartBuilder() {
  Matrix data = Matrix.builder()
      .matrixName('BubbleData')
      .columns([x: [1, 2, 3], y: [10, 20, 30], s: [5, 10, 15]])
      .types([Number, Number, Number])
      .build()

  BubbleChart chart = BubbleChart.builder(data)
      .title('Builder Bubble')
      .x('x')
      .y('y')
      .size('s')
      .build()

  assertNotNull(chart)
  assertEquals('Builder Bubble', chart.title)
  assertEquals(3, chart.sizeSeries.size())
}

@Test
void testGroupedBubbleChart() {
  Matrix data = Matrix.builder()
      .matrixName('GroupedBubble')
      .columns([
          x: [1, 2, 3, 4],
          y: [10, 20, 15, 25],
          size: [5, 15, 10, 20],
          group: ['A', 'A', 'B', 'B']
      ])
      .types([Number, Number, Number, String])
      .build()

  BubbleChart chart = BubbleChart.create('Grouped', data, 'x', 'y', 'size', 'group')
  Chart charmChart = CharmBridge.convert(chart)
  Svg svg = charmChart.render()
  assertNotNull(svg)
}
```

### 8g. Update documentation

**File:** `docs/charts.md`

Add a BubbleChart section with factory and builder examples.

**File:** `BubbleChart.groovy`

Replace the deprecated class-level GroovyDoc with proper documentation.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

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
| 8 | Implement BubbleChart (size aesthetic) | Low | `BubbleChart`, `CharmBridge`, tests, docs |
