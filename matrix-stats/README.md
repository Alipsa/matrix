[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-stats/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-stats)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-stats/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-stats)
# matrix-stats

`matrix-stats` adds statistical analysis to Matrix data: correlation, normalization, regression,
hypothesis tests, time-series diagnostics, normality tests, and clustering.

## Using the dependency

The recommended setup is to import the Matrix BOM and then declare only the modules you use.
`matrix-stats` publishes Groovy as `compileOnly`, so consumers must add their preferred Groovy
runtime/compiler dependency explicitly.

Gradle:

```groovy
dependencies {
  implementation "org.apache.groovy:groovy:GROOVY_VERSION"
  implementation platform("se.alipsa.matrix:matrix-bom:MATRIX_VERSION")
  implementation "se.alipsa.matrix:matrix-core"
  implementation "se.alipsa.matrix:matrix-stats"
}
```

Maven:

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
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>GROOVY_VERSION</version>
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

`matrix-stats` no longer brings in Apache Commons Math at runtime. `commons-math3` is kept as a
test-only dependency to validate the native implementations, so consumers do not need to declare it
unless they use that library directly in their own code.

## Correlation

```groovy
import static se.alipsa.matrix.stats.Correlation.*
import java.math.RoundingMode

assert 0.95346258924559 == cor([15, 18, 21, 24, 27], [25, 25, 27, 31, 32])
    .setScale(14, RoundingMode.HALF_EVEN)

assert 0.45643546458764 == cor([15, 18, 21, 15, 21], [25, 25, 27, 27, 27], SPEARMAN)
    .setScale(14, RoundingMode.HALF_EVEN)

assert -0.47140452079103173 == cor([12, 2, 1, 12, 2], [1, 4, 7, 1, 0], KENDALL)
```

## Normalization

```groovy
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.stats.Normalize

List<BigDecimal> values = ListConverter.toBigDecimals([1200, 34567, 3456, 12, 3456, 985, 1211])

assert [-0.1508444, 0.8147756, -0.0855572, -0.1852244, -0.0855572, -0.1570664, -0.1505261] ==
    Normalize.meanNorm(values, 7)
```

## Linear Regression

```groovy
import se.alipsa.matrix.stats.regression.LinearRegression

def x = [2.7, 3, 5, 7, 9, 11, 14]
def y = [4, 5, 7, 10.8, 15, 20, 40]
def model = new LinearRegression(x, y)

assert -6.23971066 == model.getIntercept(8)
assert 2.81388732 == model.getSlope(8)
assert 30.34082454 == model.predict(13, 8)
```

## T-tests

Use `Welch` for the default unequal-variance two-sample test, and `Student` when you explicitly want
the equal-variance or paired formulations.

```groovy
import se.alipsa.matrix.stats.ttest.Student
import se.alipsa.matrix.stats.ttest.Welch

def sample1 = [1.2, 1.4, 1.5, 1.3, 1.6]
def sample2 = [2.1, 1.9, 2.0, 2.2, 2.1]

def welch = Welch.tTest(sample1, sample2)
def student = Student.tTest(sample1, sample2, true)
def paired = Student.pairedTTest([85, 78, 92, 91], [88, 80, 93, 94])

assert welch.pVal < 0.05
assert student.df == sample1.size() + sample2.size() - 2
assert paired.n1 == 4
```

## Time Series Stationarity

```groovy
import se.alipsa.matrix.stats.timeseries.UnitRoot

def series = [1.2, 1.5, 1.3, 1.6, 1.4, 1.7, 1.5, 1.8, 1.6, 1.9, 1.7, 2.0]
def result = UnitRoot.test(series, 'drift')

println(result.summary())
println(result.isStationary() ? 'Stationary' : 'Needs differencing')
```

## Clustering

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.stats.cluster.KMeans

Matrix data = Matrix.builder('points').data(
  x: [1.0, 1.1, 0.9, 5.0, 5.1, 4.9],
  y: [1.0, 0.9, 1.1, 5.0, 5.2, 4.8]
).types(Double, Double).build()

Matrix normalized = Normalize.minMaxNorm(data)
Matrix clustered = new KMeans(normalized).fit(['x', 'y'], 2, 20, 'cluster', false)

assert clustered.columnNames().contains('cluster')
```

## More Documentation

- [Tutorial](../docs/tutorial/3-matrix-stats.md)
- [Cookbook](../docs/cookbook/matrix-stats.md)
- [Correlation tests](https://github.com/Alipsa/matrix/blob/main/matrix-stats/src/test/groovy/CorrelationTest.groovy)
- [Normalization tests](https://github.com/Alipsa/matrix/blob/main/matrix-stats/src/test/groovy/NormalizeTest.groovy)
- [Time series tests](https://github.com/Alipsa/matrix/tree/main/matrix-stats/src/test/groovy/timeseries)
