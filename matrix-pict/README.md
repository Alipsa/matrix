# Matrix Pict

Groovy chart-type-first API for matrix data.

Matrix-pict provides a familiar chart-type-first API for creating common visualizations.
You start by choosing a chart type (e.g. `BarChart`, `LineChart`), then supply data and
configure styling. It delegates to the
[Charm](../matrix-charts/docs/charm.md) rendering engine in matrix-charts under the hood.

## Dependencies

### Gradle

```groovy
implementation(platform('se.alipsa.matrix:matrix-bom:2.5.0'))
implementation 'se.alipsa.matrix:matrix-pict'
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-stats'
```

> Note: `matrix-charts` (the Charm rendering engine) is pulled in transitively via `matrix-pict`.

### Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-bom</artifactId>
      <version>2.5.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-pict</artifactId>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-core</artifactId>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-stats</artifactId>
  </dependency>
</dependencies>
```

## Quick Example

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.*

def data = Matrix.builder().data(
    product: ['Apples', 'Bananas', 'Cherries', 'Dates'],
    sales: [120, 85, 200, 150]
).types(String, Integer).build()

def chart = BarChart.builder(data)
    .title('Fruit Sales')
    .x('product')
    .y('sales')
    .build()

Plot.png(chart, new File('fruit_sales.png'))
```

### Custom Y-Axis Labels

```groovy
def chart = LineChart.builder(data)
    .title('Revenue by Month')
    .x('month')
    .y('revenue')
    .yLabels(['10000': '10K', '50000': '50K', '100000': '100K'])
    .build()

Plot.png(chart, new File('revenue.png'))
```

## Chart Types

- **AreaChart** — filled area charts for trends and cumulative values
- **BarChart** — vertical, horizontal, stacked, and grouped bars
- **BoxChart** — box-and-whisker plots for distributions
- **BubbleChart** — scatter with point size encoding a third variable
- **Histogram** — frequency distribution across bins
- **LineChart** — trends over continuous or categorical axes
- **PieChart** — proportional distribution as slices
- **ScatterChart** — relationships between two numeric variables

## Exporting Charts

Use `se.alipsa.matrix.pict.Plot` for convenient export:

```groovy
import se.alipsa.matrix.pict.Plot

Plot.png(chart, new File('chart.png'))
Plot.svg(chart, new File('chart.svg'))
Plot.jfx(chart)              // JavaFX Node
Plot.swing(chart)            // Swing SvgPanel
def dataUri = Plot.base64(chart)
```

The `se.alipsa.matrix.chartexport` package (pulled in transitively from `matrix-charts`)
provides additional formats including JPEG, PDF, and lower-level conversion APIs.

## Documentation

- **[pict.md](docs/pict.md)** — comprehensive API reference and examples

## Release version compatibility matrix

See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions.
