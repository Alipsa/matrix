# Matrix-smile release history

## v0.1.0, 2026-01-31
Initial release providing comprehensive integration between Matrix and Smile (Statistical Machine Intelligence and Learning Engine).

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
  - Logistic Regression, Decision Trees, Random Forest, Gradient Boosted Trees
  - Support Vector Machines, K-Nearest Neighbors, Naive Bayes, AdaBoost
  - Model training, prediction, and evaluation with confusion matrices
- **SmileRegression**: Wrappers for regression algorithms
  - Linear Regression, Ridge Regression, LASSO, Elastic Net
  - Regression Trees, Gradient Boosted Trees, Random Forest
  - Model fitting, prediction, and RMSE calculation
- **SmileCluster**: Wrappers for clustering algorithms
  - K-Means, Hierarchical Clustering, DBSCAN, DENCLUE, CLARANS
  - Cluster assignment and centroids calculation
- **SmileDimensionality**: Dimensionality reduction techniques
  - PCA (Principal Component Analysis), MDS (Multidimensional Scaling)
  - t-SNE (t-Distributed Stochastic Neighbor Embedding)

### Statistical Analysis (SmileStats)
- **Probability Distributions**:
  - Discrete: Binomial, Geometric, Poisson, Hypergeometric
  - Continuous: Normal, Exponential, Gamma, Beta, Chi-Squared, T, F, Weibull
  - PDF, CDF, quantile, and random sample generation
- **Hypothesis Testing**:
  - t-tests (one-sample, two-sample, paired)
  - Chi-squared test, F-test, Kolmogorov-Smirnov test
  - Correlation tests (Pearson, Spearman, Kendall) with significance testing
- **Correlation Analysis**:
  - Correlation matrices with p-values
  - Support for Pearson, Spearman, and Kendall correlation methods
- **Random Sampling**: Generate random samples from various distributions

### Feature Engineering (SmileFeatures)
- **Data Loading**: Load datasets from Smile's built-in data repository
- **Feature Scaling**:
  - StandardScaler (z-score normalization with fit/transform workflow)
  - MinMaxScaler (range normalization)
  - MaxAbsScaler (maximum absolute value scaling)
  - RobustScaler (median and IQR-based scaling)
- **Feature Encoding**:
  - One-hot encoding for categorical variables
  - Label encoding for ordinal variables
- **Feature Selection**:
  - Sum, difference, product, ratio feature creation
- **Imputation**: Missing value handling with mean, median, mode, or constant strategies

### Code Quality
- Comprehensive @CompileStatic annotation throughout for type safety and performance
- Modern Groovy 5.0+ switch expression syntax (arrow operators)
- Extensive GroovyDoc documentation (207 JavaDoc blocks)
- Comprehensive test coverage (274 tests across 10 test files, 100% test file coverage)
- Idiomatic Groovy code (as double instead of .doubleValue(), NumberExtension usage)

### Dependencies
- com.github.haifengl:smile-core 4.4.2
- Requires Java 21 (Smile 4.x not compatible with Java 22+)
- Requires Groovy 5.0+ (for modern switch expression syntax)