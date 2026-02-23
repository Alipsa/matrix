# Matrix GGCharts

Groovy ggplot2-style charting API for matrix data.

Matrix-ggcharts provides a compatibility layer mimicking R's ggplot2 API, making it easy to
port R plotting code to Groovy with minimal modifications. It delegates to the
[Charm](../matrix-charts/docs/charm.md) rendering engine in matrix-charts under the hood.

## Dependencies

### Gradle

```groovy
implementation(platform('se.alipsa.matrix:matrix-bom:2.4.1'))
implementation 'se.alipsa.matrix:matrix-ggcharts'
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-stats'
```

> Note: `matrix-charts` (the Charm rendering engine) is pulled in transitively via `matrix-ggcharts`.

### Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-bom</artifactId>
      <version>2.4.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-ggcharts</artifactId>
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
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def chart = ggplot(Dataset.mpg(), aes('cty', 'hwy')) +
    geom_point() +
    geom_smooth(method: 'lm') +
    labs(title: 'City vs Highway MPG')

write(chart.render(), new File('my_plot.svg'))
```

## Key Features

- 54+ geoms (geometric objects) including point, line, bar, histogram, boxplot, heatmap, and more
- Complete scale system (color, size, shape, transformations)
- Faceting for multi-panel plots
- Full theming support
- Statistical transformations
- Annotations and labels
- Multiple output formats (SVG, PNG, JPEG, JavaFX, Swing) via `GgExport`

## Exporting Charts

Use `se.alipsa.matrix.gg.export.GgExport` for convenient export:

```groovy
import se.alipsa.matrix.gg.export.GgExport

GgExport.toPng(chart, new File('chart.png'))
GgExport.toJpeg(chart, new File('chart.jpg'), 0.9)
def image = GgExport.toImage(chart)
def panel = GgExport.toSwing(chart)
```

## Documentation

- **[ggPlot.md](docs/ggPlot.md)** — comprehensive API reference and examples
- **[gg-charm-migration.md](docs/gg-charm-migration.md)** — migration plan from gg to Charm
- **[gg-charm-parity-matrix.md](docs/gg-charm-parity-matrix.md)** — feature parity tracking

## Release version compatibility matrix

See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions.
