# Matrix Smile Module

The matrix-smile module provides integration with the [Smile](https://haifengl.github.io/) (Statistical Machine Intelligence and Learning Engine) library. This enables machine learning capabilities including classification, regression, clustering, dimensionality reduction, and advanced statistical analysis directly on Matrix data.

## Overview

The matrix-smile module complements matrix-stats with ML-focused functionality:

- **SmileUtil** - Core conversion utilities between Matrix and Smile DataFrame
- **Gsmile** - Groovy extension methods for idiomatic syntax
- **SmileStats** - Probability distributions, hypothesis tests, correlation with significance
- **SmileClassifier** - Classification algorithms (Random Forest, Decision Tree)
- **SmileRegression** - Regression algorithms (OLS, Ridge, LASSO, ElasticNet)
- **SmileCluster** - Clustering algorithms (K-Means, DBSCAN)
- **SmileDimensionality** - Dimensionality reduction (PCA)
- **SmileData** - Data splitting for ML workflows
- **SmileFeatures** - Feature engineering and preprocessing

## Installation

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.2'
implementation "se.alipsa.matrix:matrix-core:3.2.0"
implementation "se.alipsa.matrix:matrix-smile:1.0.0"
```

### Maven Configuration

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.2</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
        <version>3.2.0</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-smile</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Data Conversion with SmileUtil

The `SmileUtil` class provides conversion between Matrix and Smile DataFrame, along with utility methods for data exploration.

### Converting Between Matrix and DataFrame

```groovy
import se.alipsa.matrix.smile.SmileUtil
import se.alipsa.matrix.core.Matrix
import smile.data.DataFrame

// Create a Matrix
Matrix matrix = Matrix.builder()
    .data(
        name: ['Alice', 'Bob', 'Charlie'],
        age: [25, 30, 35],
        salary: [50000.0, 60000.0, 75000.0]
    )
    .types([String, Integer, Double])
    .build()

// Convert to Smile DataFrame
DataFrame df = SmileUtil.toDataFrame(matrix)
println "DataFrame: ${df.nrow()} rows x ${df.ncol()} columns"

// Convert back to Matrix
Matrix converted = SmileUtil.toMatrix(df)
```

### Statistical Summary with describe()

Generate a pandas-like statistical summary:

```groovy
import se.alipsa.matrix.smile.SmileUtil
import se.alipsa.matrix.datasets.Dataset

Matrix iris = Dataset.iris()
Matrix stats = SmileUtil.describe(iris)
println stats
```

Output:
```
Matrix (null, 8 x 5)
statistic	Sepal Length	Sepal Width	Petal Length	Petal Width
count    	150         	150        	150         	150
mean     	5.8433      	3.054      	3.7587      	1.1987
std      	0.8281      	0.4336     	1.7644      	0.7632
min      	4.3         	2.0        	1.0         	0.1
25%      	5.1         	2.8        	1.6         	0.3
50%      	5.8         	3.0        	4.35        	1.3
75%      	6.4         	3.3        	5.1         	1.8
max      	7.9         	4.4        	6.9         	2.5
```

### Random Sampling

```groovy
// Sample n rows
Matrix sample = SmileUtil.sample(iris, 10)
println "Sampled ${sample.rowCount()} rows"

// Sample a fraction of rows
Matrix sample20 = SmileUtil.sample(iris, 0.2)
println "20% sample: ${sample20.rowCount()} rows"

// Reproducible sampling with seed
Matrix reproducible = SmileUtil.sample(iris, 10, new Random(42))
```

### Column Information

```groovy
// Get column info including null counts
Matrix info = SmileUtil.info(iris)
println info

// Get frequency table for a column
Matrix freq = SmileUtil.frequency(iris, 'Species')
println freq
```

## Groovy Extensions with Gsmile

The `Gsmile` class adds Groovy-idiomatic methods to Matrix and Smile DataFrame classes.

### Matrix Extensions

```groovy
import se.alipsa.matrix.smile.Gsmile
import se.alipsa.matrix.datasets.Dataset

Matrix iris = Dataset.iris()

// Convert to Smile DataFrame
DataFrame df = Gsmile.toSmileDataFrame(iris)

// Get statistical description
Matrix stats = Gsmile.smileDescribe(iris)

// Random sample
Matrix sample = Gsmile.smileSample(iris, 50)
```

### DataFrame Extensions

```groovy
// Convert DataFrame to Matrix
Matrix matrix = Gsmile.toMatrix(df)
Matrix named = Gsmile.toMatrix(df, 'myMatrix')

// Get row count and column count
int rows = Gsmile.rowCount(df)
int cols = Gsmile.columnCount(df)

// Get column names
List<String> names = Gsmile.columnNames(df)

// Get structure summary
String structure = Gsmile.structure(df)
println structure
```

### Slicing and Indexing

```groovy
// Get a single row as a Map
Map<String, Object> row = Gsmile.getAt(df, 0)
println "First row: ${row}"

// Get a column
def column = Gsmile.getAt(df, 'Sepal Length')

// Slice rows using a range
DataFrame subset = Gsmile.getAt(df, 0..9)  // First 10 rows

// Select specific columns
DataFrame selected = Gsmile.getAt(df, ['Sepal Length', 'Petal Length'])

// Head and tail
DataFrame head = Gsmile.head(df, 5)
DataFrame tail = Gsmile.tail(df, 5)
```

### Filtering and Iteration

```groovy
// Filter rows using a closure
DataFrame filtered = Gsmile.filter(df) { row ->
    (row['Sepal Length'] as Double) > 6.0
}
println "Filtered to ${Gsmile.rowCount(filtered)} rows"

// Iterate over rows
Gsmile.eachRow(df) { row ->
    println "Species: ${row['Species']}"
}

// Collect values from rows
List<Double> lengths = Gsmile.collectRows(df) { row ->
    row['Sepal Length'] as Double
}
```

## Statistical Analysis with SmileStats

The `SmileStats` class provides probability distributions, hypothesis tests, and correlation analysis with significance testing.

### Probability Distributions

```groovy
import se.alipsa.matrix.smile.stats.SmileStats

// Normal distribution
def normal = SmileStats.normal(0.0, 1.0)  // mean=0, sd=1
println "PDF at 0: ${normal.p(0.0)}"
println "CDF at 1.96: ${normal.cdf(1.96)}"
println "Random sample: ${normal.rand()}"

// Fit distribution to data
double[] data = [1.2, 2.3, 1.8, 2.1, 1.9, 2.5, 1.7]
def fitted = SmileStats.normalFit(data)
println "Fitted mean: ${fitted.mean()}, sd: ${fitted.sd()}"

// From Matrix column
Matrix matrix = Matrix.builder().data(values: data).types([Double]).build()
def fittedFromMatrix = SmileStats.normalFit(matrix, 'values')
```

### Available Distributions

```groovy
// Discrete distributions
def poisson = SmileStats.poisson(5.0)  // lambda=5
def binomial = SmileStats.binomial(10, 0.5)  // n=10, p=0.5
def bernoulli = SmileStats.bernoulli(0.7)  // p=0.7
def geometric = SmileStats.geometric(0.3)  // p=0.3

// Continuous distributions
def exponential = SmileStats.exponential(1.5)  // lambda=1.5
def gamma = SmileStats.gamma(2.0, 1.0)  // shape, scale
def beta = SmileStats.beta(2.0, 5.0)  // alpha, beta
def logNormal = SmileStats.logNormal(0.0, 1.0)  // mu, sigma
def weibull = SmileStats.weibull(2.0, 1.0)  // shape, scale

// Statistical distributions
def chiSq = SmileStats.chiSquare(5)  // degrees of freedom
def studentT = SmileStats.studentT(10)  // degrees of freedom
def fDist = SmileStats.fDistribution(5, 10)  // df1, df2
```

### Random Sampling from Distributions

```groovy
// Generate random samples
double[] normalSamples = SmileStats.randomNormal(1000, 0.0, 1.0)
int[] poissonSamples = SmileStats.randomPoisson(100, 5.0)
int[] binomialSamples = SmileStats.randomBinomial(100, 10, 0.5)
double[] uniformSamples = SmileStats.randomUniform(100, 0.0, 10.0)
double[] expSamples = SmileStats.randomExponential(100, 1.5)
```

### Hypothesis Tests

```groovy
import se.alipsa.matrix.smile.stats.SmileStats

// One-sample t-test: is mean different from 15?
double[] sample = [14, 14, 16, 13, 12, 17, 15, 14, 15, 13, 15, 14]
def tResult = SmileStats.tTestOneSample(sample, 15.0)
println "t-statistic: ${tResult.t()}, p-value: ${tResult.pvalue()}"

// Two-sample t-test
double[] group1 = [25, 27, 29, 31, 28]
double[] group2 = [22, 24, 23, 25, 21]
def twoSample = SmileStats.tTestTwoSample(group1, group2)
println "Two-sample p-value: ${twoSample.pvalue()}"

// Paired t-test
double[] before = [120, 125, 130, 118, 140]
double[] after = [118, 122, 128, 115, 135]
def paired = SmileStats.tTestPaired(before, after)
println "Paired t-test p-value: ${paired.pvalue()}"

// F-test for equality of variances
def fResult = SmileStats.fTest(group1, group2)
println "F-statistic: ${fResult.f()}, p-value: ${fResult.pvalue()}"

// KS test for normality
def ksResult = SmileStats.ksTestNormality(sample)
println "KS-test p-value: ${ksResult.pvalue()}"

// Chi-square test
int[] observed = [50, 30, 20]
double[] expected = [0.4, 0.35, 0.25]  // Must sum to 1.0
def chiResult = SmileStats.chiSquareTest(observed, expected)
println "Chi-square p-value: ${chiResult.pvalue()}"
```

### Tests on Matrix Columns

```groovy
Matrix data = Matrix.builder()
    .data(
        pre: [85, 78, 92, 91, 72, 84, 99, 80, 90, 88],
        post: [87, 80, 94, 90, 75, 88, 98, 82, 92, 89]
    )
    .types([Double, Double])
    .build()

// T-test on Matrix columns
def result = SmileStats.tTestTwoSample(data, 'pre', 'post')
println "p-value: ${result.pvalue()}"

// Paired t-test on Matrix columns
def pairedResult = SmileStats.tTestPaired(data, 'pre', 'post')
```

### Correlation with Significance

```groovy
import se.alipsa.matrix.datasets.Dataset

Matrix iris = Dataset.iris()

// Pearson correlation test
def corTest = SmileStats.correlationTest(iris, 'Sepal Length', 'Petal Length')
println "Correlation: ${corTest.cor()}"
println "p-value: ${corTest.pvalue()}"

// Spearman correlation test
def spearman = SmileStats.spearmanTest(iris, 'Sepal Length', 'Petal Length')

// Kendall correlation test
def kendall = SmileStats.kendallTest(iris, 'Sepal Length', 'Petal Length')

// Correlation matrix
Matrix corMatrix = SmileStats.correlationMatrix(iris)
println corMatrix

// P-value matrix
Matrix pMatrix = SmileStats.pValueMatrix(iris)

// Both at once
Map<String, Matrix> both = SmileStats.correlationWithSignificance(iris)
println "Correlation:\n${both.correlation}"
println "P-values:\n${both.pvalue}"

// Specify columns and method
Matrix spearmanCor = SmileStats.correlationMatrix(
    iris,
    ['Sepal Length', 'Petal Length', 'Petal Width'],
    'spearman'
)
```

## Classification with SmileClassifier

The `SmileClassifier` class provides classification algorithms with a Matrix-friendly API.

### Random Forest Classification

```groovy
import se.alipsa.matrix.smile.ml.SmileClassifier
import se.alipsa.matrix.smile.data.SmileData
import se.alipsa.matrix.datasets.Dataset

// Load and prepare data
Matrix iris = Dataset.iris()
def (train, test) = SmileData.trainTestSplit(iris, 0.2, true, 42)

// Train Random Forest classifier
def classifier = SmileClassifier.randomForest(train, 'Species', 100)  // 100 trees

// Make predictions
Matrix predictions = classifier.predict(test)
println predictions.head(5)

// Get prediction labels
List<String> labels = classifier.predictLabels(test)
println "Predictions: ${labels.take(5)}"

// Evaluate accuracy
double accuracy = classifier.accuracy(test)
println "Accuracy: ${(accuracy * 100).round(2)}%"
```

### Decision Tree Classification

```groovy
// Train Decision Tree classifier
def tree = SmileClassifier.decisionTree(train, 'Species')

// Evaluate
println "Decision Tree Accuracy: ${(tree.accuracy(test) * 100).round(2)}%"
```

### Model Evaluation

```groovy
// Confusion matrix
Matrix confusion = classifier.confusionMatrix(test)
println "Confusion Matrix:"
println confusion

// Detailed metrics (precision, recall, F1)
Matrix metrics = classifier.evaluate(test)
println "Evaluation Metrics:"
println metrics

// Get class labels
String[] classes = classifier.getClassLabels()
println "Classes: ${classes.join(', ')}"
```

## Regression with SmileRegression

The `SmileRegression` class provides linear regression models.

### Ordinary Least Squares (OLS)

```groovy
import se.alipsa.matrix.smile.ml.SmileRegression
import se.alipsa.matrix.smile.data.SmileData

// Create sample data
Matrix data = Matrix.builder()
    .data(
        x1: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
        x2: [2, 4, 5, 4, 5, 6, 7, 8, 9, 10],
        y: [3, 5, 7, 9, 11, 13, 15, 17, 19, 21]
    )
    .types([Double, Double, Double])
    .build()

def (train, test) = SmileData.trainTestSplit(data, 0.2, true, 42)

// Train OLS regression
def model = SmileRegression.ols(train, 'y')

// Make predictions
Matrix predictions = model.predict(test)
println predictions

// Get coefficients
double[] coefficients = model.getCoefficients()
double intercept = model.getIntercept()
println "Intercept: ${intercept}"
println "Coefficients: ${coefficients}"

// Evaluate
double r2 = model.rSquared(test)
double mse = model.mse(test)
double rmse = model.rmse(test)
double mae = model.mae(test)

println "R-squared: ${r2.round(4)}"
println "MSE: ${mse.round(4)}"
println "RMSE: ${rmse.round(4)}"
println "MAE: ${mae.round(4)}"

// Get all metrics as Matrix
Matrix metrics = model.evaluate(test)
println metrics
```

### Regularized Regression

```groovy
// Ridge Regression (L2 regularization)
def ridge = SmileRegression.ridge(train, 'y', 1.0)  // lambda=1.0
println "Ridge R-squared: ${ridge.rSquared(test).round(4)}"

// LASSO Regression (L1 regularization)
def lasso = SmileRegression.lasso(train, 'y', 0.1)  // lambda=0.1
println "LASSO R-squared: ${lasso.rSquared(test).round(4)}"

// ElasticNet (L1 + L2)
def elasticNet = SmileRegression.elasticNet(train, 'y', 0.5, 0.5)  // lambda1, lambda2
println "ElasticNet R-squared: ${elasticNet.rSquared(test).round(4)}"
```

## Clustering with SmileCluster

The `SmileCluster` class provides clustering algorithms.

### K-Means Clustering

```groovy
import se.alipsa.matrix.smile.ml.SmileCluster
import se.alipsa.matrix.datasets.Dataset

// Load data (numeric columns only)
Matrix iris = Dataset.iris()
Matrix features = iris.subset(
    iris.columnNames().findAll { it != 'Species' }
)

// Perform K-Means clustering
def kmeans = SmileCluster.kmeans(features, 3)  // 3 clusters

// Get cluster labels
int[] labels = kmeans.getLabels()
println "Cluster labels: ${labels.take(10)}"

// Add cluster column to original data
Matrix clustered = kmeans.addClusterColumn(features)
println clustered.head(5)

// Get cluster counts
Matrix counts = kmeans.clusterCounts()
println "Cluster counts:"
println counts

// Get centroids
Matrix centroids = kmeans.getCentroidsMatrix()
println "Cluster centroids:"
println centroids

// Predict clusters for new data
int[] newLabels = kmeans.predict(features.head(10))
```

### DBSCAN Clustering

```groovy
// DBSCAN clustering (density-based)
def dbscan = SmileCluster.dbscan(features, 5, 0.5)  // minPts, radius

// Get labels (outliers may be marked with large values)
int[] dbLabels = dbscan.getLabels()
println "Number of clusters: ${dbscan.getNumClusters()}"
println "Cluster counts:"
println dbscan.clusterCounts()
```

### Cluster Evaluation

```groovy
// Silhouette coefficient (higher is better, range -1 to 1)
double silhouette = kmeans.silhouette(features)
println "Silhouette coefficient: ${silhouette.round(4)}"
```

## Dimensionality Reduction with SmileDimensionality

The `SmileDimensionality` class provides PCA (Principal Component Analysis).

### Basic PCA

```groovy
import se.alipsa.matrix.smile.ml.SmileDimensionality
import se.alipsa.matrix.datasets.Dataset

// Load numeric features
Matrix iris = Dataset.iris()
Matrix features = iris.subset(
    iris.columnNames().findAll { it != 'Species' }
)

// Reduce to 2 dimensions
def pca = SmileDimensionality.pca(features, 2)

// Transform data
Matrix reduced = pca.transform(features)
println "Reduced data (first 5 rows):"
println reduced.head(5)
```

### Variance Analysis

```groovy
// Get variance explained
double[] variance = pca.getVariance()
double[] proportion = pca.getVarianceProportion()
double[] cumulative = pca.getCumulativeVarianceProportion()

// Variance summary as Matrix
Matrix summary = pca.varianceSummary()
println "Variance explained:"
println summary
```

Output:
```
Variance explained:
Matrix (null, 4 x 4)
component	variance	proportion	cumulative
PC1      	4.2282  	0.7296    	0.7296
PC2      	0.2424  	0.0418    	0.7714
PC3      	0.0782  	0.0135    	0.7849
PC4      	0.0238  	0.0041    	0.789
```

### PCA by Variance Retention

```groovy
// Retain 95% of variance
def pca95 = SmileDimensionality.pcaByVariance(features, 0.95)
println "Components for 95% variance: ${pca95.getNumComponents()}"

// PCA using correlation matrix (for different scales)
def pcaCor = SmileDimensionality.pcaCorrelation(features, 2)
```

### Loadings Analysis

```groovy
// Get loadings matrix
Matrix loadings = pca.getLoadingsMatrix()
println "PCA Loadings:"
println loadings

// Get center (mean) of original data
double[] center = pca.getCenter()
```

## Data Splitting with SmileData

The `SmileData` class provides data splitting utilities for ML workflows.

### Train/Test Split

```groovy
import se.alipsa.matrix.smile.data.SmileData
import se.alipsa.matrix.datasets.Dataset

Matrix iris = Dataset.iris()

// Basic split (80% train, 20% test)
def (train, test) = SmileData.trainTestSplit(iris, 0.2)
println "Train: ${train.rowCount()} rows, Test: ${test.rowCount()} rows"

// With shuffle disabled
def (train2, test2) = SmileData.trainTestSplit(iris, 0.2, false)

// With random seed for reproducibility
def (train3, test3) = SmileData.trainTestSplit(iris, 0.2, true, 42)

// Using named parameters
def splits = SmileData.trainTestSplit(
    testRatio: 0.3,
    shuffle: true,
    seed: 123,
    iris
)
```

### Stratified Split

Ensures proportional representation of each class:

```groovy
// Stratified split maintains class proportions
def (stratTrain, stratTest) = SmileData.stratifiedSplit(iris, 'Species', 0.2)

// Verify class distribution
println "Original distribution:"
println SmileUtil.frequency(iris, 'Species')
println "Stratified test distribution:"
println SmileUtil.frequency(stratTest, 'Species')
```

### K-Fold Cross-Validation

```groovy
// 5-fold cross-validation
List<SmileData.Fold> folds = SmileData.kFold(iris, 5)

for (fold in folds) {
    println "Fold ${fold.index}: train=${fold.train.rowCount()}, validation=${fold.validation.rowCount()}"
}

// Train and evaluate on each fold
double totalAccuracy = 0
for (fold in folds) {
    def classifier = SmileClassifier.randomForest(fold.train, 'Species', 50)
    double acc = classifier.accuracy(fold.validation)
    println "Fold ${fold.index} accuracy: ${(acc * 100).round(2)}%"
    totalAccuracy += acc
}
println "Average accuracy: ${((totalAccuracy / folds.size()) * 100).round(2)}%"
```

### Bootstrap Sampling

```groovy
// Create 100 bootstrap samples
List<Matrix> bootstrapSamples = SmileData.bootstrap(iris, 100)
println "Created ${bootstrapSamples.size()} bootstrap samples of size ${bootstrapSamples[0].rowCount()}"

// With specific sample size
List<Matrix> smallSamples = SmileData.bootstrap(iris, 50, 30)  // 50 samples of size 30
```

## Feature Engineering with SmileFeatures

The `SmileFeatures` class provides preprocessing transformations.

### Standardization (Z-Score)

```groovy
import se.alipsa.matrix.smile.data.SmileFeatures
import se.alipsa.matrix.datasets.Dataset

Matrix iris = Dataset.iris()
Matrix features = iris.subset(iris.columnNames().findAll { it != 'Species' })

// Standardize all numeric columns (mean=0, std=1)
Matrix standardized = SmileFeatures.standardize(features)
println standardized.head(5)

// Standardize specific columns
Matrix partial = SmileFeatures.standardize(features, ['Sepal Length', 'Petal Length'])

// Using a reusable scaler
def scaler = SmileFeatures.standardScaler()
Matrix scaled = scaler.fitTransform(features)
println "Means: ${scaler.getMeans()}"
println "Stds: ${scaler.getStds()}"
```

### Normalization (Min-Max Scaling)

```groovy
// Normalize to [0, 1] range
Matrix normalized = SmileFeatures.normalize(features)

// Normalize to custom range [-1, 1]
Matrix customNorm = SmileFeatures.normalize(features, ['Sepal Length'], -1.0, 1.0)

// Using a reusable scaler
def minMaxScaler = SmileFeatures.minMaxScaler()
Matrix scaled = minMaxScaler.fitTransform(features)
```

### Categorical Encoding

```groovy
// One-hot encoding
Matrix oneHot = SmileFeatures.oneHotEncode(iris, 'Species')
println "Columns after one-hot: ${oneHot.columnNames()}"

// One-hot multiple columns
Matrix multiHot = SmileFeatures.oneHotEncode(iris, ['Species'])

// Label encoding (convert to integers)
Matrix labeled = SmileFeatures.labelEncode(iris, 'Species')
println labeled.head(5)
```

### Transformations

```groovy
// Log transformation (log1p for handling zeros)
Matrix logTransformed = SmileFeatures.logTransform(features, 'Sepal Length')

// Square root transformation
Matrix sqrtTransformed = SmileFeatures.sqrtTransform(features, ['Sepal Length'])

// Power transformation
Matrix powerTransformed = SmileFeatures.powerTransform(features, ['Sepal Length'], 0.5)
```

### Binning

```groovy
// Equal-width binning
Matrix binned = SmileFeatures.binning(features, 'Sepal Length', 5)  // 5 bins
println binned['Sepal Length'].unique()

// Custom bin edges with labels
Matrix customBins = SmileFeatures.binning(
    features,
    'Sepal Length',
    [4.0, 5.0, 6.0, 7.0, 8.0],
    ['small', 'medium', 'large', 'xlarge']
)
```

### Handling Missing Values

```groovy
// Create data with nulls
Matrix withNulls = Matrix.builder()
    .data(
        a: [1.0, null, 3.0, 4.0],
        b: [null, 2.0, 3.0, null]
    )
    .types([Double, Double])
    .build()

// Fill with constant
Matrix filled = SmileFeatures.fillna(withNulls, 'a', 0.0)

// Fill with mean
Matrix filledMean = SmileFeatures.fillnaMean(withNulls, 'a')

// Fill with median
Matrix filledMedian = SmileFeatures.fillnaMedian(withNulls, 'a')

// Drop rows with any nulls
Matrix noNulls = SmileFeatures.dropna(withNulls)

// Drop rows with nulls in specific columns
Matrix partialDrop = SmileFeatures.dropna(withNulls, ['a'])
```

## Complete Machine Learning Example

Here's an end-to-end example demonstrating a classification workflow:

```groovy
import se.alipsa.matrix.smile.SmileUtil
import se.alipsa.matrix.smile.ml.SmileClassifier
import se.alipsa.matrix.smile.data.SmileData
import se.alipsa.matrix.smile.data.SmileFeatures
import se.alipsa.matrix.datasets.Dataset

// 1. Load and explore data
println "=== STEP 1: Load and Explore Data ==="
Matrix iris = Dataset.iris()
println "Dataset: ${iris.rowCount()} rows x ${iris.columnCount()} columns"
println "\nStatistical summary:"
println SmileUtil.describe(iris)

println "\nClass distribution:"
println SmileUtil.frequency(iris, 'Species')

// 2. Prepare features
println "\n=== STEP 2: Prepare Features ==="
Matrix features = iris.subset(iris.columnNames().findAll { it != 'Species' })
def scaler = SmileFeatures.standardScaler()
Matrix scaledFeatures = scaler.fitTransform(features)

// Combine scaled features with target
Matrix prepared = Matrix.builder()
    .columns(scaledFeatures.columns())
    .columnNames(scaledFeatures.columnNames())
    .types(scaledFeatures.types())
    .build()

// Add Species column back
Map<String, List<?>> dataMap = [:]
scaledFeatures.columnNames().each { col -> dataMap[col] = scaledFeatures[col] }
dataMap['Species'] = iris['Species']

prepared = Matrix.builder().data(dataMap).build()
println "Features standardized: mean ~ 0, std ~ 1"

// 3. Split data
println "\n=== STEP 3: Split Data ==="
def (train, test) = SmileData.stratifiedSplit(prepared, 'Species', 0.2, 42)
println "Training set: ${train.rowCount()} samples"
println "Test set: ${test.rowCount()} samples"

// 4. Train models
println "\n=== STEP 4: Train Models ==="
def rfClassifier = SmileClassifier.randomForest(train, 'Species', 100)
def dtClassifier = SmileClassifier.decisionTree(train, 'Species')
println "Models trained: Random Forest (100 trees), Decision Tree"

// 5. Evaluate models
println "\n=== STEP 5: Evaluate Models ==="
double rfAccuracy = rfClassifier.accuracy(test)
double dtAccuracy = dtClassifier.accuracy(test)
println "Random Forest Accuracy: ${(rfAccuracy * 100).round(2)}%"
println "Decision Tree Accuracy: ${(dtAccuracy * 100).round(2)}%"

println "\nRandom Forest Confusion Matrix:"
println rfClassifier.confusionMatrix(test)

println "\nRandom Forest Detailed Metrics:"
println rfClassifier.evaluate(test)

// 6. Make predictions
println "\n=== STEP 6: Make Predictions ==="
List<String> predictions = rfClassifier.predictLabels(test.head(5))
println "Sample predictions: ${predictions}"

println "\n=== Analysis Complete ==="
```

Output:
```
=== STEP 1: Load and Explore Data ===
Dataset: 150 rows x 5 columns

Statistical summary:
Matrix (null, 8 x 5)
statistic   Sepal Length  Sepal Width  Petal Length  Petal Width
count       150           150          150           150
mean        5.8433        3.054        3.7587        1.1987
...

Class distribution:
Matrix (null, 3 x 3)
value           frequency  percent
setosa          50         33.33
versicolor      50         33.33
virginica       50         33.33

=== STEP 2: Prepare Features ===
Features standardized: mean ~ 0, std ~ 1

=== STEP 3: Split Data ===
Training set: 120 samples
Test set: 30 samples

=== STEP 4: Train Models ===
Models trained: Random Forest (100 trees), Decision Tree

=== STEP 5: Evaluate Models ===
Random Forest Accuracy: 96.67%
Decision Tree Accuracy: 93.33%

Random Forest Confusion Matrix:
Matrix (null, 3 x 4)
actual      pred_setosa  pred_versicolor  pred_virginica
setosa      10           0                0
versicolor  0            9                1
virginica   0            0                10

Random Forest Detailed Metrics:
Matrix (null, 3 x 5)
class       precision  recall  f1      support
setosa      1.0        1.0     1.0     10
versicolor  1.0        0.9     0.9474  10
virginica   0.9091     1.0     0.9524  10

=== STEP 6: Make Predictions ===
Sample predictions: [setosa, setosa, versicolor, virginica, setosa]

=== Analysis Complete ===
```

## Conclusion

The matrix-smile module brings the power of the Smile machine learning library to the Matrix ecosystem. Key capabilities include:

- **Data conversion** between Matrix and Smile DataFrame
- **Statistical analysis** with distributions, hypothesis tests, and correlation
- **Classification** with Random Forest and Decision Tree
- **Regression** with OLS, Ridge, LASSO, and ElasticNet
- **Clustering** with K-Means and DBSCAN
- **Dimensionality reduction** with PCA
- **Data splitting** for train/test, k-fold CV, and stratified sampling
- **Feature engineering** with scaling, encoding, and transformations

Combined with matrix-arff for data interchange, matrix-smile provides a complete pipeline for machine learning workflows in Groovy.

Go to [previous section](16-matrix-arff.md) | Go to [next section](18-advanced-operations.md) | Back to [outline](outline.md)
