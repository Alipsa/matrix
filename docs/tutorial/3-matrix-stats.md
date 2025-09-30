# Matrix Stats Module

The matrix-stats module provides statistical functions for working with Matrix data. It includes implementations for correlations, normalizations, regressions, and hypothesis tests. This module builds upon the matrix-core functionality to enable more advanced statistical analysis.

## Installation

To use the matrix-stats module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.1'
implementation "se.alipsa.matrix:matrix-core:3.2.0"
implementation "se.alipsa.matrix:matrix-stats:2.1.0"
```

### Maven Configuration

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.1</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
        <version>3.2.0</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-stats</artifactId>
        <version>2.1.0</version>
    </dependency>
</dependencies>
```

## Correlations

Correlation is a statistical measure that expresses the extent to which two variables are linearly related. The Correlation class in the matrix-stats module can perform the most common types of correlation calculations: Pearson, Spearman, and Kendall.

### Pearson Correlation

Pearson correlation is the default method and measures the linear relationship between two continuous variables:

```groovy
import static se.alipsa.matrix.stats.Correlation.*
import java.math.RoundingMode

// Calculate Pearson correlation between two lists
def x = [15, 18, 21, 24, 27]
def y = [25, 25, 27, 31, 32]
def pearsonCorr = cor(x, y).setScale(14, RoundingMode.HALF_EVEN)
assert 0.95346258924559 == pearsonCorr
```
You can also calculate correlation directly from Matrix columns
```groovy
import se.alipsa.matrix.core.Matrix
import static se.alipsa.matrix.stats.Correlation.*

def data = Matrix.builder().data(
    x: [15, 18, 21, 24, 27],
    y: [25, 25, 27, 31, 32]
).build()

def correlation = cor(data.x, data.y)
println("Correlation coefficient: ${correlation}")
```

### Spearman Correlation

Spearman correlation assesses monotonic relationships (whether the variables tend to change together, but not necessarily at a constant rate):

```groovy
import static se.alipsa.matrix.stats.Correlation.*
import java.math.RoundingMode

// Calculate Spearman correlation
def x = [15, 18, 21, 15, 21]
def y = [25, 25, 27, 27, 27]
def spearmanCorr = cor(x, y, SPEARMAN).setScale(14, RoundingMode.HALF_EVEN)
assert 0.45643546458764 == spearmanCorr
```

### Kendall Correlation

Kendall correlation measures the ordinal association between two variables:

```groovy
import static se.alipsa.matrix.stats.Correlation.*

// Calculate Kendall correlation
def x = [12, 2, 1, 12, 2]
def y = [1, 4, 7, 1, 0]
def kendallCorr = cor(x, y, KENDALL)
assert -0.47140452079103173 == kendallCorr
```

## Normalizations

The Normalize class implements various ways of scaling data. Normalization is useful for bringing different variables to a similar scale, which is often required for machine learning algorithms and statistical analyses.

The module implements four different approaches to normalization:

1. **Logarithmic normalization**: Applies a logarithmic transformation to the data
2. **Min-Max scaling**: Scales the data to a fixed range, typically 0 to 1
3. **Mean normalization**: Scales the data to have a mean of 0
4. **Standard deviation normalization (Z-score)**: Scales the data to have a mean of 0 and a standard deviation of 1

### Example Usage

```groovy
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.stats.Normalize

// Create a list of observations
List<BigDecimal> obs = ListConverter.toBigDecimals([1200, 34567, 3456, 12, 3456, 985, 1211])

// Apply mean normalization with 7 decimal places precision
def normalized = Normalize.meanNorm(obs, 7) // 7 is the number of decimals
assert [-0.1508444, 0.8147756, -0.0855572, -0.1852244, -0.0855572, -0.1570664, -0.1505261] == normalized

// Apply min-max scaling
def minMaxScaled = Normalize.minMaxNorm(obs, 7)
println("Min-Max scaled: ${minMaxScaled}")

// Apply standard deviation normalization (Z-score)
def zScores = Normalize.stdScaleNorm(obs, 7)
println("Z-scores: ${zScores}")

// Apply logarithmic normalization
def logNormalized = Normalize.logNorm(obs, 7)
println("Log normalized: ${logNormalized}")
```
Output
```
Min-Max scaled: [0.0343800, 1.0000000, 0.0996672, 0E-7, 0.0996672, 0.0281580, 0.0346983]
Z-scores: [-0.4175944, 2.2556070, -0.2368546, -0.5127711, -0.2368546, -0.4348191, -0.4167131]
Log normalized: [7.0900768, 10.4506547, 8.1478671, 2.4849066, 8.1478671, 6.8926416, 7.0992017]
```
## Linear Regression

