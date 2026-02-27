# Charm Guide

A comprehensive guide to using the `se.alipsa.matrix.charm` package for creating data visualizations in Groovy.

## Table of Contents
- [Introduction](#introduction)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [DSL Reference](#dsl-reference)
- [Layers and Geoms](#layers-and-geoms)
- [Scales](#scales)
- [Themes](#themes)
- [Faceting](#faceting)
- [Coordinate Systems](#coordinate-systems)
- [Labels and Annotations](#labels-and-annotations)
- [Lifecycle](#lifecycle)
- [Output Formats](#output-formats)
- [Static Compilation](#static-compilation)
- [Relationship to gg and charts APIs](#relationship-to-gg-and-charts-apis)
- [Migration Guide](#migration-guide)

## Introduction

Charm is the core Grammar of Graphics engine for matrix-charts. It provides a Groovy-native DSL for building chart specifications that compile into immutable chart models and render to SVG.

There is one engine powering all three charting APIs in matrix-charts:

- **Charm** -- idiomatic Groovy closure DSL (this guide)
- **[gg](ggPlot.md)** -- ggplot2-compatible wrapper for R migration
- **charts** -- familiar chart-type-first API (AreaChart, BarChart, etc.)

All three APIs produce the same SVG output through the Charm renderer.

### Key Features
- Closure-based DSL with `@CompileStatic` support
- Immutable compiled chart models (thread-safe after `build()`)
- Deterministic lifecycle: specification -> compilation -> rendering
- String-based aesthetic mappings (column names as strings)
- SVG output via gsvg, exportable to PNG, JPEG, JavaFX, and Swing

## Quick Start

### Dependencies

```groovy
implementation(platform('se.alipsa.matrix:matrix-bom:2.4.0'))
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-charts'
implementation 'se.alipsa.matrix:matrix-stats'
```

### Your First Chart

```groovy
import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.datasets.Dataset

def chart = plot(Dataset.mpg()) {
  mapping {
    x = 'cty'
    y = 'hwy'
    color = 'class'
  }
  layers {
    geomPoint().size(2).alpha(0.7)
    geomSmooth().method('lm')
  }
  labels {
    title = 'City vs Highway MPG'
    x = 'City MPG'
    y = 'Highway MPG'
  }
}.build()

chart.writeTo('mpg_chart.svg')
```

### Rendering and Exporting

```groovy
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.chartexport.ChartToPng

// Render to SVG object (default 800x600)
Svg svg = chart.render()

// Render at custom dimensions
Svg svg = chart.render(640, 420)

// Write to SVG file
chart.writeTo('chart.svg')

// Export to PNG
ChartToPng.export(chart, new File('chart.png'))
```

## Core Concepts

### The Grammar of Graphics

Charm follows the Grammar of Graphics approach where plots are built by composing layers of specification:

```groovy
plot(data) {
  mapping { ... }   // Aesthetic mappings (data -> visual properties)
  layers { ... }    // Geometric objects (how to display)
  scale { ... }     // Scale transformations (data -> coordinates)
  theme { ... }     // Visual styling
  facet { ... }     // Multi-panel layout
  coord { ... }     // Coordinate system
  labels { ... }    // Titles and labels
  annotate { ... }  // Annotations
}
```

### DSL Grammar Rules

Charm uses two syntax forms depending on context:

**Rule 1 -- Inside closures: use property assignment (`=`)**

```groovy
mapping {
  x = 'cty'        // correct
  y = 'hwy'        // correct
}
```

**Rule 2 -- Outside closures: use named arguments (`:`)**

```groovy
spec.mapping(x: 'cty', y: 'hwy')   // correct
spec.addLayer(geomPoint().size(2))
```

Mixing these forms (colon syntax inside closures) will silently fail. The closure form creates property assignments, not map entries.

### Column References

Column references use string column names inside `mapping {}` blocks:

```groovy
mapping {
  x = 'cty'
  y = 'hwy'
  color = 'class'
}
```

You can also look up column names from the data by index:

```groovy
mapping {
  x = data[0].name    // first column name
  y = data[1].name    // second column name
}
```

## DSL Reference

### Entry Points

```groovy
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.charm.Charts.chart  // alias for import conflicts

// Closure DSL (primary)
PlotSpec spec = plot(data) {
  mapping { x = 'x'; y = 'y' }
  layers { geomPoint() }
}

// Programmatic (for @CompileStatic)
import se.alipsa.matrix.charm.geom.PointBuilder

PlotSpec spec = plot(data)
spec.mapping(x: 'x', y: 'y')
spec.addLayer(new PointBuilder().size(2))
```

### Aesthetics (Mappings)

Map data columns to visual properties. Every value in a `mapping {}` block is a **column name** from your data matrix:

```groovy
mapping {
  x = 'x_column'         // x position
  y = 'y_column'         // y position
  color = 'category'     // point/line color (mapped from column values)
  fill = 'fill_column'   // area fill color (mapped from column values)
  size = 'size_column'   // point/line size
  shape = 'shape_column' // point shape
  group = 'group_column' // grouping variable
}
```

For example, `color = 'class'` means "read values from the `class` column and assign a distinct color to each unique value." The color scale is chosen automatically:

- **Categorical data** (strings) -- each unique value gets a distinct color from a discrete palette
- **Numeric data** -- values are interpolated along a gradient

This is different from setting a **literal color** on a layer (see [Layer Parameters](#layer-parameters) below).

## Layers and Geoms

### Builder DSL

Layers are added inside a `layers {}` block using fluent builder methods:

```groovy
plot(data) {
  mapping { x = 'x'; y = 'y' }
  layers {
    geomPoint()                 // scatter plot
    geomLine()                  // line plot
    geomSmooth().method('lm')   // regression / smoothed line
    geomTile()                  // heatmap tiles
    geomArea()                  // filled area
    geomPie()                   // pie chart
  }
}
```

### Layer Parameters

Configure layers using fluent builder methods. Unlike `mapping {}`, values here are **literal** -- `fill('#336699')` sets all points to that color, rather than mapping from data:

```groovy
layers {
  geomPoint().size(3).alpha(0.5).fill('#336699')
  geomSmooth().method('lm')
  geomHistogram().bins(4).fill('#cc6677')
}
```

When both a mapping and a layer parameter exist for the same aesthetic, the mapped values take priority.

### Programmatic addLayer()

For `@CompileStatic` or dynamic layer creation, use `addLayer()` with builder instances:

```groovy
import se.alipsa.matrix.charm.geom.PointBuilder
import se.alipsa.matrix.charm.geom.SmoothBuilder

plot(data) {
  mapping(x: 'x', y: 'y')
  addLayer(new PointBuilder().size(2).alpha(0.7))
  addLayer(new SmoothBuilder().method('lm'))
}
```

Available geom builders: `PointBuilder`, `LineBuilder`, `BarBuilder`, `ColBuilder`, `TileBuilder`,
`HistogramBuilder`, `BoxplotBuilder`, `SmoothBuilder`, `AreaBuilder`, `PieBuilder`, `DensityBuilder`,
`ViolinBuilder`, `TextBuilder`, `LabelBuilder`, `SegmentBuilder`, `HlineBuilder`, `VlineBuilder`,
`AblineBuilder`, `RibbonBuilder`, `ErrorbarBuilder`, `ErrorbarhBuilder`, `PathBuilder`, `StepBuilder`,
`JitterBuilder`, `RugBuilder`, `ContourBuilder`, `HexBuilder`, `Bin2dBuilder`, `RasterBuilder`,
`DotplotBuilder`, `FunctionBuilder`, `CurveBuilder`, `RectBuilder`, `CrossbarBuilder`,
`LinerangeBuilder`, `PointrangeBuilder`, `FreqpolyBuilder`, `QqBuilder`, `QqLineBuilder`,
`QuantileBuilder`, `CountBuilder`, `PolygonBuilder`, `MapBuilder`, `Density2dBuilder`,
`Density2dFilledBuilder`, `ContourFilledBuilder`, `SpokeBuilder`, `MagBuilder`, `ParallelBuilder`,
`LogticksBuilder`, `BlankBuilder`, `RasterAnnBuilder`, `SfBuilder`, `SfLabelBuilder`, `SfTextBuilder`,
`CustomBuilder`.

### Layer-Level Aesthetics

Layers can override plot-level aesthetics:

```groovy
plot(data) {
  mapping { x = 'x'; y = 'y' }
  layers {
    geomPoint()
    geomPoint().inheritMapping(false).mapping(x: 'other_x', y: 'other_y')
  }
}
```

## Scales

### Built-in Transforms

```groovy
scale {
  x = log10()       // logarithmic x-axis
  y = sqrt()        // square root y-axis
  x = reverse()     // reversed axis
  x = date()        // date/time axis
  x = time()        // time axis
  color = continuous()
  fill = discrete()
}
```

### Custom Transforms

```groovy
scale {
  x = custom('doubler',
    { BigDecimal v -> v * 2 },    // forward
    { BigDecimal v -> v / 2 }     // inverse
  )
}
```

## Themes

Configure visual styling with flat property setters:

```groovy
theme {
  legendPosition = 'top'       // 'top', 'bottom', 'left', 'right', 'none'
  legendDirection = 'horizontal'
  axisLineWidth = 0.75
  textSize = 12
  textColor = '#333333'
  gridColor = '#eeeeee'
  baseFamily = 'sans-serif'
  baseSize = 11
}
```

## Faceting

### Grid Facets

Arrange panels in a grid by row and/or column variables:

```groovy
facet {
  rows = ['year']
  cols = ['drv']
}
```

Setting `rows` and/or `cols` automatically selects GRID mode. This is equivalent to `facet_grid()` in the gg API.

### Wrap Facets

Wrap panels into rows/columns by one or more variables:

```groovy
facet {
  wrap {
    vars = ['drv']
    ncol = 2
  }
}
```

This is equivalent to `facet_wrap()` in the gg API.

Grid and wrap modes cannot be combined in the same `facet {}` block.

## Coordinate Systems

```groovy
coord {
  type = 'polar'
  theta = 'y'
  start = 0
}
```

Available types: `CARTESIAN` (default), `POLAR`, and others.

## Labels and Annotations

### Labels

```groovy
labels {
  title = 'City vs Highway MPG'
  subtitle = 'Data from EPA fuel economy'
  caption = 'Source: fueleconomy.gov'
  x = 'City MPG'
  y = 'Highway MPG'
}
```

### Annotations

```groovy
annotate {
  text {
    x = 2
    y = 12
    label = 'Peak'
  }
  rect {
    xmin = 1
    xmax = 3
    ymin = 9
    ymax = 13
    alpha = 0.15
  }
  segment {
    x = 1
    xend = 3
    y = 10
    yend = 10
    color = 'red'
  }
}
```

## Lifecycle

Charm defines a four-step lifecycle:

### 1. Specification (mutable)

```groovy
PlotSpec spec = plot(data) {
  mapping { x = 'x'; y = 'y' }
  layers { geomPoint() }
}
```

The `PlotSpec` is mutable -- you can add layers, change theme settings, etc.

### 2. Compilation (immutable)

```groovy
Chart chart = spec.build()
```

`build()` validates mappings and facet variables, applies defaults, resolves aesthetics inheritance, and freezes an immutable chart model. After `build()`, modifications to the original `PlotSpec` do not affect the compiled `Chart`.

### 3. Rendering

```groovy
// Default dimensions (800x600)
Svg svgDefault = chart.render()

// Custom dimensions
Svg svgCustom = chart.render(640, 420)
```

Rendering is side-effect free and deterministic. The same chart produces the same SVG every time.

### 4. Writing (convenience)

```groovy
chart.writeTo('plot.svg')
```

`writeTo()` internally calls `render()` and writes the SVG to disk.

### How this differs from gg and charts

The explicit `build()` + `render()` lifecycle is specific to the Charm API. The other
APIs handle these steps internally:

- **gg:** `GgChart.render()` compiles and renders in a single call -- there is no separate `build()` step.
- **charts:** Neither `build()` nor `render()` is called by user code. Export methods like
  `Plot.png(chart, file)` and `ChartToPng.export(chart, file)` handle everything through
  an internal bridge.

### Custom Render Configuration

For simple dimension changes, use `render(width, height)`:

```groovy
Svg svg = chart.render(640, 420)
```

For full control over margins, tick counts, and other rendering parameters, use
the fluent `renderConfig()` builder:

```groovy
Svg svg = chart.renderConfig()
    .width(640)
    .height(420)
    .marginLeft(100)
    .render()
```

## Output Formats

### SVG (default)

```groovy
chart.writeTo('plot.svg')
```

### PNG

```groovy
import se.alipsa.matrix.chartexport.ChartToPng

ChartToPng.export(chart, new File('plot.png'))

// Or to an OutputStream
ByteArrayOutputStream baos = new ByteArrayOutputStream()
ChartToPng.export(chart, baos)
```

### JPEG

```groovy
import se.alipsa.matrix.chartexport.ChartToJpeg

ChartToJpeg.export(chart, new File('plot.jpg'), 0.9)
```

### JavaFX

```groovy
import se.alipsa.matrix.chartexport.ChartToJfx

def svgImage = ChartToJfx.export(chart)
// Use svgImage in your JavaFX application
```

### Swing

```groovy
import se.alipsa.matrix.chartexport.ChartToSwing

def panel = ChartToSwing.export(chart)
// Add panel to your Swing container
```

## Static Compilation

Charm core is implemented with `@CompileStatic`. Users writing `@CompileStatic` chart code should use the programmatic API with string mappings:

```groovy
import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.geom.PointBuilder
import static se.alipsa.matrix.charm.Charts.plot

@CompileStatic
class MyCharts {
  Chart createChart(Matrix data) {
    PlotSpec spec = plot(data)
    spec.mapping(x: 'x', y: 'y', color: 'category')
    spec.addLayer(new PointBuilder().size(2).alpha(0.7))
    spec.build()
  }
}
```

## Relationship to gg and charts APIs

All three APIs in matrix-charts share the same Charm rendering engine:

```
charm ----renders----> Svg (gsvg)
gg ------adapts------> charm ---renders-> Svg
charts --builds------> charm ---renders-> Svg
         Svg ----exports----> chartexport ------> PNG/JPEG/JFX/Swing
```

- **Charm** is the core. Use it for new code and when you want the most idiomatic Groovy experience.
- **gg** is a compatibility wrapper that builds the same Charm `PlotSpec` under the hood. Use it when porting R code or when you prefer ggplot2 syntax.
- **charts** (`AreaChart`, `BarChart`, etc.) is a chart-type-first API backed by Charm via `CharmBridge`. The `Plot` class is `@Deprecated` but continues to work.

## Migration Guide

### Migrating from old charts backend

The `charts` package previously used multiple backend-specific converters:

| Old path                                          | Status              | Replacement                                                  |
|---------------------------------------------------|---------------------|--------------------------------------------------------------|
| `charts.jfx.*` (JfxConverter, JfxBoxChart, ...)   | **Removed**         | `ChartToJfx.export(chart)` via chartexport                   |
| `charts.swing.*` (SwingPlot, SwingConverter, ...) | **Removed**         | `ChartToSwing.export(chart)` via chartexport                 |
| `charts.png.PngConverter`                         | **Removed**         | `ChartToPng.export(chart, file)` via chartexport             |
| `charts.svg.SvgBarChart` / `SvgChart`             | **Removed**         | Use Charm DSL or gg API, then `chart.render()` for SVG       |
| `charts.util.StyleUtil`                           | **Removed**         | No replacement needed (was JavaFX-specific)                  |
| `Plot.jfx(chart)`                                 | Deprecated, rewired | Returns `javafx.scene.Node` (was `javafx.scene.chart.Chart`) |
| `Plot.png(chart, file)`                           | Deprecated, rewired | Works as before, no longer requires JavaFX toolkit           |
| `Plot.base64(chart)`                              | Deprecated, rewired | Works as before                                              |

### Breaking changes in this release

- **`se.alipsa.matrix.charts.charmfx` removed.** Classes `CharmChartFx`, `ChartPane`, `LegendPane`, `PlotPane`, `TitlePane`, `Position`, `HorizontalLegendPane`, and `VerticalLegendPane` are deleted. Use Charm core + chartexport instead.
- **`Plot.jfx()` return type changed** from `javafx.scene.chart.Chart` to `javafx.scene.Node`. Code using `inout.view(Plot.jfx(chart))` is unaffected since `view()` accepts `Node`.
- **`org.knowm.xchart` dependency removed.** The xchart library is no longer a transitive dependency. If you depended on it, add it directly.

### Recommended migration paths

**From `SwingPlot.png(chart, file)` / `PngConverter`:**
```groovy
// Old
SwingPlot.png(chart, file)

// New (via deprecated but working Plot)
Plot.png(chart, file)

// New (via chartexport directly)
import se.alipsa.matrix.chartexport.ChartToPng
ChartToPng.export(chart, file)
```

**From direct JFX converter usage:**
```groovy
// Old
import se.alipsa.matrix.charts.jfx.JfxConverter
Node node = JfxConverter.convert(chart)

// New
import se.alipsa.matrix.chartexport.ChartToJfx
def node = ChartToJfx.export(chart)

// Or via deprecated Plot
def node = Plot.jfx(chart)
```

**From charts API to Charm DSL:**
```groovy
// Old charts API
def barChart = BarChart.createVertical("Sales", data, "month", ChartType.BASIC, "sales")
Plot.png(barChart, new File("chart.png"))

// New Charm DSL
import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.chartexport.ChartToPng

def chart = plot(data) {
  mapping { x = 'month'; y = 'sales' }
  layers { geomBar() }
  labels { title = 'Sales' }
}.build()

ChartToPng.export(chart, new File('chart.png'))
```

## Examples

### Scatter Plot with Regression Line

```groovy
import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.datasets.Dataset

def chart = plot(Dataset.mpg()) {
  mapping {
    x = 'cty'
    y = 'hwy'
  }
  layers {
    geomPoint()
    geomSmooth().method('lm')
  }
  labels {
    title = 'City vs Highway MPG'
    x = 'City MPG'
    y = 'Highway MPG'
  }
}.build()

chart.writeTo('scatter_regression.svg')
```

### Faceted Plot

```groovy
def chart = plot(Dataset.mpg()) {
  mapping {
    x = 'cty'
    y = 'hwy'
  }
  layers { geomPoint() }
  facet {
    wrap {
      vars = ['drv']
      ncol = 3
    }
  }
  labels { title = 'MPG by Drive Type' }
}.build()

chart.writeTo('faceted.svg')
```

### Heatmap

```groovy
import se.alipsa.matrix.core.Matrix

def data = Matrix.builder().data(
    x: [1, 2, 3, 1, 2, 3, 1, 2, 3],
    y: [1, 1, 1, 2, 2, 2, 3, 3, 3],
    value: [5, 10, 15, 8, 12, 18, 3, 7, 20]
).types(Integer, Integer, Integer).build()

def chart = plot(data) {
  mapping {
    x = 'x'
    y = 'y'
    fill = 'value'
  }
  layers { geomTile() }
  labels { title = 'Heatmap' }
}.build()

chart.writeTo('heatmap.svg')
```

### Multi-Layer Chart with Custom Theme

```groovy
def chart = plot(Dataset.mpg()) {
  mapping {
    x = 'cty'
    y = 'hwy'
    color = 'class'
  }
  layers {
    geomPoint().size(2)
    geomSmooth().method('lm')
  }
  scale {
    x = log10()
  }
  theme {
    legendPosition = 'top'
    axisLineWidth = 0.75
  }
  labels {
    title = 'City vs Highway MPG'
    subtitle = 'Log-scaled x-axis'
  }
}.build()

chart.writeTo('styled_chart.svg')
```

## Additional Resources

- **[ggPlot.md](ggPlot.md)** -- ggplot2-compatible API guide (54+ geoms)
- **[charm-specification.md](charm-specification.md)** -- Charm v1 formal specification
- **Examples** -- See `matrix-charts/examples/charm/` for runnable scripts
- **API Documentation** -- [JavaDoc](https://javadoc.io/doc/se.alipsa.matrix/matrix-charts)
