# Matrix GGPlot Module

The `matrix-ggplot` module provides a ggplot2-compatible API for Matrix data.
It delegates rendering to the Charm engine in `matrix-charts`, while preserving a familiar R-style workflow.

## Installation

Add `matrix-ggplot` to your dependencies:

### Gradle

```groovy
implementation platform('se.alipsa.matrix:matrix-bom:2.5.1')
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

## Positional Range Aesthetics

Use endpoint and range aesthetics in `aes()` when a geom needs more than `x` and `y`.
`xend` and `yend` define segment endpoints. `ymin` and `ymax` define vertical ranges for
error bars and ribbons, while `xmin` and `xmax` define horizontal ranges for rectangular geoms.

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.core.Matrix

def data = Matrix.builder()
    .columnNames(['x', 'y', 'xend', 'yend', 'lower', 'upper'])
    .rows([
        [1, 10, 2, 13, 8, 12],
        [2, 14, 3, 16, 11, 17],
        [3, 9, 4, 11, 7, 11]
    ])
    .build()

def chart = ggplot(data, aes(x: 'x', y: 'y')) +
    geom_segment(mapping: aes(xend: 'xend', yend: 'yend'), linewidth: 1.2) +
    geom_errorbar(mapping: aes(ymin: 'lower', ymax: 'upper'), width: 0.2) +
    geom_point(size: 3)

ggsave('ggplot-positional-ranges.svg', chart)
```

## Labels and Legend Titles

`labs()` can set chart labels and independent legend titles for each mapped aesthetic.

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.core.Matrix

def data = Matrix.builder()
    .columnNames(['category', 'value', 'kind', 'source'])
    .rows([
        ['A', 10, 'baseline', 'observed'],
        ['B', 14, 'target', 'model'],
        ['C', 9, 'baseline', 'observed']
    ])
    .build()

def chart = ggplot(data, aes(x: 'category', y: 'value')) +
    geom_col(aes(fill: 'kind')) +
    geom_point(mapping: aes(color: 'source'), size: 4) +
    labs(
        title: 'Grouped results',
        x: 'Category',
        y: 'Value',
        color: 'Source',
        fill: 'Kind'
    )

ggsave('ggplot-labels-and-legends.svg', chart)
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
