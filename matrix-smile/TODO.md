# Matrix-Smile Improvement Plan

## Current State

The module currently provides only bidirectional conversion between Matrix and Smile DataFrame via `DataframeConverter`. This is a solid foundation but underutilizes Smile's extensive ML capabilities.

---

## Phase 1: Foundation Improvements

### 1.1 Create SmileUtil Class
Create a central utility class (similar to TableUtil in matrix-tablesaw) with:
- `toMatrix(DataFrame)` - convert Smile DataFrame to Matrix
- `toDataFrame(Matrix)` - convert Matrix to Smile DataFrame
- `describe(Matrix)` - statistical summary
- `sample(Matrix, n)` - random sampling
- Optimized null detection for choosing `of()` vs `ofNullable()` vectors (addresses TODO at DataframeConverter.groovy:73)

### 1.2 Add README.md
- Usage examples for conversion
- JDK compatibility notes (Smile 4.x requires JDK ≤21)
- Quick start guide

### 1.3 Performance Optimization in DataframeConverter
- Detect null presence in columns to choose optimal ValueVector variant
- Bulk data transfer instead of row-by-row iteration where possible

---

## Phase 2: Machine Learning Wrappers

### 2.1 Create SmileClassifier
```groovy
// Target API
def classifier = SmileClassifier.randomForest(trainingMatrix, 'targetColumn')
def predictions = classifier.predict(testMatrix)
def accuracy = classifier.evaluate(testMatrix, 'targetColumn')
```
Algorithms to support:
- RandomForest
- SVM
- DecisionTree
- NaiveBayes
- KNN
- LogisticRegression

Features:
- Return predictions as Matrix column
- Confusion matrix as Matrix

### 2.2 Create SmileRegression
```groovy
def model = SmileRegression.ols(matrix, 'dependent', ['x1', 'x2'])
def predictions = model.predict(newData)
def stats = model.summary() // R², coefficients, p-values as Matrix
```
Algorithms to support:
- OLS (Ordinary Least Squares)
- Ridge
- Lasso
- ElasticNet

### 2.3 Create SmileCluster
```groovy
def result = SmileCluster.kmeans(matrix, k: 3, columns: ['x', 'y'])
def labeled = result.labeledMatrix() // Original matrix + cluster column
def centroids = result.centroids() // Centroids as Matrix
```
Algorithms to support:
- KMeans
- KMeans++
- Hierarchical
- DBSCAN
- GMM (Gaussian Mixture Models)

### 2.4 Create SmileDimensionality
```groovy
def pca = SmileDimensionality.pca(matrix, components: 2)
def reduced = pca.transform(matrix)
def variance = pca.explainedVariance()
```
Algorithms to support:
- PCA
- t-SNE
- IsoMap

---

## Phase 3: Data Operations

### 3.1 Create SmileData - Train/Test Splitting
```groovy
def (train, test) = SmileData.trainTestSplit(matrix, testRatio: 0.2, shuffle: true)
def folds = SmileData.kFold(matrix, k: 5)
```

### 3.3 Create SmileFeatures - Feature Engineering
```groovy
def scaled = SmileFeatures.standardize(matrix, ['col1', 'col2'])
def encoded = SmileFeatures.oneHotEncode(matrix, 'category')
def normalized = SmileFeatures.normalize(matrix, ['col1'])
```

---

## Phase 4: Statistical Extensions

### 4.1 Create SmileStats
Complement matrix-stats with ML-focused statistics:
- Probability distributions: `normal()`, `poisson()`, `binomial()`
- Hypothesis tests not in matrix-stats
- Correlation matrices with significance

---

## Phase 5: Groovy Convenience

### 5.1 Create Gsmile Extension Class
```groovy
// Enable operator overloading and Groovy idioms
def df = matrix.toSmileDataFrame()
def result = df[0..10, 'name', 'value']  // Slicing syntax
```

---

## Proposed File Structure

```
matrix-smile/src/main/groovy/se/alipsa/matrix/smile/
├── DataframeConverter.groovy  (existing)
├── SmileUtil.groovy           (1.1)
├── Gsmile.groovy              (5.1)
├── ml/
│   ├── SmileClassifier.groovy (2.1)
│   ├── SmileRegression.groovy (2.2)
│   ├── SmileCluster.groovy    (2.3)
│   └── SmileDimensionality.groovy (2.4)
├── data/
│   ├── SmileData.groovy       (3.1)
│   ├── SmileDatasets.groovy   (3.2)
│   └── SmileFeatures.groovy   (3.3)
└── stats/
    └── SmileStats.groovy      (4.1)
```

---

## Design Principles

1. **Matrix-first API** - All methods accept and return Matrix where possible
2. **Type safety** - Use `@CompileStatic` throughout
3. **Consistency** - Follow patterns from matrix-tablesaw and matrix-stats
4. **Documentation** - Each class has usage examples
5. **Testability** - Comprehensive tests for each wrapper