The LinearRegression class implements least squares regression with one independent variable, estimating a linear model in the form of y = mx + b.

### Basic Usage

```groovy
import se.alipsa.matrix.stats.regression.LinearRegression

// Define independent (x) and dependent (y) variables
def x = [2.7, 3, 5, 7, 9, 11, 14]
def y = [4, 5, 7, 10.8, 15, 20, 40]

// Create a linear regression model
def model = new LinearRegression(x, y)

// Get the intercept (b in y = mx + b)
assert -6.23971066 == model.getIntercept(8) // 8 is the number of decimals to round to

// Get the slope (m in y = mx + b)
assert 2.81388732 == model.getSlope(8)

// Predict a value for a new x
assert 30.34082454 == model.predict(13, 8)
```

### Using with Matrix

You can also create a linear regression model directly from a Matrix:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.regression.LinearRegression

// Create a Matrix with x and y data
def table = Matrix.builder().data(
    x: [2.7, 3, 5, 7, 9, 11, 14],
    y: [4, 5, 7, 10.8, 15, 20, 40]
).build()

// Create a linear regression model from the Matrix
def model = new LinearRegression(table, 'x', 'y')

// Get model parameters and make predictions
println("Intercept: ${model.getIntercept(4)}")
println("Slope: ${model.getSlope(4)}")
println("Predicted value at x=10: ${model.predict(10, 4)}")

// Get the R-squared value (coefficient of determination)
println("R-squared: ${model.r2.round(4)}")
```
Output
```
Intercept: -6.2397
Slope: 2.8139
Predicted value at x=10: 21.8992
R-squared: 0.8861
```
## Hypothesis Tests

The matrix-stats module includes implementations of common statistical hypothesis tests, such as Student's t-test.

### Two Sample t-test

The two-sample t-test compares the means of two independent groups:

```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.stats.Student

// Load the iris dataset
def iris = Dataset.iris()
def speciesIdx = iris.columnIndex("Species")

// Create subsets for two different species
def setosa = iris.subset {
    it[speciesIdx] == 'setosa'
}
def virginica = iris.subset {
    it[speciesIdx] == 'virginica'
}

// Perform a t-test comparing petal lengths between species
Student.Result result = Student.tTest(setosa['Petal Length'], virginica['Petal Length'], false)
println(result)
```

The output will look like:

```
Welch two sample t-test with unequal variance
t = -49.986, df = 58.609, p = 0.000
x: mean = 1.462, size = 50, sd = 0.174
y: mean = 5.552, size = 50, sd = 0.552
```

### One Sample t-test

The one-sample t-test compares a sample mean to a known or hypothesized population mean:

```groovy
import se.alipsa.matrix.stats.Student

// Define a sample of plant heights
def plantHeights = [14, 14, 16, 13, 12, 17, 15, 14, 15, 13, 15, 14]

// Test if the mean height is significantly different from 15
def t = Student.tTest(plantHeights, 15)
println(t)
```

The output will look like:

```
One Sample t-test
t = -1.685, df = 11, p = 0.120
mean = 14.333, size = 12, sd = 1.371
```

### Paired t-test

A paired t-test compares the means of two related groups:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Student

// Create a Matrix with pre and post test scores
def data = Matrix.builder().data(
    score: [85, 85, 78, 78, 92, 94, 91, 85, 72, 97,
            84, 95, 99, 80, 90, 88, 95, 90, 96, 89,
            84, 88, 88, 90, 92, 93, 91, 85, 80, 93,
            97, 100, 93, 91, 90, 87, 94, 83, 92, 95],
    group: ['pre']*20 + ['post']*20
).types(Integer, String).build()

// Create subsets for pre and post groups
def pre = data.subset('group', {it == 'pre'})
def post = data.subset('group', {it == 'post'})

// Perform a paired t-test
def result = Student.tTest(pre.score, post.score, true)
println(result)
```
Output:
```
Welch two sample t-test with equal variance
t = -1.101, df = 38.000, p = 0.279
x: mean = 88.150, size = 20, sd = 7.242
y: mean = 90.300, size = 20, sd = 4.879 
```
## Conclusion

The matrix-stats module provides a comprehensive set of statistical tools for working with Matrix data. From basic correlation analysis to hypothesis testing and linear regression, this module enables you to perform sophisticated statistical analyses on your data.

In the next section, we'll explore the matrix-datasets module, which provides access to common datasets used in data science and statistics.

Go to [previous section](2-matrix-core.md) | Go to [next section](4-matrix-datasets.md) | Back to [outline](outline.md)