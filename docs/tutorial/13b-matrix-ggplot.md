# Matrix GGPlot Module

The `matrix-ggplot` module provides a ggplot2-compatible API for Matrix data.
It delegates rendering to the Charm engine in `matrix-charts`, while preserving a familiar R-style workflow.

## Installation

Add `matrix-ggplot` to your dependencies:

### Gradle

```groovy
implementation platform('se.alipsa.matrix:matrix-bom:2.2.0')
implementation 'se.alipsa.matrix:matrix-ggplot'
implementation 'se.alipsa.matrix:matrix-datasets'
```

## Basic ggplot Example

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = ggplot(mtcars, aes(x: 'mpg', y: 'wt', color: 'cyl')) +
    geom_point() +
    geom_smooth(method: 'lm') +
    labs(title: 'MPG vs Weight')

ggsave('ggplot-basic.svg', chart)
```

## Closure-based `aes`

You can avoid quoting common column names in new code:

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = ggplot(mtcars, aes { x = mpg; y = wt; color = cyl }) +
    geom_point(alpha: 0.7) +
    geom_smooth(method: 'lm')

ggsave('ggplot-closure-aes.svg', chart)
```

## Quick Exploratory Charts with `qplot()`

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()

def scatter = qplot(data: mtcars, x: 'mpg', y: 'wt', color: 'cyl')
def hist = qplot(data: mtcars, x: 'mpg', bins: 20, title: 'MPG distribution')

ggsave('qplot-scatter.svg', scatter)
ggsave('qplot-hist.svg', hist)
```

## Validated Column References with `cols()`

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def c = cols(mtcars)
def chart = ggplot(mtcars, aes(x: c.mpg, y: c.wt, color: c.cyl)) + geom_point()

ggsave('ggplot-cols.svg', chart)
```

## References

- [matrix-ggplot README](../../matrix-ggplot/README.md)
- [matrix-ggplot/docs/ggPlot.md](../../matrix-ggplot/docs/ggPlot.md)
- [matrix-charts/docs/charm.md](../../matrix-charts/docs/charm.md)

Go to [previous section](13-matrix-charts.md) | Go to [next section](14-matrix-tablesaw.md) | Back to [outline](outline.md)
