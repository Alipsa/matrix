# Matrix stats release history

## v2.3.0, In progress
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