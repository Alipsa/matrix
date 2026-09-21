# Matrix-pict Release History

## v0.6.0, unreleased

**New chart types**

- `HeatmapChart` renders wide numeric matrix data as tiles, with optional value labels
  and two- or three-colour gradients. Value labels retain their natural scale by default;
  `valueDecimals(int)` rounds them and `labelColor(Color)` sets a fixed label colour.
- `CorrelationHeatmapChart` computes Pearson, Spearman, or Kendall correlations with a
  fixed `[-1, 1]` diverging scale and two-decimal value labels by default.
- `RadarChart` renders one polygon per matrix row across three or more numeric spokes,
  with optional per-column normalization and radial scales.

**API improvements**

- All builders support positional and named `seriesColors(...)` overrides. Resolution is
  named colour, positional colour, then Charm's default palette.
- Histograms expose stable value-series names for named colour overrides.

**Bug fixes**

- `Histogram.ranges` no longer drops the maximum value when `(max - min) / bins` rounds down;
  the last bin's upper bound is now exactly the column maximum.
- `Histogram` ignores `null` values and rejects `bins` that are `null` or not positive with a
  clear `IllegalArgumentException` instead of `NumberFormatException` / `ArithmeticException`.
- `BoxChart` ignores rows whose category is `null` instead of throwing `NullPointerException`.
- `BarChart.create(title, type, direction, categories)` with no value columns no longer names
  series `["1", "0"]`.
- Rendering now rejects value series whose length differs from the category list instead of
  silently plotting `null`s or dropping values. This affects the deprecated positional factories
  (`AreaChart.create(title, categories, values...)`, `PieChart.create(title, categories, values)`,
  `BarChart.create(title, type, direction, categories, values...)`) and charts whose
  `categorySeries`/`valueSeries` — or, for `BubbleChart`, the `sizeSeries`/`groupSeries`
  properties — were assigned directly after construction. A grouped `BubbleChart` (built with
  `group(...)`) whose `groupSeries` is later emptied is rejected instead of silently rendering ungrouped.
- `ScatterChart.create(title, data, x, y)` now sets `valueSeriesNames` to `[y]`, so named
  `seriesColors` overrides apply.
- A non-numeric `yLabels` key is reported as `IllegalArgumentException("yLabels key '…' is not numeric")`.
- `RadarChart.fillAlpha` affects only the polygon fill (Charm now renders polygon alpha as
  `fill-opacity`); previously the outline faded too.

**API improvements (continued)**

- `Plot.jpg`, `Plot.pdf`, `Plot.jfx` and `Plot.swing` gained overloads taking explicit
  `width` and `height`; `Plot.base64(Chart, int, int)` replaces the `double` variant, which is
  deprecated.
- `Chart.validateSeries(Matrix[])` is deprecated; no factory accepts `Matrix[]` input.

## v0.5.0, 2026-06-27

First standalone release of `matrix-pict`. The `se.alipsa.matrix.pict` package
previously lived inside `matrix-charts`; update your dependency from `matrix-charts`
to `matrix-pict`. The package name `se.alipsa.matrix.pict` is unchanged.

**Bug Fixes**
- `DataType.sqlType(BigDecimal)` now returns `'DECIMAL'` instead of falling through
  to the `'BLOB'` default.
- `DataType.sqlType(BigInteger)` now returns `'NUMERIC'` instead of falling through
  to the `'BLOB'` default.
- `Chart.toString()` no longer throws `NullPointerException` on a chart where the
  series fields have not yet been populated.

**API Improvements**
- Added `yLabels(Map<String, String>)` fluent builder method to `Chart.ChartBuilder`
  so custom y-axis break labels can be set in the builder chain instead of requiring
  `chart.style.yLabels = [...]` after build.
- Added `Chart.getValueSeries(int)` (typo fix; the original `getValueSerie(int)` is
  deprecated but retained as a delegating alias for source compatibility).
- Added JavaBean-convention alias getters `getXAxisTitle()`, `getYAxisTitle()`,
  `getXAxisScale()`, `getYAxisScale()`. The original lowercase getters
  (`getxAxisTitle()`, etc.) are retained; Groovy property access (`chart.xAxisTitle`)
  continues to use them and is unaffected.
- `Plot.png(Chart, File, int, int)` and `Plot.png(Chart, OutputStream, int, int)` added,
  matching `Plot.svg()`. The previous `double` overloads are deprecated but still
  compile; migrate to the `int` overloads.
- `BarChart.createHorizontal()` and `BarChart.createVertical()` are now annotated
  `@Deprecated`; use `BarChart.builder(data)` for new code.

