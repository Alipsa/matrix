# Matrix GGPlot

`matrix-ggplot` provides a ggplot2-compatible API on top of the Charm rendering engine.
Use it when you want R-style grammar and migration-friendly syntax.

## Recipe: Quick Scatter with `qplot()`

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = qplot(data: mtcars, x: 'mpg', y: 'wt', color: 'cyl')
ggsave('quick-scatter.svg', chart)
```

## Recipe: Quick Histogram with `qplot()`

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = qplot(data: mtcars, x: 'mpg', bins: 20, title: 'MPG distribution')
ggsave('quick-hist.svg', chart)
```

## Recipe: Closure-based `aes` in a ggplot Pipeline

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def chart = ggplot(mtcars, aes { x = mpg; y = wt; color = cyl }) +
    geom_point(alpha: 0.7) +
    geom_smooth(method: 'lm') +
    labs(title: 'MPG vs Weight', x: 'Miles per gallon', y: 'Weight')

ggsave('closure-aes-pipeline.svg', chart)
```

## Recipe: Validated Column References with `cols()`

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mtcars = Dataset.mtcars()
def c = cols(mtcars)
def chart = ggplot(mtcars, aes(x: c.mpg, y: c.wt, color: c.cyl)) + geom_point()
ggsave('cols-validation.svg', chart)
```

## Recipe: Segment chart with `aes(xend, yend)`

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.core.Matrix

def data = Matrix.builder()
    .columnNames(['x', 'y', 'xend', 'yend'])
    .rows([[1, 1, 2, 3], [2, 2, 3, 4], [3, 3, 4, 2]])
    .build()

def chart = ggplot(data, aes(x: 'x', y: 'y', xend: 'xend', yend: 'yend')) +
    geom_segment(linewidth: 1.5) +
    labs(title: 'Segment endpoints')

ggsave('segment-endpoints.svg', chart)
```

## Recipe: Error bars with `aes(ymin, ymax)`

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.core.Matrix

def data = Matrix.builder()
    .columnNames(['group', 'mean', 'lower', 'upper'])
    .rows([['A', 10, 8, 12], ['B', 14, 11, 17], ['C', 9, 7, 11]])
    .build()

def chart = ggplot(data, aes(x: 'group', y: 'mean', ymin: 'lower', ymax: 'upper')) +
    geom_errorbar(width: 0.25) +
    geom_point(size: 3) +
    labs(title: 'Interval estimate')

ggsave('errorbar-ranges.svg', chart)
```

## Recipe: Separate legend titles per aesthetic

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
    labs(title: 'Independent legend titles', color: 'Source', fill: 'Kind')

ggsave('separate-legend-titles.svg', chart)
```

## Recipe: `stat_summary` with named geom

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.core.Matrix

def data = Matrix.builder()
    .columnNames(['group', 'value'])
    .rows([
        ['A', 1], ['A', 3],
        ['B', 2], ['B', 6],
        ['C', 4], ['C', 8]
    ])
    .build()

def chart = ggplot(data, aes(x: 'group', y: 'value')) +
    stat_summary(geom: 'line', fun: { List<Number> values -> values.average() }) +
    geom_point(alpha: 0.6)

ggsave('stat-summary-line.svg', chart)
```

## References

- [matrix-ggplot README](../../matrix-ggplot/README.md)
- [ggPlot API guide](../../matrix-ggplot/docs/ggPlot.md)

---
[Back to index](cookbook.md)
