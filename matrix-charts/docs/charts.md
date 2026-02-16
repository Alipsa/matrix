# Charts Guide

A comprehensive guide to using the `se.alipsa.matrix.charts` package for creating data visualizations in Groovy.

## Table of Contents
- [Introduction](#introduction)
- [Quick Start](#quick-start)
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
- Static factory methods for quick chart creation
- Fluent API for configuration (titles, axes, styling)
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

def chart = BarChart.createVertical('Fruit Sales', data, 'product', ChartType.BASIC, 'sales')

// Export to PNG
ChartToPng.export(chart, new File('fruit_sales.png'))

// Or use the Plot convenience class
Plot.png(chart, new File('fruit_sales.png'))
```

### General Workflow

1. **Create** a chart via a static factory method
2. **Configure** titles, axes, and styling via fluent setters
3. **Export** to SVG, PNG, JPEG, JavaFX, or Swing

```groovy
def chart = LineChart.create('Monthly Revenue', data, 'month', 'revenue')
    .setXAxisTitle('Month')
    .setYAxisTitle('Revenue (USD)')

chart.style.plotBackgroundColor = new java.awt.Color(245, 245, 245)

Plot.png(chart, new File('revenue.png'))
```

## Chart Types

### AreaChart

Filled area chart for showing trends and cumulative values.

**Factory Methods:**

```groovy
// From a 2-column Matrix (uses matrix name as title)
AreaChart chart = AreaChart.create(data)

// From Matrix with named columns
AreaChart chart = AreaChart.create('Sales Trend', data, 'month', 'value')

// From lists
AreaChart chart = AreaChart.create('Quarterly Sales',
    ['Q1', 'Q2', 'Q3', 'Q4'],        // categories
    [10, 25, 15, 30])                  // values
```

**Multi-series area chart:**

```groovy
AreaChart chart = AreaChart.create('Revenue Comparison',
    ['Q1', 'Q2', 'Q3', 'Q4'],        // categories
    [10, 25, 15, 30],                  // series 1
    [5, 15, 10, 20])                   // series 2
```

**Example:**

```groovy
def data = Matrix.builder().data(
    month: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    visitors: [1200, 1500, 1800, 1600, 2000, 2200]
).types(String, Integer).build()

def chart = AreaChart.create('Website Visitors', data, 'month', 'visitors')
    .setXAxisTitle('Month')
    .setYAxisTitle('Visitors')

Plot.png(chart, new File('visitors.png'))
```

### BarChart

Vertical or horizontal bar chart with support for stacked and grouped layouts.

**Factory Methods:**

```groovy
// Vertical bar chart (most common)
BarChart chart = BarChart.createVertical('Sales', data, 'category', ChartType.BASIC, 'value')

// Horizontal bar chart
BarChart chart = BarChart.createHorizontal('Sales', data, 'category', ChartType.BASIC, 'value')

// With explicit direction
BarChart chart = BarChart.create('Sales', ChartType.STACKED, data, 'category',
    ChartDirection.VERTICAL, 'q1', 'q2', 'q3')

// From lists
BarChart chart = BarChart.create('Sales', ChartType.BASIC, ChartDirection.VERTICAL,
    ['A', 'B', 'C'],                  // categories
    [10, 20, 30])                      // values
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
| `ChartDirection.VERTICAL` | Bars grow upward |
| `ChartDirection.HORIZONTAL` | Bars grow rightward |

**Example -- stacked bar chart:**

```groovy
def data = Matrix.builder().data(
    region: ['North', 'South', 'East', 'West'],
    q1: [50, 40, 60, 45],
    q2: [55, 42, 65, 50],
    q3: [60, 38, 70, 55]
).types(String, Integer, Integer, Integer).build()

def chart = BarChart.createVertical('Quarterly Revenue by Region', data, 'region',
    ChartType.STACKED, 'q1', 'q2', 'q3')
    .setXAxisTitle('Region')
    .setYAxisTitle('Revenue')

Plot.png(chart, new File('stacked_bars.png'))
```

**Querying bar chart properties:**

```groovy
chart.direction        // ChartDirection.VERTICAL or HORIZONTAL
chart.chartType        // ChartType.BASIC, STACKED, or GROUPED
chart.isStacked()      // true if chartType == ChartType.STACKED
```

### BoxChart

Box-and-whisker plot for showing distributions across categories.

**Factory Methods:**

```groovy
// Category-based: split data by a category column
BoxChart chart = BoxChart.create('Score Distribution', data, 'department', 'score')

// Column-based: treat each named column as a distribution
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

def chart = BoxChart.create('Performance Scores', data, 'department', 'score')
    .setXAxisTitle('Department')
    .setYAxisTitle('Score')

Plot.png(chart, new File('boxplot.png'))
```

### Histogram

Distribution chart showing frequency of values across bins.

**Factory Methods:**

```groovy
// From Matrix column (most common)
Histogram chart = Histogram.create('Score Distribution', data, 'score')

// With custom bin count
Histogram chart = Histogram.create('Score Distribution', data, 'score', 12)

// With custom bin count and decimal precision
Histogram chart = Histogram.create('Measurements', data, 'value', 10, 2)

// From a map of parameters
Histogram chart = Histogram.create(
    title: 'Score Distribution',
    data: data,
    columnName: 'score',
    bins: 8,
    binDecimals: 1
)

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
def chart = Histogram.create('MPG Distribution', mtcars, 'mpg', 8)
    .setXAxisTitle('Miles per Gallon')
    .setYAxisTitle('Frequency')

Plot.png(chart, new File('mpg_histogram.png'))
```

### LineChart

Line chart for showing trends over a continuous or categorical axis.

**Factory Methods:**

```groovy
// From Matrix with title
LineChart chart = LineChart.create('Temperature Trend', data, 'date', 'temperature')

// From Matrix using matrix name as title
LineChart chart = LineChart.create(data, 'date', 'temperature')

// Multi-series line chart
LineChart chart = LineChart.create('Comparison', data, 'month', 'actual', 'forecast')
```

**Example:**

```groovy
def data = Matrix.builder().data(
    month: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    actual: [100, 120, 115, 130, 145, 160],
    forecast: [110, 115, 120, 125, 140, 155]
).types(String, Integer, Integer).build()

def chart = LineChart.create('Sales: Actual vs Forecast', data, 'month', 'actual', 'forecast')
    .setXAxisTitle('Month')
    .setYAxisTitle('Sales')

Plot.png(chart, new File('line_chart.png'))
```

### PieChart

Pie chart for showing proportional distribution.

**Factory Methods:**

```groovy
// From Matrix with title
PieChart chart = PieChart.create('Market Share', data, 'company', 'share')

// From Matrix using matrix name as title
PieChart chart = PieChart.create(data, 'company', 'share')

// From lists
PieChart chart = PieChart.create('Colors',
    ['Red', 'Blue', 'Green'],         // labels
    [40, 35, 25])                      // values
```

**Example:**

```groovy
def data = Matrix.builder().data(
    category: ['Housing', 'Food', 'Transport', 'Entertainment', 'Savings'],
    amount: [1200, 600, 400, 300, 500]
).types(String, Integer).build()

def chart = PieChart.create('Monthly Budget', data, 'category', 'amount')

Plot.png(chart, new File('budget_pie.png'))
```

### ScatterChart

Scatter plot for showing relationships between two numeric variables.

**Factory Methods:**

```groovy
// Only factory method
ScatterChart chart = ScatterChart.create('Height vs Weight', data, 'height', 'weight')
```

**Example:**

```groovy
def data = Matrix.builder().data(
    height: [160, 165, 170, 175, 180, 185, 190],
    weight: [55, 62, 68, 72, 78, 85, 90]
).types(Integer, Integer).build()

def chart = ScatterChart.create('Height vs Weight', data, 'height', 'weight')
    .setXAxisTitle('Height (cm)')
    .setYAxisTitle('Weight (kg)')

Plot.png(chart, new File('scatter.png'))
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

All setters return the chart instance for method chaining:

```groovy
def chart = BarChart.createVertical('Sales', data, 'product', ChartType.BASIC, 'value')
    .setTitle('Updated Title')
    .setXAxisTitle('Product')
    .setYAxisTitle('Units Sold')

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

Area, bar, and line charts support multiple value series. Pass additional column names to the factory method:

```groovy
// Multi-series line chart
def chart = LineChart.create('Metrics', data, 'date', 'cpu', 'memory', 'disk')

// Multi-series bar chart
def chart = BarChart.createVertical('Sales', data, 'region',
    ChartType.GROUPED, 'q1', 'q2', 'q3', 'q4')

// Multi-series area chart from lists
def chart = AreaChart.create('Revenue',
    ['Q1', 'Q2', 'Q3', 'Q4'],         // categories
    [100, 120, 110, 130],              // series 1
    [80, 95, 90, 105],                 // series 2
    [60, 70, 65, 80])                  // series 3
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

### PNG (via Plot)

```groovy
// To file (default 800x600)
Plot.png(chart, new File('chart.png'))

// With custom dimensions
Plot.png(chart, new File('chart.png'), 1024, 768)

// To OutputStream
ByteArrayOutputStream baos = new ByteArrayOutputStream()
Plot.png(chart, baos)
```

### PNG (via chartexport)

```groovy
import se.alipsa.matrix.chartexport.ChartToPng

ChartToPng.export(chart, new File('chart.png'))
```

### SVG (via Plot)

```groovy
Plot.svg(chart, new File('chart.svg'))
```

### Base64 Data URI

```groovy
String dataUri = Plot.base64(chart)
// Returns: "data:image/png;base64,iVBOR..."

// With custom dimensions
String dataUri = Plot.base64(chart, 640, 480)
```

### JavaFX

```groovy
javafx.scene.Node node = Plot.jfx(chart)
// Use node in a JavaFX application
```

### JPEG (via chartexport)

```groovy
import se.alipsa.matrix.chartexport.ChartToJpeg

ChartToJpeg.export(chart, new File('chart.jpg'), 0.9)
```

### Swing (via chartexport)

```groovy
import se.alipsa.matrix.chartexport.ChartToSwing

def panel = ChartToSwing.export(chart)
// Add panel to a Swing container
```

> **Note:** `Plot` is `@Deprecated` but continues to work. For new code, prefer using `ChartToPng`, `ChartToJpeg`, `ChartToJfx`, `ChartToSwing`, or `ChartToImage` from `se.alipsa.matrix.chartexport` directly.

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

- **Charts** (this guide) -- chart-type-first API. Start with `BarChart.createVertical(...)` and configure from there.
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
def lineChart = LineChart.create('Sales Trend', sales, 'month', 'online', 'retail')
    .setXAxisTitle('Month')
    .setYAxisTitle('Units')
    .setValueSeriesNames(['Online', 'Retail'])
ChartToPng.export(lineChart, new File('trend.png'))

// Stacked bar chart for composition
def barChart = BarChart.createVertical('Sales by Channel', sales, 'month',
    ChartType.STACKED, 'online', 'retail')
    .setValueSeriesNames(['Online', 'Retail'])
ChartToPng.export(barChart, new File('composition.png'))
```

### Histogram from a Dataset

```groovy
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.charts.*

def mtcars = Dataset.mtcars()
def chart = Histogram.create('Engine Displacement', mtcars, 'disp', 10)
    .setXAxisTitle('Displacement (cu.in.)')
    .setYAxisTitle('Count')

Plot.png(chart, new File('displacement.png'))
```

### Styled Pie Chart

```groovy
import java.awt.Color

def data = Matrix.builder().data(
    language: ['Java', 'Groovy', 'Kotlin', 'Scala'],
    usage: [45, 25, 20, 10]
).types(String, Integer).build()

def chart = PieChart.create('JVM Language Usage', data, 'language', 'usage')
chart.style.chartBackgroundColor = Color.WHITE
chart.style.legendPosition = 'RIGHT'

Plot.png(chart, new File('languages.png'))
```

### Box Plot Comparison

```groovy
def data = Matrix.builder().data(
    method: ['A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
             'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B'],
    result: [23, 25, 28, 22, 27, 24, 26, 29,
             30, 32, 35, 28, 33, 31, 34, 36]
).types(String, Integer).build()

def chart = BoxChart.create('Method A vs Method B', data, 'method', 'result')
    .setXAxisTitle('Method')
    .setYAxisTitle('Result')

Plot.png(chart, new File('comparison.png'))
```

## Additional Resources

- **[charm.md](charm.md)** -- Charm DSL guide (Grammar of Graphics)
- **[ggPlot.md](ggPlot.md)** -- ggplot2-compatible API guide (54+ geoms)
- **API Documentation** -- [JavaDoc](https://javadoc.io/doc/se.alipsa.matrix/matrix-charts)
