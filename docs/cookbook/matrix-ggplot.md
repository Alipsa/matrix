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

## References

- [matrix-ggplot README](../../matrix-ggplot/README.md)
- [ggPlot API guide](../../matrix-ggplot/docs/ggPlot.md)

---
[Back to index](cookbook.md)
