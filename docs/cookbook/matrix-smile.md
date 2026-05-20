# Matrix-Smile Cookbook

Recipes for Smile ML integration: data conversion, feature engineering, statistical analysis, and machine learning.

## Data Conversion

### Matrix to Smile DataFrame and back

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.SmileUtil
import smile.data.DataFrame

Matrix matrix = Matrix.builder()
    .data(name: ['Alice', 'Bob'], age: [25, 30], salary: [50000.0, 60000.0])
    .types([String, Integer, Double])
    .build()

DataFrame df = SmileUtil.toDataFrame(matrix)
Matrix back = SmileUtil.toMatrix(df)
```

## Exploratory Data Analysis

### Statistical summary

```groovy
Matrix stats = SmileUtil.describe(matrix)
println stats.content()
// Prints count, mean, std, min, 25%, 50%, 75%, max for each numeric column
```

### Column metadata

```groovy
Matrix info = matrix.info()
println info.content()
// Prints column name, type, non-null count, null count, unique count
```

### Frequency tables

```groovy
import se.alipsa.matrix.core.Stat

Matrix freq = Stat.frequency(matrix, 'department')
println freq.content()
// Prints value, frequency, percent sorted by frequency descending
```

### Sampling and slicing

```groovy
Matrix first5 = matrix.top()          // first 5 rows as Matrix
Matrix last3 = matrix.bottom(3)       // last 3 rows as Matrix
Matrix sample = matrix.sample(10)     // 10 random rows without replacement
Matrix frac = matrix.sampleFraction(0.2)  // 20% of rows
```

## Feature Engineering

### Standardization and normalization

```groovy
import se.alipsa.matrix.smile.data.SmileFeatures

// Z-score standardization (mean=0, std=1)
Matrix zScored = SmileFeatures.standardize(features)

// Min-max normalization to [0, 1]
Matrix normalized = SmileFeatures.normalize(features)

// Reusable scaler (fit on train, transform test with same parameters)
def scaler = SmileFeatures.standardScaler()
Matrix trainScaled = scaler.fitTransform(trainFeatures)
Matrix testScaled = scaler.transform(testFeatures)
```

### Categorical encoding — stateless

```groovy
// One-hot encoding (throws on null values)
Matrix oneHot = SmileFeatures.oneHotEncode(matrix, 'category')

// Label encoding (throws on null values)
Matrix labeled = SmileFeatures.labelEncode(matrix, 'category')
```

### Categorical encoding — stateful (fit on train, transform test)

```groovy
// LabelEncoder
def le = SmileFeatures.labelEncoder()
le.fit(train, 'color')              // learn mapping: blue=0, green=1, red=2
Matrix trainEncoded = le.transform(train, 'color')
Matrix testEncoded = le.transform(test, 'color')
String original = le.inverse(0)     // -> 'blue'

// OneHotEncoder
def ohe = SmileFeatures.oneHotEncoder()
Matrix result = ohe.fitTransform(train, 'color')
// Produces columns: color_blue, color_green, color_red

// Or fit and transform separately
ohe.fit(train, 'color')
Matrix testResult = ohe.transform(test, 'color')

// Keep original column alongside one-hot columns
Matrix withOriginal = ohe.transform(test, 'color', false)
```

### Transformations and binning

```groovy
Matrix logged = SmileFeatures.logTransform(features, 'income')
Matrix sqrted = SmileFeatures.sqrtTransform(features, ['value'])
Matrix binned = SmileFeatures.binning(features, 'age', 5)  // 5 equal-width bins
```

### Missing value handling

```groovy
Matrix filled = SmileFeatures.fillnaMean(data, 'value')     // fill with column mean
Matrix dropped = SmileFeatures.dropna(data)                  // drop rows with any null
```

## Distribution Fitting

```groovy
import se.alipsa.matrix.smile.stats.SmileStats

// From double[]
def dist = SmileStats.normalFit([1.2, 2.3, 1.8, 2.1] as double[])

// From List<Number> (nulls excluded automatically)
def dist2 = SmileStats.normalFit([1.2, null, 1.8, 2.1])

// From Matrix column (nulls excluded automatically)
def dist3 = SmileStats.normalFit(matrix, 'values')

// Available: normalFit, exponentialFit, gammaFit, betaFit, logNormalFit
```

## Machine Learning Quick-Start

### Classification

```groovy
import se.alipsa.matrix.smile.ml.SmileClassifier
import se.alipsa.matrix.smile.data.SmileData

def (train, test) = SmileData.stratifiedSplit(data, 'Species', 0.2, 42)
def model = SmileClassifier.randomForest(train, 'Species')
Matrix predictions = SmileClassifier.predict(model, test, 'Species')
double accuracy = SmileClassifier.accuracy(predictions, 'Species', 'prediction')
```

### Regression

```groovy
import se.alipsa.matrix.smile.ml.SmileRegression

def model = SmileRegression.ols(train, 'price')
Matrix predictions = SmileRegression.predict(model, test, 'price')
```

### Clustering

```groovy
import se.alipsa.matrix.smile.ml.SmileCluster

def result = SmileCluster.kMeans(features, 3)  // 3 clusters
Matrix labeled = SmileCluster.getLabelsMatrix(result, features)
```
