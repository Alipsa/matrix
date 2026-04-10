# Matrix Stats Module

`matrix-stats` extends the Matrix project with statistical tools for descriptive analysis,
hypothesis testing, regression, formula/model-frame workflows, linear algebra,
interpolation, native distributions, numerical solvers, time-series diagnostics, and clustering.

## Installation

Use the Matrix BOM so `matrix-core`, `matrix-stats`, and any additional Matrix modules stay aligned.
`matrix-stats` publishes Groovy as `compileOnly`, so you must add Groovy explicitly. The examples
below also use `matrix-datasets` for `Dataset.iris()`.

### Gradle

```groovy
dependencies {
  implementation "org.apache.groovy:groovy:GROOVY_VERSION"
  implementation platform("se.alipsa.matrix:matrix-bom:MATRIX_VERSION")
  implementation "se.alipsa.matrix:matrix-core"
  implementation "se.alipsa.matrix:matrix-stats"
  implementation "se.alipsa.matrix:matrix-datasets"
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
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-datasets</artifactId>
  </dependency>
</dependencies>
```

`matrix-stats` no longer carries Apache Commons Math as a transitive runtime dependency.
`commons-math3` is kept as a test-only dependency to validate the native implementations.
Consumers do not need to add it unless they use Commons Math directly in their own code.

The public linear algebra facade uses EJML internally via an `implementation` dependency.
Consumers do not need to add EJML unless they want to use EJML APIs directly.

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

## Linear Algebra

`matrix-stats` exposes a public dense linear algebra facade in `se.alipsa.matrix.stats.linalg.Linalg`.
The facade accepts `Matrix`, `Grid`, and list-backed numeric vectors, computes internally in
`double` precision, and returns Groovy-friendly `BigDecimal`, `List<BigDecimal>`, `Matrix`,
`Grid<BigDecimal>`, and `SvdResult` values.

```groovy
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.linalg.Linalg

Matrix source = Matrix.builder()
    .columnNames(['x', 'y'])
    .rows([
        [4.0, 7.0],
        [2.0, 6.0]
    ])
    .types([Double, Double])
    .build()

assert [0.6, -0.2] == Linalg.solve(source, [1.0, 0.0])
assert 10.0 == Linalg.det(source)

Matrix inverse = Linalg.inverse(source)
assert ['c0', 'c1'] == inverse.columnNames()

Grid<Number> grid = new Grid<Number>([
    [4.0, 7.0],
    [2.0, 6.0]
])
assert 0.6 == Linalg.inverse(grid)[0, 0]
assert Linalg.svd(grid).singularValues.size() == 2
```

## Interpolation

`matrix-stats` exposes public linear interpolation utilities in
`se.alipsa.matrix.stats.interpolation.Interpolation`.

The current 2.4.0 scope is intentionally narrow:

- linear interpolation only
- idiomatic Groovy numeric inputs with `BigDecimal` scalar results
- explicit `(x, y, targetX)` interpolation as the primitive operation
- convenience overloads for evenly spaced numeric series and Matrix/Grid-backed columns
- no extrapolation outside the supported domain

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.interpolation.Interpolation

assert 5.0 == Interpolation.linear(
    [0.0, 2.0, 4.0],
    [0.0, 10.0, 20.0],
    1.0
)

assert 30.0 == Interpolation.linear([10.0, 20.0, 40.0], 1.5)

Matrix points = Matrix.builder()
    .columnNames(['time', 'value'])
    .rows([
        [1.0, 3.0],
        [2.0, 6.0],
        [4.0, 12.0]
    ])
    .types([Double, Double])
    .build()

assert 9.0 == Interpolation.linear(points, 'time', 'value', 3.0)
```

## Formula Models and Fit Registry

The formula pipeline supports a focused R-style subset for model-frame evaluation and fit-method
dispatch through `FitRegistry`.

Supported formula features include:

- additive terms and intercept control: `y ~ x + z`, `y ~ 0 + x`
- interactions and shorthand expansion: `:`, `*`, `^`, `/`
- quoted identifiers: `` `gross margin` ~ `unit price` ``
- transformed numeric terms: `log(x)`, `sqrt(x)`, `exp(x)`, `I(x + 1)`
- polynomial terms: `poly(x, 3)`
- smooth terms for GAMs: `s(x)` and `s(x, 6)`

The Groovy-native closure DSL, `Formula.build { y | x + group }`, relies on dynamic
`propertyMissing` lookup for bare column names and is not supported inside `@CompileStatic`
callers; use string formulas or a `@CompileDynamic` helper there. In that DSL, write
intercept removal as `noIntercept + x` rather than `0 + x`.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.NaAction
import se.alipsa.matrix.stats.regression.FitRegistry

Matrix data = Matrix.builder()
    .columnNames(['y', 'x', 'group'])
    .rows([
        [1.0, 1.0, 'A'],
        [2.0, 2.0, 'B'],
        [3.0, 3.0, 'A'],
        [4.0, 4.0, 'B']
    ])
    .types([BigDecimal, BigDecimal, String])
    .build()

def frame = ModelFrame.of('y ~ x + group', data)
    .naAction(NaAction.OMIT)
    .evaluate()

def lm = FitRegistry.instance().get('lm').fit(frame)
assert lm.fittedValues.length == data.rowCount()
```

