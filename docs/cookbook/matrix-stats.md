# Matrix Stats

Focused recipes for the `matrix-stats` module.

## Correlation

Compute Pearson, Spearman, and Kendall correlations with the same entry point.

```groovy
import static se.alipsa.matrix.stats.Correlation.*

def x = [12, 14, 18, 21, 25]
def y = [8, 11, 13, 19, 24]

println cor(x, y)
println cor(x, y, SPEARMAN)
println cor(x, y, KENDALL)
```

## Normalization

Scale a numeric column before downstream modeling or clustering.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Normalize

Matrix sales = Matrix.builder('sales').data(
  month : ['Jan', 'Feb', 'Mar', 'Apr'],
  amount: [1200, 34567, 3456, 985]
).types(String, Integer).build()

Matrix minMax = Normalize.minMaxNorm(sales)
Matrix zScore = Normalize.stdScaleNorm(sales)

println(minMax['amount'])
println(zScore['amount'])
```

## T-tests

Use Welch for unequal variances and Student for pooled-variance or paired designs.

```groovy
import se.alipsa.matrix.stats.ttest.Student
import se.alipsa.matrix.stats.ttest.Welch

def control = [14, 14, 16, 13, 12, 17, 15, 14]
def treatment = [18, 17, 19, 18, 16, 20, 18, 19]

def welch = Welch.tTest(control, treatment)
def student = Student.tTest(control, treatment, true)
def paired = Student.pairedTTest([88, 80, 93, 94], [85, 78, 92, 91])

println(welch)
println(student)
println(paired)
```

## ANOVA

Compare more than two groups with one-way ANOVA.

```groovy
import se.alipsa.matrix.stats.Anova

def anova = Anova.aov([
  control   : [10, 12, 11, 13, 9],
  treatmentA: [14, 15, 13, 16, 14],
  treatmentB: [8, 9, 7, 10, 8]
])

println("F = ${anova.fValue}, p = ${anova.pValue}")
println(anova.evaluate() ? 'Reject equal means' : 'Fail to reject equal means')
```

## Linear Regression

Fit a simple regression and make predictions.

```groovy
import se.alipsa.matrix.stats.regression.LinearRegression

def x = [2.7, 3, 5, 7, 9, 11, 14]
def y = [4, 5, 7, 10.8, 15, 20, 40]
def model = new LinearRegression(x, y)

println(model.getIntercept(4))
println(model.getSlope(4))
println(model.predict(10, 4))
println(model.getRsquared(4))
```

## Time Series Stationarity Tests

Run the combined unit-root workflow when you want DF, ADF, ADF-GLS, and KPSS in one report.

```groovy
import se.alipsa.matrix.stats.timeseries.UnitRoot

def series = [1.2, 1.5, 1.3, 1.6, 1.4, 1.7, 1.5, 1.8, 1.6, 1.9, 1.7, 2.0]
def result = UnitRoot.test(series, 'drift')

println(result.summary())
println(result.isStationary() ? 'Stationary' : 'Not stationary')
```

If you only want one test, call the individual implementation directly:

```groovy
import se.alipsa.matrix.stats.timeseries.Adf
import se.alipsa.matrix.stats.timeseries.Kpss

println(Adf.test(series))
println(Kpss.test(series, 'level'))
```

## Normality Tests

Use Shapiro-Wilk for a strong default and Jarque-Bera when skewness and kurtosis diagnostics matter.

```groovy
import se.alipsa.matrix.stats.normality.JarqueBera
import se.alipsa.matrix.stats.normality.ShapiroWilk

def sample = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]

def shapiro = ShapiroWilk.test(sample)
def jb = JarqueBera.test(sample)

println(shapiro)
println(jb)
println(shapiro.isNormal() ? 'Looks normal' : 'Non-normal')
```

## Clustering

Normalize features first, then fit a K-means model and append cluster assignments.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.stats.cluster.KMeans

Matrix points = Matrix.builder('points').data(
  x: [1.0, 1.1, 0.9, 5.0, 5.1, 4.9],
  y: [1.0, 0.9, 1.1, 5.0, 5.2, 4.8]
).types(Double, Double).build()

Matrix normalized = Normalize.minMaxNorm(points)
Matrix clustered = new KMeans(normalized).fit(['x', 'y'], 2, 20, 'cluster', false)

println(clustered.content())
```

---
[Back to index](cookbook.md)  |  [Next (Matrix CSV)](matrix-csv.md)
