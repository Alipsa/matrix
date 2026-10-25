# Matrix-stats 3.0 usage and migration

Use `se.alipsa.matrix:matrix-stats:3.0.0` with Java 21. Add that dependency to your Gradle build,
then run these Groovy examples with the project's classpath. During development use `3.0.0-SNAPSHOT`.
R is only used to regenerate reference fixtures, never by the library or tests.

## Fractional normalization

```groovy
import se.alipsa.matrix.stats.Normalize
Number half = Normalize.minMaxNorm(5, 0, 10)
assert half == 0.5G
assert half instanceof BigDecimal
assert Normalize.stdScaleNorm([7]) == [null]
```

The scalar log, min-max, mean, and standard-score methods return `Number`. Integral and BigDecimal
inputs produce BigDecimal; Float and Double inputs preserve their type. Non-finite inputs or
summary parameters in min-max, mean, and standard-score scaling produce missing values. Optional decimal places
use half-even rounding; leaving them out avoids requested rounding. Missing/undefined results
use `MissingValueType.NULL` by default; min-max and mean scaling also accept `FLOAT_NAN` or
`DOUBLE_NAN`. Min/max, mean and standard deviation describe the original sample in scalar calls.

## Exact contingency inference

```groovy
import se.alipsa.matrix.stats.contingency.Fisher

def result = Fisher.test([[3, 1], [1, 3]], 'two.sided', 0.90)
assert (result.oddsRatio - 6.4083196582G).abs() < 1e-8G
println result.confidenceInterval
println result.sampleOddsRatio
```

`table` is a 2×2 list or Matrix of non-negative integer counts. `alternative` defaults to
`two.sided`; `less` and `greater` give directional tests and intervals. `confidenceLevel`
defaults to 0.95 and must be strictly between zero and one. `oddsRatio` is the conditional
maximum-likelihood estimate, and `confidenceInterval` is exact. Boundary estimates/endpoints
can be `Double.POSITIVE_INFINITY`, so use `Number` for these values. Assigning an infinite
estimate or endpoint to `BigDecimal` throws `NumberFormatException` even when the assignment
compiles. Check `Double.isFinite(value.doubleValue())` before a BigDecimal conversion. `sampleOddsRatio` retains
the sample cross-product estimate, including the existing zero-cell correction.

## Reproducible Euclidean clustering

```groovy
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.cluster.KMeans
import se.alipsa.matrix.stats.cluster.KMeansPlusPlus

def iris = Dataset.iris()
def clustered = new KMeans(iris).fit(iris.columnNames().take(4), 3, 50, 'cluster', false, 42L)
assert clustered.rowCount() == 150

def medianClusters = new KMeansPlusPlus.Builder(2, [[0d], [1d], [10d]] as double[][])
    .useL1norm(true).randomSeed(42L).maxIterationsPerRun(300).build()
```

The facade takes feature columns, cluster count, restart count, output column (`Group`),
mutation flag (`true`), and optional random seed (`null`, unseeded). The automatic-count
facade defaults to 30 restarts and elbow estimation. The builder defaults to 50 restarts,
Euclidean distance, and 300 updates per restart. L1 mode uses coordinate medians and reports
absolute-distance cost through `WCSS`; Euclidean mode reports squared-distance cost.

## Ellipse geometry and density

```groovy
import se.alipsa.matrix.stats.Ellipse
import se.alipsa.matrix.stats.kde.KernelDensity
import se.alipsa.matrix.stats.kde.Kernel

def ellipse = Ellipse.calculate([1,2,3,4,5], [2,5,4,8,7], 0.95, 'norm', 51)
assert ellipse.x.size() == 51
def density = new KernelDensity([1,2,3,4,5], [kernel: Kernel.EPANECHNIKOV, bandwidth: 0.8, adjust: 1])
println density.toMatrix()
```

Ellipse arguments are x/y coordinates, level (default 0.95, strictly between zero and one),
type (default `t`), and segments (default 51, at least three). Both coordinate lists must
have the same size and at least four observations. `t` uses robust covariance, `norm` uses
classical sample covariance, and `euclid` draws a circle with radius equal to level.
Statistical ellipses use a finite-sample F radius. Chart statistics skip undersized groups.

KDE needs at least two observations. Kernel defaults to Gaussian; `bandwidth` defaults to
Silverman's selector, `adjust` defaults to 1 and multiplies bandwidth, and `n` defaults to
512 output points. Both bandwidth and adjustment must be finite and positive. Scott's
selector matches R `bw.nrd`, including the IQR cap; all kernels have unit variance, so compact
kernel support differs from older releases.

## Known-distribution KS and fitted models

```groovy
import se.alipsa.matrix.stats.normality.KolmogorovSmirnov
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.core.Matrix

def ks = KolmogorovSmirnov.testStandardNormality([-1.1,-0.4,0.2,0.7,1.3], null, 'two.sided')
println ks.pValue

def data = Matrix.builder().columns([y:[1d,3d,5d,7d], x:[0d,1d,2d,3d], z:[1d,0d,1d,0d]])
    .types(Double, Double, Double).build()
def frame = ModelFrame.of('y ~ z + x', data).evaluate()
println frame.predictorNames
```

KS `exact` defaults to null: untied samples below 100 use exact inference; other samples
use asymptotics. Pass true/false to override. `alternative` defaults to `two.sided`, with
`less`/`greater` available. Estimated-normal KS uses sample standard deviation but remains
conservative; use Lilliefors for fitted-normal inference.

Formula terms now follow interaction degree and first appearance; use coefficient names
instead of assuming alphabetical positions. NaN is missing under the selected NA policy,
including weights and offsets. Linear-model aliases are NaN in primitive arrays and null
in list accessors. Polynomial regression and confidence bands use a centered/scaled basis.
Loess defaults to degree 2 and span 0.75, accepts spans in (0,1], and computes the direct
surface; its neighbourhood size is floor(n·span). GAM smooths use an identity ridge penalty,
and standard errors do not account for smoothing effective degrees of freedom.

Unit-root critical values and common-sample MAIC/AIC lag selection changed; automatic lag
choices and conclusions can therefore change. Pass an explicit lag when comparing historical
results. Durbin-Watson's alternative overload is deprecated because it computes no p-value.