**Code Quality**
- Builders for `BoxChart`, `Histogram`, and `BubbleChart` no longer call their own
  deprecated `create()` factory methods internally; core creation logic has been
  extracted to private static methods shared by both builders and factories.
- `CharmBridge.applyYLabels()` rewritten with idiomatic Groovy (sort by BigDecimal
  comparison on string keys) instead of `AbstractMap.SimpleEntry`.
- `validateSeries` loop simplified to start at index 1, eliminating the
  `if (idx == 0) { continue }` pattern.
- GroovyDoc added to `MinMax` class (`Histogram.groovy`), `AxisScale` getters, and
  `Style.xAxisVisible` / `Style.yAxisVisible` fields.

**Documentation**
- Tutorial (`docs/tutorial/13-matrix-charts.md`) updated: `ChartType.NONE` →
  `ChartType.BASIC`; removed obsolete `setColors()`, `setLegendSide(javafx.geometry.Side)`,
  and `javafx.scene.paint.Color` references; corrected `setXAxisLabel` →
  `setXAxisTitle`.
- `yLabels(Map)` builder example added to cookbook and pict API guide.

---

## Pre-v0.5.0 History (formerly in matrix-charts)

Before v0.5.0, the `se.alipsa.matrix.pict` package was part of `matrix-charts`.
The sections below summarise the pict-relevant changes from that period.

### matrix-charts v0.5.0

**Architecture**
- The pict API (`se.alipsa.matrix.pict`) is backed by Charm via `CharmBridge`.
  Use `Plot` as the PICT-facing export helper.
- `Plot.png()` no longer requires JavaFX toolkit initialization.

**Breaking changes affecting pict users**
- `Plot.jfx()` return type changed from `javafx.scene.chart.Chart` to
  `javafx.scene.Node`. Code passing the result to `view()` or methods accepting
  `Node` is unaffected.
- Legacy backend packages removed: `se.alipsa.matrix.charts.jfx`,
  `se.alipsa.matrix.charts.swing`, `se.alipsa.matrix.charts.png`,
  `se.alipsa.matrix.charts.svg`, `se.alipsa.matrix.charts.util.StyleUtil`.
  Use the `chartexport` classes (`ChartToPng`, `ChartToJfx`, `ChartToSwing`,
  `ChartToJpeg`, `ChartToImage`) instead.

**New features for pict**
- Fluent builder API introduced for all chart types: `AreaChart.builder(data)`,
  `BarChart.builder(data)`, `BoxChart.builder(data)`, `BubbleChart.builder(data)`,
  `Histogram.builder(data)`, `LineChart.builder(data)`, `PieChart.builder(data)`,
  `ScatterChart.builder(data)`.
- `BubbleChart` fully implemented with factory and builder variants, including
  grouped bubbles via `.group(columnName)`.
- Legend API centered on the `Legend` class with fluent builder methods
  (`legendTitle`, `legendPosition`, `legendFont`, `legendBackgroundColor`,
  `legendDirection`, `legendVisible`).
- Builder style methods: `titleVisible`, `xAxisVisible`, `yAxisVisible`, `css`.
- `AxisScale` is now immutable and validated at construction (`start < end`,
  `step > 0`, non-null values).
- `Style.css` injects raw CSS into the Charm-rendered SVG output.
- `Style.yLabels` (map of numeric break strings to display labels) is applied
  by the Charm bridge.
- Histogram bin calculations preserve exact bin boundaries internally; labels
  are rounded only for display.
- `Charm stylesheet(String)` injects raw CSS into rendered SVG; `Style.css` uses
  that path.

### matrix-charts v0.4.0, 2026-01-31

- Refactored the pict chart builder API: `setLegend`, `setGridLines`, `setTitle`,
  `setSubTitle`, `setCoordinateSystem`, and `setStyle` replaced multiple `add*`
  methods.
- Introduced the `Style` class; `Title` and `SubTitle` now extend a new abstract
  `Text` class.
- Applied `@CompileStatic` across pict classes.
- Updated `matrix-charts` to v0.4.0.

### matrix-charts v0.3.1, 2025-07-19

- Dependency upgrade: `org.jfree:jfreechart` 1.5.5 → 1.5.6.

### matrix-charts v0.3.0, 2025-04-01

- Enabled fluent interaction on pict chart types.

### matrix-charts v0.2, 2025-03-12

- Requires JDK 21.

### matrix-charts v0.1, 2025-02-16

- Initial release of the pict API within `matrix-charts`.
