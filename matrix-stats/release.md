# Matrix stats release history

## v2.4.0, 2026-04-23
**Native runtime cleanup and idiomatic Groovy API expansion**

### Runtime and Dependency Changes
- Remove Apache Commons Math from the runtime dependency surface. `commons-math3` is now test-only and used to validate native implementations.
- Add EJML Simple as an internal `implementation` dependency for the public linear algebra facade.
- Configure `matrix-stats` production Groovy compilation through the shared compile-static Gradle script.
- Make CodeNarc fail the build instead of reporting non-blocking findings.

### New Public APIs

#### Linear Algebra
- Add `se.alipsa.matrix.stats.linalg.Linalg` for dense matrix operations:
  inverse, determinant, linear solves, real eigenvalues, and SVD.
- Add Matrix and Grid adapters with Groovy-facing return types:
  `Matrix`, `Grid<BigDecimal>`, `BigDecimal`, `List<BigDecimal>`, and `SvdResult`.
- Add `LinalgSingularMatrixException` and shared adapter utilities for singular and invalid matrix input handling.
- Add compatibility classes under `se.alipsa.matrix.stats.linear` for native matrix algebra support.

#### Linear Interpolation
- Add `se.alipsa.matrix.stats.interpolation.Interpolation` for public linear interpolation.
- Support explicit `(x, y, targetX)` interpolation, evenly spaced series interpolation, and Matrix/Grid-backed column interpolation.
- Reject extrapolation, unsorted domains, duplicate domain values, length mismatches, ragged grids, and non-numeric columns.
- Keep spline logic internal to formula/GAM smooth-term support; public spline interpolation is not part of the 2.4.0 API.

#### Formula and Model-Frame Pipeline
- Add R-style formula parsing and normalization support, including additive terms, intercept control, interactions, shorthand expansion, quoted identifiers, numeric transformations, `poly(...)`, and `s(...)` smooth terms.
- Add `ModelFrame` and `ModelFrameResult` for design-matrix construction from Matrix data.
- Add a Groovy-native operator DSL for formulas using `|`, `noIntercept`, `interaction(...)`, `smooth(...)`, and `I { ... }`.
- Add categorical treatment encoding, dot expansion, subset support, NA handling, weights, offsets, and external environment variable resolution.
- Add `NaAction` and formula metadata classes to make downstream model fitting explicit.
- Reject unsupported response forms, smooth-term interactions, and unsupported frame metadata instead of silently ignoring them.

#### Fit Registry and Formula-Based Regression
- Add `FitRegistry`, `FitMethod`, `FitOptions`, and `FitResult` for named fit-method dispatch.
- Add built-in `lm`, `loess`, and `gam` fit methods.
- Add `FitDsl` convenience entry points so Groovy callers can write `lm(data) { y | x + group }`, `loess(data) { y | x }`, and `gam(data) { y | smooth(time, 6) + group }`.
- Add `MultipleLinearRegression`, `LmMethod`, `LoessMethod`, and `GamMethod`.
- Add `LoessOptions` and `GamOptions` with Groovy-facing numeric option surfaces.

#### Native Distributions
- Add native `NormalDistribution`, `ChiSquaredDistribution`, and `HypergeometricDistribution`.
- Extend `TDistribution`, `FDistribution`, and `SpecialFunctions` to remove Commons Math runtime usage.
- Add Groovy-facing `Number` overloads returning `BigDecimal` for public scalar distribution APIs.
- Add typed entry points for F-distribution ANOVA helpers to avoid unsafe generic overload dispatch.

#### Native Solvers and Optimization
- Replace Commons Math optimizer/solver runtime usage with native implementations.
- Add `BrentSolver` for one-dimensional bracketing root finding.
- Update `GoalSeek` to use the native Brent-Dekker solver.
- Add `NelderMeadOptimizer` for derivative-free multivariate minimization.
- Add `LinearProgramSolver` for equality-form linear programs with non-negative variables.
- Add `UnivariateObjective` and `MultivariateObjective` interfaces with Groovy-facing numeric bridges.

