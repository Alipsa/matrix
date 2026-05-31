# Matrix Charts

Matrix Charts provides the Charm rendering engine and export utilities. The separate
`matrix-pict` module provides predefined chart types (AreaChart, BarChart, etc.) that
can easily be created based on Matrix data. The native representation is SVG. Export
PICT charts to PNG with `Plot.png(...)`. The `se.alipsa.matrix.chartexport` package
provides additional targets including JPEG, PDF, JavaFX, and Swing, plus lower-level
conversion APIs.

## Fluent Builder API

In addition to the static `create()` factory methods, each chart type provides a fluent builder API
that starts from `builder(data)`, chains configuration methods, and terminates with `build()`.
All existing `create()` factories remain unchanged — the builder is a purely additive alternative.

### AreaChart

```groovy
def chart = AreaChart.builder(data)
    .title('Boston Robberies')
    .x('Record')
    .y('Robberies')
    .build()
```

### BarChart

```groovy
def chart = BarChart.builder(data)
    .title('Sales by Region')
    .x('region')
    .y('q1', 'q2', 'q3')
    .horizontal()
    .stacked()
    .build()
```

Additional methods: `chartType(ChartType)`, `direction(ChartDirection)`, `horizontal()`, `vertical()`, `stacked()`.

### LineChart

```groovy
def chart = LineChart.builder(data)
    .title('Trends')
    .x('year')
    .y('sales', 'profit')
    .build()
```

### ScatterChart

```groovy
def chart = ScatterChart.builder(data)
    .title('MPG vs Weight')
    .x('wt')
    .y('mpg')
    .build()
```

### PieChart

```groovy
def chart = PieChart.builder(data)
    .title('Market Share')
    .x('company')
    .y('revenue')
    .build()
```

### BoxChart

With category and value columns:

```groovy
def chart = BoxChart.builder(data)
    .title('Salary Distribution')
    .x('department')
    .y('salary')
    .build()
```

With multiple columns (each column becomes a box):

```groovy
def chart = BoxChart.builder(data)
    .title('Feature Comparison')
    .columns(['mpg', 'hp', 'wt'])
    .build()
```

### Histogram

```groovy
def chart = Histogram.builder(data)
    .title('MPG Distribution')
    .x('mpg')
    .bins(5)
    .binDecimals(2)
    .build()
```

### Common builder methods

All builders share these methods inherited from `Chart.ChartBuilder`:

- `title(String)` — chart title
- `x(String)` — x-axis (category) column name
- `y(String)` — single y-axis (value) column name
- `y(String...)` — multiple y-axis column names
- `xAxisTitle(String)` / `yAxisTitle(String)` — axis labels
- `xAxisScale(AxisScale)` / `yAxisScale(AxisScale)` — axis scales
- `xAxisScale(start, end, step)` / `yAxisScale(start, end, step)` — axis scales from values
- `legend(Legend)` — legend configuration
- `style(Style)` — style configuration

`AxisScale` settings are applied during Charm rendering. `Style.yLabels` can be used for
custom y-axis labels on legacy PICT charts. `Style.css` is injected into Charm-rendered
SVG as a stylesheet.

## Charm API Recipes

The `matrix-charts` module also includes the Charm Grammar-of-Graphics DSL.
Use Charm when you want a Groovy-native layered API without ggplot compatibility wrappers.

### Recipe: Basic Charm Scatter

```groovy
import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = plot(mtcars) {
  mapping {
    x = 'mpg'
    y = 'wt'
    color = 'cyl'
  }
  layers {
    geomPoint().size(2).alpha(0.7)
  }
  labels {
    title = 'MPG vs Weight'
    x = 'Miles per gallon'
    y = 'Weight'
  }
}.build()

chart.writeTo('charm-scatter.svg')
```

### Recipe: Charm Scatter with Trend Line

```groovy
import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = plot(mtcars) {
  mapping {
    x = 'mpg'
    y = 'wt'
    color = 'cyl'
  }
  layers {
    geomPoint().size(2).alpha(0.7)
    geomSmooth().method('lm')
  }
}.build()

chart.writeTo('charm-scatter-smooth.svg')
```

### Recipe: Charm Histogram

```groovy
import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = plot(mtcars) {
  mapping {
    x = 'mpg'
  }
  layers {
    geomHistogram().bins(20).fill('#5F9EA0').alpha(0.8)
  }
  labels {
    title = 'MPG Distribution'
    x = 'Miles per gallon'
    y = 'Count'
  }
}.build()

chart.writeTo('charm-histogram.svg')
```

### Recipe: Charm Segment Arrows

```groovy
import se.alipsa.matrix.charm.ArrowSpec
import static se.alipsa.matrix.charm.Charts.plot

def chart = plot(data) {
  mapping {
    x = 'x'
    y = 'y'
    xend = 'x2'
    yend = 'y2'
  }
  layers {
    geomSegment().arrow(ArrowSpec.end(8, 6)).color('#336699')
  }
}.build()

chart.writeTo('segment-arrows.svg')
```

Use `ArrowSpec.start(...)`, `ArrowSpec.end(...)`, or `ArrowSpec.both(...)` to choose
where arrowheads are rendered. Length and width are SVG user-unit pixels.

### Recipe: PICT Axis Scale and Y Labels

```groovy
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.Style
import se.alipsa.matrix.pict.Plot

def style = new Style()
style.yLabels = ['0': 'Low', '50': 'Target', '100': 'High']

def chart = LineChart.builder(data)
    .title('Capacity')
    .x('month')
    .y('capacity')
    .yAxisScale(0, 100, 50)
    .style(style)
    .build()

Plot.png(chart, new File('capacity.png'))
```

### Recipe: Export a Plot Grid with Dimensions

```groovy
import static se.alipsa.matrix.charm.Charts.plotGrid

def grid = plotGrid([chart1, chart2, chart3, chart4], 2)

grid.writeTo('dashboard.png', 1200, 800)
grid.writeTo('dashboard.pdf', 1200, 800)
```

### Recipe: Export SVG XML to PDF

```groovy
import se.alipsa.matrix.chartexport.ChartToPdf

String svgXml = charmChart.render().toXml()
ChartToPdf.export(svgXml, new File('chart.pdf'))
```

## GGPlot Cookbook

GGPlot recipes are documented in [Matrix GGPlot](matrix-ggplot.md).
For full API reference, see [matrix-ggplot/docs/ggPlot.md](../../matrix-ggplot/docs/ggPlot.md).

---
[Back to index](cookbook.md)
