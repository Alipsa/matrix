# Charts Guide

A comprehensive guide to using the `se.alipsa.matrix.charts` package for creating data visualizations in Groovy.

## Table of Contents
- [Introduction](#introduction)
- [Quick Start](#quick-start)
- [Builder API](#builder-api)
- [Chart Types](#chart-types)
  - [AreaChart](#areachart)
  - [BarChart](#barchart)
  - [BoxChart](#boxchart)
  - [Histogram](#histogram)
  - [LineChart](#linechart)
  - [PieChart](#piechart)
  - [ScatterChart](#scatterchart)
- [Styling](#styling)
- [Axis Configuration](#axis-configuration)
- [Multi-Series Charts](#multi-series-charts)
- [Output Formats](#output-formats)
- [Error Handling](#error-handling)
- [Relationship to Charm and gg APIs](#relationship-to-charm-and-gg-apis)

## Introduction

The `se.alipsa.matrix.charts` package provides a chart-type-first API for creating common visualizations. You start by choosing a chart type (e.g. `BarChart`, `LineChart`), then supply data and configure styling. This is a familiar pattern for users of libraries like xchart or JavaFX charts.

All chart types are backed by the [Charm](charm.md) rendering engine internally. Charts render to SVG and can be exported to PNG, JPEG, JavaFX, or Swing via `se.alipsa.matrix.chartexport`.

### Key Features
- Fluent builder API for creating and configuring charts in one chain
- Static factory methods for quick one-liner chart creation
- Works with Matrix data structures or raw lists
- Multiple output formats (SVG, PNG, JPEG, JavaFX, Swing)
- Multi-series support for area, bar, and line charts

## Quick Start

### Dependencies

```groovy
implementation(platform('se.alipsa.matrix:matrix-bom:2.4.0'))
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-charts'
```

### Creating Your First Chart

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.charts.*
import se.alipsa.matrix.chartexport.ChartToPng

def data = Matrix.builder().data(
    product: ['Apples', 'Bananas', 'Cherries', 'Dates'],
    sales: [120, 85, 200, 150]
).types(String, Integer).build()

def chart = BarChart.builder(data)
    .title('Fruit Sales')
    .x('product')
    .y('sales')
    .build()

// Export to PNG
ChartToPng.export(chart, new File('fruit_sales.png'))
```

### General Workflow

1. **Create** a chart via the fluent builder (or a static factory method)
2. **Configure** title, axes, and chart-specific options in the builder chain
3. **Build** the chart with `.build()`
4. **Export** to SVG, PNG, JPEG, JavaFX, or Swing

```groovy
def chart = LineChart.builder(data)
    .title('Monthly Revenue')
    .x('month')
    .y('revenue')
    .xAxisTitle('Month')
    .yAxisTitle('Revenue (USD)')
    .build()

ChartToPng.export(chart, new File('revenue.png'))
```

## Builder API

Every chart type provides a fluent builder via `ChartType.builder(Matrix data)`. The builder returns the concrete chart after calling `.build()`.

### Common Builder Methods

All builders inherit these methods from `Chart.ChartBuilder`:

| Method | Description |
|---|---|
| `title(String)` | Chart title |
| `x(String)` | X-axis (category) column name |
| `y(String)` | Y-axis (value) column name |
| `y(String...)` | Multiple y-axis column names (multi-series) |
| `xAxisTitle(String)` | X-axis label |
| `yAxisTitle(String)` | Y-axis label |
| `xAxisScale(BigDecimal start, BigDecimal end, BigDecimal step)` | Custom x-axis scale |
| `yAxisScale(BigDecimal start, BigDecimal end, BigDecimal step)` | Custom y-axis scale |
| `xAxisScale(AxisScale)` | X-axis scale from AxisScale object |
| `yAxisScale(AxisScale)` | Y-axis scale from AxisScale object |
| `legend(Legend)` | Legend configuration |
| `style(Style)` | Style configuration |
| `build()` | Build the chart |

### Chart-Specific Builder Methods

| Chart Type | Extra Methods |
|---|---|
| `BarChart.Builder` | `horizontal()`, `vertical()`, `stacked()`, `chartType(ChartType)`, `direction(ChartDirection)` |
| `Histogram.Builder` | `bins(Integer)`, `binDecimals(int)` |
| `BoxChart.Builder` | `columns(List<String>)` |

### Example

```groovy
def chart = BarChart.builder(data)
    .title('Quarterly Revenue')
    .x('region')
    .y('q1', 'q2', 'q3')
    .stacked()
    .xAxisTitle('Region')
    .yAxisTitle('Revenue')
    .yAxisScale(0, 200, 50)
    .build()
```

Static factory methods (e.g. `BarChart.createVertical(...)`, `PieChart.create(...)`) are still available for quick one-liner creation.

## Chart Types

### AreaChart

Filled area chart for showing trends and cumulative values.

**Builder:**

```groovy
AreaChart chart = AreaChart.builder(data)
    .title('Sales Trend')
    .x('month')
    .y('value')
    .build()

// Multi-series
AreaChart chart = AreaChart.builder(data)
    .title('Revenue Comparison')
    .x('month')
    .y('online', 'retail')
    .build()
```

**Factory Methods:**

```groovy
AreaChart chart = AreaChart.create('Sales Trend', data, 'month', 'value')
AreaChart chart = AreaChart.create(data)  // uses matrix name as title

// From lists
AreaChart chart = AreaChart.create('Quarterly Sales',
    ['Q1', 'Q2', 'Q3', 'Q4'], [10, 25, 15, 30])

// Multi-series from lists
AreaChart chart = AreaChart.create('Revenue',
    ['Q1', 'Q2', 'Q3', 'Q4'], [10, 25, 15, 30], [5, 15, 10, 20])
```

**Example:**

```groovy
def data = Matrix.builder().data(
    month: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    visitors: [1200, 1500, 1800, 1600, 2000, 2200]
).types(String, Integer).build()

def chart = AreaChart.builder(data)
    .title('Website Visitors')
    .x('month')
    .y('visitors')
    .xAxisTitle('Month')
    .yAxisTitle('Visitors')
    .build()

ChartToPng.export(chart, new File('visitors.png'))
```

### BarChart

Vertical or horizontal bar chart with support for stacked and grouped layouts.

**Builder:**

```groovy
// Vertical bar chart (default)
BarChart chart = BarChart.builder(data)
    .title('Sales')
    .x('category')
    .y('value')
    .build()

// Horizontal bar chart
BarChart chart = BarChart.builder(data)
    .title('Sales')
    .x('category')
    .y('value')
    .horizontal()
    .build()

// Stacked multi-series
BarChart chart = BarChart.builder(data)
    .title('Revenue')
    .x('region')
    .y('q1', 'q2', 'q3')
    .stacked()
    .build()

// Grouped with explicit chart type
BarChart chart = BarChart.builder(data)
    .title('Revenue')
    .x('region')
    .y('q1', 'q2', 'q3')
    .chartType(ChartType.GROUPED)
    .build()
```

**Factory Methods:**

```groovy
BarChart chart = BarChart.createVertical('Sales', data, 'category', ChartType.BASIC, 'value')
BarChart chart = BarChart.createHorizontal('Sales', data, 'category', ChartType.BASIC, 'value')
BarChart chart = BarChart.create('Sales', ChartType.STACKED, data, 'category',
    ChartDirection.VERTICAL, 'q1', 'q2', 'q3')
```

**ChartType options:**

| Type | Description |
|---|---|
| `ChartType.BASIC` | Standard non-stacked bars (default) |
| `ChartType.STACKED` | Values stacked on top of each other |
| `ChartType.GROUPED` | Values placed side by side |

**ChartDirection options:**

| Direction | Description |
|---|---|
| `ChartDirection.VERTICAL` | Bars grow upward (default) |
| `ChartDirection.HORIZONTAL` | Bars grow rightward |

**Example -- stacked bar chart:**

```groovy
def data = Matrix.builder().data(
    region: ['North', 'South', 'East', 'West'],
    q1: [50, 40, 60, 45],
    q2: [55, 42, 65, 50],
    q3: [60, 38, 70, 55]
).types(String, Integer, Integer, Integer).build()

def chart = BarChart.builder(data)
    .title('Quarterly Revenue by Region')
    .x('region')
    .y('q1', 'q2', 'q3')
    .stacked()
    .xAxisTitle('Region')
    .yAxisTitle('Revenue')
    .build()

ChartToPng.export(chart, new File('stacked_bars.png'))
```

**Querying bar chart properties:**

```groovy
chart.direction        // ChartDirection.VERTICAL or HORIZONTAL
chart.chartType        // ChartType.BASIC, STACKED, or GROUPED
chart.isStacked()      // true if chartType == ChartType.STACKED
```

### BoxChart

Box-and-whisker plot for showing distributions across categories.

**Builder:**

```groovy
// Category/value mode: split data by a category column
BoxChart chart = BoxChart.builder(data)
    .title('Score Distribution')
    .x('department')
    .y('score')
    .build()

// Multi-column mode: each column becomes a separate box
BoxChart chart = BoxChart.builder(data)
    .title('Comparison')
    .columns(['seriesA', 'seriesB', 'seriesC'])
    .build()
```

**Factory Methods:**

```groovy
BoxChart chart = BoxChart.create('Score Distribution', data, 'department', 'score')
BoxChart chart = BoxChart.create('Comparison', data, ['seriesA', 'seriesB', 'seriesC'])
```

**Example:**

```groovy
def data = Matrix.builder().data(
    department: ['Sales', 'Sales', 'Sales', 'Sales', 'Sales',
                 'Engineering', 'Engineering', 'Engineering', 'Engineering', 'Engineering'],
    score: [78, 82, 85, 90, 88,
            92, 88, 95, 91, 87]
).types(String, Integer).build()

def chart = BoxChart.builder(data)
    .title('Performance Scores')
    .x('department')
    .y('score')
    .xAxisTitle('Department')
    .yAxisTitle('Score')
    .build()

ChartToPng.export(chart, new File('boxplot.png'))
```

### Histogram

Distribution chart showing frequency of values across bins.

**Builder:**

```groovy
Histogram chart = Histogram.builder(data)
    .title('Score Distribution')
    .x('score')
    .bins(12)
    .binDecimals(2)
    .build()
```

**Factory Methods:**

```groovy
Histogram chart = Histogram.create('Score Distribution', data, 'score')
Histogram chart = Histogram.create('Score Distribution', data, 'score', 12)
Histogram chart = Histogram.create('Measurements', data, 'value', 10, 2)

// From a map of parameters
Histogram chart = Histogram.create(
    title: 'Score Distribution', data: data,
    columnName: 'score', bins: 8, binDecimals: 1)

// From a raw list of numbers
Histogram chart = Histogram.create([1.2, 2.1, 4.1, 4.3, 5.7, 6.2, 6.9, 8.5, 9.9], 5)
```

**Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `bins` | 9 | Number of bins |
| `binDecimals` | 1 | Decimal precision for bin boundaries |

**Querying histogram properties:**

```groovy
chart.numberOfBins    // Integer
chart.originalData    // List<? extends Number>
chart.ranges          // Map<MinMax, Integer> -- bin range to frequency count
```

**Example:**

```groovy
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = Histogram.builder(mtcars)
    .title('MPG Distribution')
    .x('mpg')
    .bins(8)
    .xAxisTitle('Miles per Gallon')
    .yAxisTitle('Frequency')
    .build()

ChartToPng.export(chart, new File('mpg_histogram.png'))
```

### LineChart

Line chart for showing trends over a continuous or categorical axis.

**Builder:**

```groovy
LineChart chart = LineChart.builder(data)
    .title('Temperature Trend')
    .x('date')
    .y('temperature')
    .build()

// Multi-series
LineChart chart = LineChart.builder(data)
    .title('Comparison')
    .x('month')
    .y('actual', 'forecast')
    .build()
```

**Factory Methods:**

```groovy
LineChart chart = LineChart.create('Temperature Trend', data, 'date', 'temperature')
LineChart chart = LineChart.create(data, 'date', 'temperature')  // uses matrix name as title
LineChart chart = LineChart.create('Comparison', data, 'month', 'actual', 'forecast')
```

**Example:**

```groovy
def data = Matrix.builder().data(
    month: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    actual: [100, 120, 115, 130, 145, 160],
    forecast: [110, 115, 120, 125, 140, 155]
).types(String, Integer, Integer).build()

def chart = LineChart.builder(data)
    .title('Sales: Actual vs Forecast')
    .x('month')
    .y('actual', 'forecast')
    .xAxisTitle('Month')
    .yAxisTitle('Sales')
    .build()

ChartToPng.export(chart, new File('line_chart.png'))
```

### PieChart

Pie chart for showing proportional distribution.

**Builder:**

```groovy
PieChart chart = PieChart.builder(data)
    .title('Market Share')
    .x('company')
    .y('share')
    .build()
```

**Factory Methods:**

```groovy
PieChart chart = PieChart.create('Market Share', data, 'company', 'share')
PieChart chart = PieChart.create(data, 'company', 'share')  // uses matrix name as title

// From lists
PieChart chart = PieChart.create('Colors',
    ['Red', 'Blue', 'Green'], [40, 35, 25])
```

**Example:**

```groovy
def data = Matrix.builder().data(
    category: ['Housing', 'Food', 'Transport', 'Entertainment', 'Savings'],
    amount: [1200, 600, 400, 300, 500]
).types(String, Integer).build()

def chart = PieChart.builder(data)
    .title('Monthly Budget')
    .x('category')
    .y('amount')
    .build()

ChartToPng.export(chart, new File('budget_pie.png'))
```

### ScatterChart

Scatter plot for showing relationships between two numeric variables.

**Builder:**

```groovy
ScatterChart chart = ScatterChart.builder(data)
    .title('Height vs Weight')
    .x('height')
    .y('weight')
    .build()
```

**Factory Methods:**

```groovy
ScatterChart chart = ScatterChart.create('Height vs Weight', data, 'height', 'weight')
```

**Example:**

```groovy
def data = Matrix.builder().data(
    height: [160, 165, 170, 175, 180, 185, 190],
    weight: [55, 62, 68, 72, 78, 85, 90]
).types(Integer, Integer).build()

def chart = ScatterChart.builder(data)
    .title('Height vs Weight')
    .x('height')
    .y('weight')
    .xAxisTitle('Height (cm)')
    .yAxisTitle('Weight (kg)')
    .build()

ChartToPng.export(chart, new File('scatter.png'))
```

## Styling

All chart types share the same `Style` object accessible via `chart.style`.

### Background Colors

```groovy
import java.awt.Color

// Plot area background (the area where data is drawn)
chart.style.plotBackgroundColor = new Color(245, 245, 245)

// Overall chart background (including margins/title area)
chart.style.chartBackgroundColor = Color.WHITE
```

### Legend

```groovy
// Hide the legend
chart.style.legendVisible = false

// Position the legend
chart.style.legendPosition = 'TOP'     // TOP, RIGHT, BOTTOM, LEFT

// Legend font and background
chart.style.legendFont = new java.awt.Font('SansSerif', java.awt.Font.PLAIN, 10)
chart.style.legendBackgroundColor = new Color(250, 250, 250)
```

### Title and Axis Visibility

```groovy
chart.style.titleVisible = false
chart.style.XAxisVisible = false
chart.style.YAxisVisible = false
```

### CSS Styling

```groovy
chart.style.css = 'stroke-width: 2; font-family: Arial;'
```

### Fluent Configuration

Builder methods handle title and axis configuration. For additional styling, access `chart.style` after building:

```groovy
def chart = BarChart.builder(data)
    .title('Sales')
    .x('product')
    .y('value')
    .xAxisTitle('Product')
    .yAxisTitle('Units Sold')
    .build()

chart.style.plotBackgroundColor = new Color(240, 240, 240)
chart.style.legendVisible = false
```

## Axis Configuration

### Custom Axis Scale

Set explicit start, end, and step values for axes:

```groovy
import se.alipsa.matrix.charts.AxisScale

// Using AxisScale object
chart.setXAxisScale(new AxisScale(0, 100, 10))
chart.setYAxisScale(new AxisScale(0, 500, 50))

// Using convenience method with BigDecimal parameters
chart.setXAxisScale(0, 100, 10)
chart.setYAxisScale(0, 500, 50)
```

### Axis Titles

```groovy
chart.setXAxisTitle('Time (seconds)')
chart.setYAxisTitle('Temperature (\u00B0C)')
```

## Multi-Series Charts

Area, bar, and line charts support multiple value series. Pass additional column names to the builder's `y()` method:

```groovy
// Multi-series line chart
def chart = LineChart.builder(data)
    .title('Metrics').x('date').y('cpu', 'memory', 'disk').build()

// Multi-series grouped bar chart
def chart = BarChart.builder(data)
    .title('Sales').x('region').y('q1', 'q2', 'q3', 'q4')
    .chartType(ChartType.GROUPED).build()

// Multi-series area chart from lists (factory method)
def chart = AreaChart.create('Revenue',
    ['Q1', 'Q2', 'Q3', 'Q4'],
    [100, 120, 110, 130], [80, 95, 90, 105], [60, 70, 65, 80])
```

### Custom Series Names

```groovy
chart.setValueSeriesNames(['CPU %', 'Memory %', 'Disk %'])
```

### Accessing Series Data

```groovy
chart.categorySeries        // List -- category labels
chart.valueSeries           // List<List> -- all value series
chart.getValueSerie(0)      // List -- first value series
chart.valueSeriesNames      // List<String> -- series names
```

## Output Formats

All export classes are in the `se.alipsa.matrix.chartexport` package and accept legacy `Chart` objects, Charm `Chart`, `Svg`, or SVG strings.

### PNG

```groovy
import se.alipsa.matrix.chartexport.ChartToPng

// To file
ChartToPng.export(chart, new File('chart.png'))

// To OutputStream
ByteArrayOutputStream baos = new ByteArrayOutputStream()
ChartToPng.export(chart, baos)
```

### SVG

```groovy
import se.alipsa.matrix.chartexport.ChartToSvg

ChartToSvg.export(chart, new File('chart.svg'))

// To OutputStream or Writer
ChartToSvg.export(chart, outputStream)
ChartToSvg.export(chart, writer)
```

### JPEG

```groovy
import se.alipsa.matrix.chartexport.ChartToJpeg

// quality: 0.0 to 1.0 (default 1.0)
ChartToJpeg.export(chart, new File('chart.jpg'), 0.9)

// To OutputStream
ChartToJpeg.export(chart, outputStream, 0.9)
```

### Base64 Data URI

```groovy
import se.alipsa.matrix.chartexport.ChartToPng

String dataUri = ChartToPng.base64(chart)
// Returns: "data:image/png;base64,iVBOR..."
```

### BufferedImage

```groovy
import se.alipsa.matrix.chartexport.ChartToImage

BufferedImage image = ChartToImage.export(chart)
```

### JavaFX

```groovy
import se.alipsa.matrix.chartexport.ChartToJfx

javafx.scene.Node node = ChartToJfx.export(chart)
```

### Swing

```groovy
import se.alipsa.matrix.chartexport.ChartToSwing

def panel = ChartToSwing.export(chart)
// Add panel to a Swing container
```

> **Note:** The `Plot` class is `@Deprecated`. Use the `chartexport` classes above instead.

## Error Handling

### Column Validation

```groovy
// AreaChart.create(data) requires exactly 2 columns
IllegalArgumentException: "Matrix does not contain 2 columns."

// Histogram requires a numeric column
IllegalArgumentException: "Column must be numeric in a histogram"
```

### Series Validation

```groovy
// Empty series
IllegalArgumentException: "The series contains no data"

// Mismatched column types across series
IllegalArgumentException: "Column mismatch in series..."
```

### BubbleChart

`BubbleChart` is deprecated and not yet implemented. Calling `BubbleChart.create(...)` throws `RuntimeException("Not yet implemented")`. Use the [Charm](charm.md) or [gg](ggPlot.md) API with a point geom and size aesthetic for bubble-style plots.

## Relationship to Charm and gg APIs

All three APIs in matrix-charts share the same Charm rendering engine:

```
charm ----renders----> Svg (gsvg)
gg ------adapts------> charm ---renders-> Svg
charts --builds------> charm ---renders-> Svg (via CharmBridge)
         Svg ----exports----> chartexport ------> PNG/JPEG/JFX/Swing
```

- **Charts** (this guide) -- chart-type-first API. Start with `BarChart.builder(data)` and configure from there.
- **[Charm](charm.md)** -- core Grammar of Graphics DSL. More expressive, supports closures, scales, faceting, annotations, and custom themes.
- **[gg](ggPlot.md)** -- ggplot2-compatible wrapper. Best for porting R code or when you prefer `ggplot() + geom_point()` syntax.

### When to Use Which

| Use case | Recommended API |
|---|---|
| Quick bar/pie/line chart with minimal configuration | **Charts** |
| Full Grammar of Graphics with faceting, annotations, and custom scales | **Charm** |
| Porting R ggplot2 code to Groovy | **gg** |
| `@CompileStatic` chart code | **Charm** (programmatic API) |
| Multi-layer plots (e.g. points + regression line) | **Charm** or **gg** |

## Examples

### Dashboard-Style Charts

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.charts.*
import se.alipsa.matrix.chartexport.ChartToPng

def sales = Matrix.builder().data(
    month: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    online: [45, 52, 48, 61, 55, 67],
    retail: [30, 28, 35, 32, 40, 38]
).types(String, Integer, Integer).build()

// Line chart for trends
def lineChart = LineChart.builder(sales)
    .title('Sales Trend')
    .x('month')
    .y('online', 'retail')
    .xAxisTitle('Month')
    .yAxisTitle('Units')
    .build()
lineChart.setValueSeriesNames(['Online', 'Retail'])
ChartToPng.export(lineChart, new File('trend.png'))

// Stacked bar chart for composition
def barChart = BarChart.builder(sales)
    .title('Sales by Channel')
    .x('month')
    .y('online', 'retail')
    .stacked()
    .build()
barChart.setValueSeriesNames(['Online', 'Retail'])
ChartToPng.export(barChart, new File('composition.png'))
```

### Histogram from a Dataset

```groovy
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.charts.*
import se.alipsa.matrix.chartexport.ChartToPng

def mtcars = Dataset.mtcars()
def chart = Histogram.builder(mtcars)
    .title('Engine Displacement')
    .x('disp')
    .bins(10)
    .xAxisTitle('Displacement (cu.in.)')
    .yAxisTitle('Count')
    .build()

ChartToPng.export(chart, new File('displacement.png'))
```

### Styled Pie Chart

```groovy
import java.awt.Color

def data = Matrix.builder().data(
    language: ['Java', 'Groovy', 'Kotlin', 'Scala'],
    usage: [45, 25, 20, 10]
).types(String, Integer).build()

def chart = PieChart.builder(data)
    .title('JVM Language Usage')
    .x('language')
    .y('usage')
    .build()
chart.style.chartBackgroundColor = Color.WHITE
chart.style.legendPosition = 'RIGHT'

ChartToPng.export(chart, new File('languages.png'))
```

### Box Plot Comparison

```groovy
def data = Matrix.builder().data(
    method: ['A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
             'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B'],
    result: [23, 25, 28, 22, 27, 24, 26, 29,
             30, 32, 35, 28, 33, 31, 34, 36]
).types(String, Integer).build()

def chart = BoxChart.builder(data)
    .title('Method A vs Method B')
    .x('method')
    .y('result')
    .xAxisTitle('Method')
    .yAxisTitle('Result')
    .build()

ChartToPng.export(chart, new File('comparison.png'))
```

## Additional Resources

- **[charm.md](charm.md)** -- Charm DSL guide (Grammar of Graphics)
- **[ggPlot.md](ggPlot.md)** -- ggplot2-compatible API guide (54+ geoms)
- **API Documentation** -- [JavaDoc](https://javadoc.io/doc/se.alipsa.matrix/matrix-charts)