### Idiomatic Groovy Numeric API Cleanup
- Move public scalar result values toward `BigDecimal` and public numeric inputs toward `Number`.
- Add `NumericConversion` to centralize finite-value checks, BigDecimal conversion, array/list conversion, alpha validation, and exact integer validation.
- Add `StatUtils` and `LeastSquaresKernel` for small internal double-precision kernels where performance or algorithm constraints justify primitive arrays.
- Replace duplicate numeric coercion and conversion logic across regression, interpolation, distributions, time-series, KDE, and solver code.
- Add typed compatibility entry points for `Johansen` and F-distribution list/array APIs where JVM erasure makes same-name generic overloads unsafe.
- Deprecate primitive/double-style identity accessors in selected result classes where Groovy-facing properties are now preferred.

### Time-Series and Statistical Robustness
- Extract shared time-series utility logic into `TimeSeriesUtils`.
- Improve singular-matrix handling and error messages in time-series code.
- Harden Johansen critical-value handling for unsupported variable counts.
- Improve CCM library-size validation by rejecting non-integral numeric inputs instead of truncating silently.
- Update ANOVA and contingency result APIs to accept `Number` alpha inputs.

### Tests and Quality
- Add coverage for formula parsing, model-frame construction, design matrices, spline basis expansion, fit registry, `lm`, `loess`, `gam`, multiple linear regression, interpolation, linalg, SVD, native distributions, native solvers, numeric conversion, and least-squares kernels.
- Add direct unit tests for `GroupEstimator.estimateNumberOfGroups` and `estimateKByElbow`, covering both the `double[][]` and Groovy-facing `List` overloads, custom `maxK`/`iterations`, and error cases (too few points, too few distinct points).
- Add benchmark-oriented tests for selected Groovy-facing paths versus retained primitive kernels.
- Update tests for BigDecimal/Groovy-friendly assertions and numeric API behavior.
- Increase coverage around null handling, weights, offsets, subset filtering, categorical encoding, exact integer validation, singular matrices, and solver convergence.

### Documentation
- Refresh `matrix-stats` README, tutorial, and cookbook docs for the 2.4.0 API surface.
- Document the Commons Math runtime removal and EJML implementation detail.
- Add examples for `Linalg`, `Interpolation`, formula/model-frame fitting, the Groovy formula DSL, fit convenience helpers, native distributions, and native solvers.
- Refresh broader tutorial setup snippets to use the current BOM and Groovy versions.

## v2.3.0, 2026-01-30
**Major expansion with 26+ new statistical tests and comprehensive refactoring**

### New Statistical Tests

#### Contingency Table Tests (6 tests)
- **Fisher's Exact Test**: exact p-values for 2×2 contingency tables
- **Chi-Squared Test**: Pearson, G-test, and Yates' continuity correction
- **Cochran-Armitage Test**: trend test for ordered categorical data (280 lines, 17 tests)
- **Cochran-Mantel-Haenszel Test**: stratified 2×2 tables (255 lines, 17 tests)
- **Barnard's Test**: unconditional exact test (309 lines, 19 tests)
- **Boschloo's Test**: exact unconditional test (311 lines, 21 tests)

#### Normality Tests (8 tests)
- **Shapiro-Wilk Test**: gold standard normality test
- **Shapiro-Francia Test**: variant for large samples (261 lines, 22 tests)
- **Kolmogorov-Smirnov Test**: one-sample and two-sample tests
- **Lilliefors Test**: K-S test with estimated parameters (211 lines, 9 tests)
- **Jarque-Bera Test**: skewness and kurtosis based
- **Anderson-Darling Test**: weighted K-S variant (194 lines, 8 tests)
- **Cramér-von Mises Test**: goodness-of-fit (254 lines, 18 tests)
- **D'Agostino's K² Test**: omnibus normality (248 lines, 19 tests)

#### Time Series Tests (12 tests)
- **Augmented Dickey-Fuller (ADF)**: unit root test (314 lines, 12 tests)
- **ADF-GLS Test**: efficient ADF variant (474 lines, 19 tests)
- **Dickey-Fuller Test**: original unit root test (393 lines, 16 tests)
- **KPSS Test**: stationarity test (243 lines, 12 tests)
- **Durbin-Watson Test**: autocorrelation detection (170 lines, 11 tests)
- **Ljung-Box Test**: autocorrelation in residuals (Portmanteau: 244 lines, 16 tests)
- **Chow Test**: structural breaks (329 lines, 16 tests)
- **Granger Causality Test**: time series causation (366 lines, 15 tests)
- **Johansen Test**: cointegration test (300 lines, 15 tests)
- **Convergent Cross Mapping (CCM)**: nonlinear causality (399 lines, 19 tests)
- **Turning Point Test**: randomness test (193 lines, 21 tests)
- **Unit Root Test**: stationarity testing (289 lines, 20 tests)