Built-in fit methods through `FitRegistry`:

- `lm` for ordinary least squares
- `loess` for univariate local regression
- `gam` for additive models using `s(...)` smooth terms

Current limitations:

- transformed responses on the left-hand side are not supported
- `na.action` supports only `OMIT` and `FAIL`
- `loess` supports only a single predictor
- `lm`, `loess`, and `gam` reject unsupported frame metadata rather than silently ignoring it
- smooth terms cannot be used inside interactions such as `s(x):z` or `s(x) * z`

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
println(matrixModel.getRsquared(4))
```

## Native Distributions

`matrix-stats` includes native distribution implementations used by the module's runtime and exposed
through Groovy-friendly `Number`/`BigDecimal` APIs.

```groovy
import se.alipsa.matrix.stats.distribution.HypergeometricDistribution
import se.alipsa.matrix.stats.distribution.NormalDistribution

def normal = new NormalDistribution(2, 1.5)
assert 0.5 == normal.cumulativeProbability(2)
assert 2.0 == normal.inverseCumulativeProbability(0.5)

def hyper = new HypergeometricDistribution(37, 21, 17)
assert hyper.supportLowerBound == 1
assert hyper.supportUpperBound == 17
assert hyper.probability(10) > 0
```

## Numerical Solvers

For low-level numerical tasks, `matrix-stats` includes native solver APIs.

```groovy
import se.alipsa.matrix.stats.solver.BrentSolver
import se.alipsa.matrix.stats.solver.LinearProgramSolver
import se.alipsa.matrix.stats.solver.UnivariateObjective

def root = BrentSolver.solve(
    { double x -> x * x - 2.0d } as UnivariateObjective,
    0.0,
    2.0,
    1.0e-12,
    1.0e-12,
    100
)
assert Math.abs((root.rootValue as double) - Math.sqrt(2.0d)) < 1e-10

def solution = LinearProgramSolver.minimize([1.0, 2.0], [[1.0, 1.0]], [1.0])
assert [1.0, 0.0] == solution.pointValues
assert solution.objectiveValue == 1.0
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

## Time Series Stationarity

Use `UnitRoot.test(...)` when you want DF, ADF, ADF-GLS, and KPSS in one report.

```groovy
import se.alipsa.matrix.stats.timeseries.UnitRoot

def series = [1.2, 1.5, 1.3, 1.6, 1.4, 1.7, 1.5, 1.8, 1.6, 1.9, 1.7, 2.0]
def result = UnitRoot.test(series, 'drift')

println(result.summary())
println(result.isStationary() ? 'Stationary' : 'Needs differencing')
```

## Result Objects

Most `matrix-stats` APIs return a small result object rather than raw tuples.

- `Welch.tTest()` returns [TtestResult](/home/per/project/groovy-projects/matrix/matrix-stats/src/main/groovy/se/alipsa/matrix/stats/ttest/TtestResult.groovy) with `tVal`, `pVal`, `df`, `mean1`, `mean2`, `var1`, `var2`, `sd1`, `sd2`, `n1`, `n2`, and `description`.
- `Student.tTest(first, second, true)` returns `Student.Result`, which extends `TtestResult` for the pooled-variance case.
- `Student.tTest(values, comparison)` returns `Student.SingleResult` with `tVal`, `pVal`, `df`, `mean`, `var`, `sd`, `n`, and `description`.
- `Student.pairedTTest()` returns `Student.PairedResult`, which extends the two-sample result and adds `sd` for the standard deviation of paired differences.
- `Anova.aov()` returns `Anova.AnovaResult` with `fValue` and `pValue`.
- `UnitRoot.test()` returns `UnitRoot.UnitRootResult` with DF, ADF, ADF-GLS, and KPSS results plus summary helpers.
- `BrentSolver.solve()` returns `BrentSolver.SolverResult` with `rootValue`, `lowerBoundValue`, `upperBoundValue`, `evaluations`, and `iterations`.
- `LinearProgramSolver.minimize()` returns `LinearProgramSolver.Solution` with `pointValues`, `objectiveValue`, and `iterations`.
- `FitRegistry.instance().get(...).fit(...)` returns `FitResult` with `coefficients`, `standardErrors`, `fittedValues`, `residuals`, `rSquared`, and `predictorNames`.

All of these result types provide a readable `toString()` and helper getters like `getT(int decimals)` or `getP(int decimals)` where that makes sense. For the full API surface, use the GroovyDoc and the source classes above.

## More Examples

For time-series stationarity tests, normality tests, and clustering recipes, see the
[Matrix Stats cookbook](../cookbook/matrix-stats.md).

Go to [previous section](2-matrix-core.md) | Go to [next section](4-matrix-datasets.md) | Back to [outline](outline.md)
