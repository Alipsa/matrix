# Matrix Stats Module

`matrix-stats` extends the Matrix project with statistical tools for descriptive analysis,
hypothesis testing, time-series diagnostics, regression, and clustering.

## Installation

Use the Matrix BOM so `matrix-core`, `matrix-stats`, and any additional Matrix modules stay aligned.

### Gradle

```groovy
dependencies {
  implementation platform("se.alipsa.matrix:matrix-bom:MATRIX_VERSION")
  implementation "se.alipsa.matrix:matrix-core"
  implementation "se.alipsa.matrix:matrix-stats"
}
```

### Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-bom</artifactId>
      <version>MATRIX_VERSION</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
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

At the time of writing, `matrix-stats` still carries Apache Commons Math as a transitive runtime
dependency. You do not need to add it manually.

## Correlations

Use `Correlation.cor()` for Pearson, Spearman, and Kendall coefficients.

```groovy
import static se.alipsa.matrix.stats.Correlation.*
import java.math.RoundingMode

def x = [15, 18, 21, 24, 27]
def y = [25, 25, 27, 31, 32]

assert 0.95346258924559 == cor(x, y).setScale(14, RoundingMode.HALF_EVEN)
assert 0.45643546458764 == cor([15, 18, 21, 15, 21], [25, 25, 27, 27, 27], SPEARMAN)
    .setScale(14, RoundingMode.HALF_EVEN)
assert -0.47140452079103173 == cor([12, 2, 1, 12, 2], [1, 4, 7, 1, 0], KENDALL)
```

## Normalization

`Normalize` supports logarithmic, min-max, mean, and standard-deviation scaling.

```groovy
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.stats.Normalize

List<BigDecimal> obs = ListConverter.toBigDecimals([1200, 34567, 3456, 12, 3456, 985, 1211])

def meanNormalized = Normalize.meanNorm(obs, 7)
def minMaxScaled = Normalize.minMaxNorm(obs, 7)
def zScores = Normalize.stdScaleNorm(obs, 7)
def logScaled = Normalize.logNorm(obs, 7)

println(meanNormalized)
println(minMaxScaled)
println(zScores)
println(logScaled)
```

## Linear Regression

`LinearRegression` fits a simple least-squares model with one predictor.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.regression.LinearRegression

def x = [2.7, 3, 5, 7, 9, 11, 14]
def y = [4, 5, 7, 10.8, 15, 20, 40]

def model = new LinearRegression(x, y)
assert -6.23971066 == model.getIntercept(8)
assert 2.81388732 == model.getSlope(8)
assert 30.34082454 == model.predict(13, 8)

Matrix table = Matrix.builder().data(
  x: x,
  y: y
).build()

def matrixModel = new LinearRegression(table, 'x', 'y')
println(matrixModel.getRSquared(4))
```

## T-tests

`Welch` is the recommended two-sample test when equal variance is not guaranteed.
Use `Student` when you explicitly want the pooled-variance or paired formulation.

### Welch two-sample t-test

```groovy
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.ttest.Welch

def iris = Dataset.iris()
def species = iris.columnIndex('Species')
def setosa = iris.subset { it[species] == 'setosa' }
def virginica = iris.subset { it[species] == 'virginica' }

def result = Welch.tTest(setosa['Petal Length'], virginica['Petal Length'])
println(result)
```

### Student two-sample t-test

```groovy
import se.alipsa.matrix.stats.ttest.Student

def control = [14, 14, 16, 13, 12, 17, 15, 14]
def treatment = [18, 17, 19, 18, 16, 20, 18, 19]

def result = Student.tTest(control, treatment, true)
println(result)
```

### Paired t-test

```groovy
import se.alipsa.matrix.stats.ttest.Student

def before = [85, 78, 92, 91, 72, 97]
def after = [88, 80, 93, 94, 80, 100]

def paired = Student.pairedTTest(after, before)
println(paired)
```

## ANOVA

One-way ANOVA is available through `Anova.aov()`.

```groovy
import se.alipsa.matrix.stats.Anova

def result = Anova.aov([
  control   : [10, 12, 11, 13, 9],
  treatmentA: [14, 15, 13, 16, 14],
  treatmentB: [8, 9, 7, 10, 8]
])

println(result)
assert result.evaluate()
```

## More Examples

For time-series stationarity tests, normality tests, and clustering recipes, see the
[Matrix Stats cookbook](../cookbook/matrix-stats.md).

Go to [previous section](2-matrix-core.md) | Go to [next section](4-matrix-datasets.md) | Back to [outline](outline.md)