#### Regression Enhancements
- **Quantile Regression**: Barrodale-Roberts simplex algorithm (408 lines, 18 tests)
- **Polynomial Regression**: using commons-math3 for polynomial fitting (177 lines)
- **Logistic Regression**: binary classification (443 lines, 12 tests)
- **Decision Tree**: complete implementation (691 lines, 29 tests)
- **RegressionUtils**: diagnostics and standard error calculations

#### Kernel Density Estimation
- **KernelDensity**: Gaussian, Epanechnikov, uniform, triangular kernels (466 lines)
- **BandwidthSelector**: Silverman's rule of thumb, Scott's rule (131 lines)
- **Kernel**: factory for all kernel types (191 lines)

#### Statistical Distributions
- **TDistribution**: native implementation with CDF, p-values (150 lines, 17 tests)
- **FDistribution**: F-distribution for ANOVA (153 lines, 17 tests)
- **SpecialFunctions**: incomplete beta, log gamma (155 lines, 14 tests, 1e-10 accuracy)
- **Ellipse**: confidence ellipses for bivariate normal data (126 lines, 13 tests)

### Code Quality Improvements

#### BigDecimal Consistency
- refactored Student.groovy to use BigDecimal.sqrt() instead of Math.sqrt() (eliminated precision loss)
- refactored Correlation.groovy to use BigDecimal throughout (corPearson, corSpearman, corKendall)
- migrated most statistical calculations to BigDecimal (still some usage of commons math though)

#### DRY Refactoring
- refactored Normalize.groovy: consolidated 4 functions × 6-7 overloads from 800+ lines to ~350 lines
- extracted common normalization logic for logNorm, minMaxNorm, meanNorm, stdScaleNorm variants
- simplified Result class getters across Student.groovy

#### Performance & Type Safety
- add @CompileStatic to all 51+ classes for type safety and performance
- optimized KMeansPlusPlus: replaced O(m) linear search with O(log m) binary search

### Bug Fixes
- **Critical**: fix incorrect corSpearman implementation (was calculating Pearson, not Spearman)
- fix ChowTest flaky test by using seeded random (was non-deterministic)
- fix division by zero risk in GgStat quantile method
- increase numerical accuracy of distribution tests from 1e-3 to 1e-10 (Apache Commons Math level)

### API Enhancements
- add comprehensive input validation to all statistical tests
- add missing Randomize and Sampler utility classes
- add GroupEstimator for cluster analysis
- add Accuracy enhancements for model evaluation

### Test Coverage
- increased from 14 test files (~27% coverage) to comprehensive test suite
- 450+ total tests across all packages (100% package coverage)

### Documentation
- enhanced documentation for all new statistical tests
- fixed MAE/RMSE documentation in Accuracy.groovy
- added usage examples for all kernel density methods
- added comprehensive GroovyDoc for all new classes

### Dependencies
- add org.apache.commons:commons-math3 for polynomial regression and special functions

## v2.2.0, 2025-06-02
- Ensure that all methods in Normalize can handle all zeroes as input
- Add KMeans clustering
  Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-stats/2.2.0/matrix-stats-2.2.0.jar)

## v2.1.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-stats/2.1.0/matrix-stats-2.1.0.jar)
- add temp implementation of one way anova using commons math.

## v2.0.0, 2025-03-12
- Require JDK 21
- overload Normalize methods with a way to normalize an entire matrix

## v1.1.0, 2025-01-06
- adapt to Matrix 2.2.0

## v1.0.2, 2024-10-31
- adapt to matrix 2.0.0
- Implement GoalSeek

## v1.0.1, 2024-07-04
- Implement Accuracy.evaluatePredictions
- Add DecisionTree and Randomize to support LineCharts

## v1.0.0, 2023-08-06
- initial version able to handle correlations,
- normalization, linear regression, and t-tests
