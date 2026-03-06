# Matrix GGPlot

Groovy ggplot2-style charting API for matrix data.

Matrix-ggplot provides a compatibility layer mimicking R's ggplot2 API, making it easy to
port R plotting code to Groovy with minimal modifications. It delegates to the
[Charm](../matrix-charts/docs/charm.md) rendering engine in matrix-charts under the hood.

## Dependencies

### Gradle

```groovy
implementation(platform('se.alipsa.matrix:matrix-bom:2.4.1'))
implementation 'se.alipsa.matrix:matrix-ggplot'
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-stats'
```

> Note: `matrix-charts` (the Charm rendering engine) is pulled in transitively via `matrix-ggplot`.

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
    <artifactId>matrix-ggplot</artifactId>
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

## Aesthetic Mapping Styles

```groovy
def mtcars = Dataset.mtcars()
```

Map-based mappings remain fully supported:

```groovy
ggplot(mtcars, aes(x: 'mpg', y: 'wt', color: 'cyl')) + geom_point()
```

Closure-based mappings let you use unquoted column names:

```groovy
ggplot(mtcars, aes { x = mpg; y = wt; color = cyl }) + geom_point()
```

Use quotes for column names with spaces/special characters:

```groovy
ggplot(Dataset.iris(), aes { x = 'Sepal Length'; y = 'Petal Width' }) + geom_point()
```

Wrappers work in both styles, including `I()`, `factor()`, `expr {}`, `after_stat()`, `after_scale()`, and `cut_width()`.

## Quick Plots with `qplot()`

```groovy
// Geom inferred as point (x and y)
qplot(data: mtcars, x: 'mpg', y: 'wt', color: 'cyl')

// Geom inferred as histogram (numeric x only)
qplot(data: mtcars, x: 'mpg', bins: 15)

// Closure-based quick plot
qplot(mtcars) { x = mpg; y = wt; color = cyl }
```

`qplot()` infers an appropriate geom and can be overridden with `geom: 'line'`, `geom: 'bar'`, etc.

## Validated Column References with `cols()`

```groovy
def c = cols(mtcars)
ggplot(mtcars, aes(x: c.mpg, y: c.wt)) + geom_point()   // valid
ggplot(mtcars, aes(x: c.mpgg, y: c.wt)) + geom_point()  // throws IllegalArgumentException
```

`cols()` helps catch column typos early by validating property access against matrix columns.
The `c.mpg` style relies on dynamic property resolution and therefore works in dynamic/untyped
contexts (scripts/tests or `@CompileDynamic`). In `@CompileStatic` code, use quoted column names.

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
- **[gg-charm-parity-matrix.md](docs/gg-charm-parity-matrix.md)** — feature parity tracking

## Release version compatibility matrix

See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions.
