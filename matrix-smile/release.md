# Matrix-smile release history

## v0.2.0, 2026-05-23
Bug fixes and API improvements.

### Bug Fixes
- **tTestTwoSample**: `equalVariance` parameter is now properly wired to Smile's pooled-variance t-test instead of being ignored
- **tTestTwoSample(Matrix, ...)**: `equalVariance` parameter was missing from the Matrix overload; it now accepts `equalVariance` (default `false`) matching the array overload
- **MinMaxScaler**: Added missing `transform()` method for fit-then-transform workflows; calling `transform()` before `fit()` now throws `IllegalStateException`
- **SmileClassifier.randomForest**: `ntrees` parameter was silently ignored — the model always used Smile's defaults; now correctly passed via `RandomForest.Options(ntrees)`
- **SmileClassifier.randomForest**: `ntrees <= 0` now throws `IllegalArgumentException` instead of silently producing an invalid model

### API Improvements
- **CorrelationMethod enum**: Added `PEARSON`, `SPEARMAN`, `KENDALL` enum for type-safe correlation method selection
- **Correlation convenience overloads**: `correlationMatrix(matrix, CorrelationMethod)`, `pValueMatrix(matrix, CorrelationMethod)`, and `correlationWithSignificance(matrix, CorrelationMethod)` — no need to pass `null` for columns when selecting a method
- **String overloads**: `correlationMatrix`, `pValueMatrix`, and `correlationWithSignificance` retain backward-compatible `String` overloads (`"pearson"`, `"spearman"`, `"kendall"`) that delegate to the enum
- **powerTransform**: Parameter changed from `double` to `Number` for better Groovy ergonomics; passing `null` now throws `IllegalArgumentException`
- **SmileClassifier.getModel()**: New getter exposes the underlying Smile `DataFrameClassifier` model

### Documentation
- **README**: Updated dependency versions (groovy 5.0.6, matrix-core 3.7.1, matrix-smile 0.2.0)
- **release.md**: Corrected v0.1.0 feature list to reflect only shipped functionality

---

## v0.1.0, 2026-01-31
Initial release providing integration between Matrix and Smile (Statistical Machine Intelligence and Learning Engine).

### Core Features
- **DataframeConverter**: Bidirectional conversion between Matrix and Smile DataFrame with support for 18 data types
- **SmileUtil**: Pandas-like utility functions for data exploration and manipulation
  - Statistical summary (describe), column information (info), frequency tables
  - Sampling (random, by count, by fraction, with seed)
  - Head/tail operations, null detection and counting
- **Gsmile Extension Module**: Natural Groovy syntax extensions for Matrix and DataFrame
  - Matrix extensions: toSmileDataFrame(), smileDescribe(), smileSample()
  - DataFrame extensions: toMatrix(), subscript operators (getAt), filtering, iteration
  - Comprehensive test coverage (24 extension method tests)

### Machine Learning Wrappers
- **SmileClassifier**: Wrappers for classification algorithms
  - Decision Trees, Random Forest
  - Model training, prediction, and evaluation with confusion matrices
- **SmileRegression**: Wrappers for regression algorithms
  - Linear Regression (OLS), Ridge Regression, LASSO, Elastic Net
  - Model fitting, prediction, and evaluation metrics (R², MSE, RMSE, MAE)
- **SmileCluster**: Wrappers for clustering algorithms
  - K-Means, DBSCAN
  - Cluster assignment and centroids calculation
- **SmileDimensionality**: Dimensionality reduction techniques
  - PCA (Principal Component Analysis)

### Statistical Analysis (SmileStats)
- **Probability Distributions**:
  - Discrete: Binomial, Geometric, Poisson
  - Continuous: Normal, Exponential, Gamma, Beta, Chi-Squared, T, F, Log-Normal, Weibull
  - PDF, CDF, quantile, and random sample generation
- **Hypothesis Testing**:
  - t-tests (one-sample, two-sample with pooled or Welch's variance, paired)
  - Chi-squared test, F-test, Kolmogorov-Smirnov test
  - Correlation tests (Pearson, Spearman, Kendall) with significance testing
- **Correlation Analysis**:
  - Correlation matrices with p-values
  - Support for Pearson, Spearman, and Kendall correlation methods
- **Random Sampling**: Generate random samples from various distributions

### Feature Engineering (SmileFeatures)
- **Feature Scaling**:
  - StandardScaler (z-score normalization with fit/transform workflow)
  - MinMaxScaler (range normalization with fit/transform workflow)
- **Feature Encoding**:
  - One-hot encoding for categorical variables
  - Label encoding for ordinal variables
- **Transformations**: Log, square root, and power transformations
- **Binning**: Equal-width and custom edge binning
- **Imputation**: Missing value handling with mean, median, or constant strategies
- **Data Cleaning**: dropna for null-containing rows

### Code Quality
- Comprehensive @CompileStatic annotation throughout for type safety and performance
- Modern Groovy 5.0+ switch expression syntax (arrow operators)
- Extensive GroovyDoc documentation
- Comprehensive test coverage (274+ tests across 10 test files)
- Idiomatic Groovy code (as double instead of .doubleValue(), NumberExtension usage)

### Dependencies
- com.github.haifengl:smile-core 4.4.2
- Requires Java 21 (Smile 4.x not compatible with Java 22+)
- Requires Groovy 5.0+ (for modern switch expression syntax)
