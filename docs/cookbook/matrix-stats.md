# Matrix Stats

Focused recipes for the `matrix-stats` module.

## Setup Notes

Use the Matrix BOM and add Groovy explicitly. `matrix-stats` publishes Groovy as `compileOnly` and
does not bring Apache Commons Math in as a runtime dependency anymore.

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

## Linear Algebra

Use `Linalg` for inverse, determinant, eigenvalues, SVD, and linear solves.

```groovy
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

println(Linalg.det(source))
println(Linalg.solve(source, [1.0, 0.0]))
println(Linalg.inverse(source).content())
```

## Interpolation

Use `Interpolation.linear(...)` for explicit domains, evenly spaced series, or Matrix/Grid columns.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.interpolation.Interpolation

assert 5.0 == Interpolation.linear([0.0, 2.0, 4.0], [0.0, 10.0, 20.0], 1.0)
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

## Formula Models

Build a model frame first, then dispatch to a named fit method through `FitRegistry`.
When using the Groovy-native closure DSL, `Formula.build { y | x + group }`, keep it
outside `@CompileStatic` callers or wrap it in a `@CompileDynamic` helper because bare
column names use dynamic `propertyMissing` lookup. In that DSL, write intercept removal
as `noIntercept + x` rather than `0 + x`.

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

def fit = FitRegistry.instance().get('lm').fit(frame)
println(fit.predictorNames)
println(fit.rSquared)
```

## Groovy Formula DSL Recipes

Compare the string and Groovy operator forms side by side when building the same model frame:

```groovy
import se.alipsa.matrix.stats.formula.ModelFrame

def stringFrame = ModelFrame.of('y ~ x + group + x:group', data).evaluate()

def dslFrame = ModelFrame.of(data) {
  y | x + group + (x % group)
}.evaluate()
```

Use the fit convenience helpers when you want to go straight from data plus DSL formula to a fitted
model:

```groovy
import static se.alipsa.matrix.stats.regression.FitDsl.lm

def model = lm(data) {
  y | x + group + interaction(x, group)
}
```

For three-way interactions, prefer the explicit helper:

```groovy
def interactionFrame = ModelFrame.of(data) {
  y | interaction(x, group, z)
}.evaluate()
```

Remember the Groovy DSL uses deferred syntax and dynamic lookup:

- `|` instead of `~`
- `noIntercept` instead of `0 +`
- bare column names are resolved through `propertyMissing`, so keep the DSL out of `@CompileStatic`
  callers unless you wrap it in a `@CompileDynamic` helper

Inside `I { ... }`, stick to the arithmetic supported by the string formula subset: numeric
literals, unary minus, `+`, `-`, `*`, `/`, `**`, and supported transform helpers such as `log`,
`sqrt`, and `exp`.

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

## Native Distributions

Use the native distribution classes directly when you need CDF, quantile, or exact probability helpers.

```groovy
import se.alipsa.matrix.stats.distribution.HypergeometricDistribution
import se.alipsa.matrix.stats.distribution.NormalDistribution

def normal = new NormalDistribution(2, 1.5)
println(normal.cumulativeProbability(2))
println(normal.inverseCumulativeProbability(0.975))

def hyper = new HypergeometricDistribution(37, 21, 17)
println(hyper.probability(10))
println(hyper.cumulativeProbability(10))
```

## Numerical Solvers

Use Brent for scalar roots and the simplex solver for equality-form linear programs.

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
println(root.rootValue)

def solution = LinearProgramSolver.minimize([1.0, 2.0], [[1.0, 1.0]], [1.0])
println(solution.pointValues)
println(solution.objectiveValue)
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
