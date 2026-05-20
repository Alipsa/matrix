# Matrix-Smile

Integration between [Matrix](https://github.com/Alipsa/matrix) and [Smile](https://haifengl.github.io/) (Statistical Machine Intelligence and Learning Engine).

## Requirements

- Java 21 or earlier (Smile 4.x is not compatible with Java 22+)
- Groovy 5.0+ (required for modern switch expression syntax)

## Installation

Add the dependency to your build.gradle:

```groovy
implementation 'org.apache.groovy:groovy:5.0.6'
implementation 'se.alipsa.matrix:matrix-core:3.7.1'
implementation 'se.alipsa.matrix:matrix-smile:0.2.0'
```

## Design Principles

1. **Matrix-first API** - All methods accept and return Matrix where possible
2. **Type safety** - Use `@CompileStatic` throughout
3. **Consistency** - Follow patterns from matrix-tablesaw and matrix-stats
4. **Documentation** - Each class has usage examples
5. **Testability** - Comprehensive tests for each wrapper

## Usage

### Converting Between Matrix and Smile DataFrame

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.DataframeConverter
import se.alipsa.matrix.smile.SmileUtil
import smile.data.DataFrame

// Create a Matrix
def matrix = Matrix.builder()
    .data(
        id: [1, 2, 3, 4, 5],
        name: ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve'],
        salary: [50000.0, 60000.0, 55000.0, 70000.0, 65000.0]
    )
    .types([Integer, String, Double])
    .build()

// Convert Matrix to Smile DataFrame
DataFrame df = DataframeConverter.convert(matrix)
// or using SmileUtil
DataFrame df2 = SmileUtil.toDataFrame(matrix)

// Convert Smile DataFrame back to Matrix
Matrix result = DataframeConverter.convert(df)
// or using SmileUtil
Matrix result2 = SmileUtil.toMatrix(df)
```

### Statistical Summary

Get a pandas-like describe() summary of your data:

```groovy
import se.alipsa.matrix.smile.SmileUtil

def matrix = Matrix.builder()
    .data(
        age: [25, 30, 35, 40, 45, 50],
        income: [40000, 55000, 60000, 75000, 80000, 90000]
    )
    .types([Integer, Double])
    .build()

// Get statistical summary (count, mean, std, min, 25%, 50%, 75%, max)
Matrix summary = SmileUtil.describe(matrix)
println summary.content()
```

Output:
```
statistic   age      income
count       6        6
mean        37.5     66666.6667
std         9.354    18257.4188
min         25.0     40000.0
25%         28.75    51250.0
50%         37.5     67500.0
75%         46.25    82500.0
max         50.0     90000.0
```

### Column Information

Get detailed information about each column:

```groovy
Matrix info = SmileUtil.info(matrix)
println info.content()
```

Output:
```
column   type      non-null   null   unique
age      Integer   6          0      6
income   Double    6          0      6
```

### Frequency Tables

Analyze the distribution of categorical data:

```groovy
def matrix = Matrix.builder()
    .data(
        department: ['Sales', 'IT', 'Sales', 'HR', 'IT', 'IT', 'Sales']
    )
    .types([String])
    .build()

Matrix freq = SmileUtil.frequency(matrix, 'department')
println freq.content()
```

Output:
```
value    frequency   percent
IT       3           42.86
Sales    3           42.86
HR       1           14.29
```

### Sampling Data

Take random samples from your data:

```groovy
// Sample by count
Matrix sample = SmileUtil.sample(matrix, 10)

// Sample by fraction (e.g., 20% of rows)
Matrix sample = SmileUtil.sample(matrix, 0.2)

// Reproducible sampling with a seed
Matrix sample = SmileUtil.sample(matrix, 10, new Random(42))
```

### Head and Tail

View the first or last rows:

```groovy
// Get first 5 rows (default)
Matrix first5 = SmileUtil.head(matrix)

// Get first 10 rows
Matrix first10 = SmileUtil.head(matrix, 10)

// Get last 5 rows (default)
Matrix last5 = SmileUtil.tail(matrix)

// Get last 3 rows
Matrix last3 = SmileUtil.tail(matrix, 3)
```

### Null Detection

Check for null values in your data:

```groovy
// Check if any nulls exist
boolean hasNulls = SmileUtil.hasNulls(matrix.column('age'))

// Count null values
int nullCount = SmileUtil.countNulls(matrix.column('age'))
```

### Feature Engineering with SmileFeatures

```groovy
import se.alipsa.matrix.smile.data.SmileFeatures

// Standardization and normalization
Matrix standardized = SmileFeatures.standardize(features)
Matrix normalized = SmileFeatures.normalize(features)

// Categorical encoding (throws on null values)
Matrix oneHot = SmileFeatures.oneHotEncode(matrix, 'category')
Matrix labeled = SmileFeatures.labelEncode(matrix, 'category')

// Stateful encoders: fit on train, apply to test
def le = SmileFeatures.labelEncoder()
le.fit(train, 'color')
Matrix trainEncoded = le.transform(train, 'color')
Matrix testEncoded = le.transform(test, 'color')
String original = le.inverse(0)  // reverse lookup

def ohe = SmileFeatures.oneHotEncoder()
Matrix result = ohe.fitTransform(data, 'color')
// Columns: color_blue, color_green, color_red
```

### Groovy Extensions with Gsmile

```groovy
import se.alipsa.matrix.smile.Gsmile

// Matrix extensions
DataFrame df = Gsmile.toSmileDataFrame(matrix)
Matrix stats = Gsmile.smileDescribe(matrix)
// The following are convenience aliases that delegate to the matrix-core methods
// (Matrix.top, Matrix.bottom, Matrix.info, Stat.frequency, Matrix.sample)
Matrix head = Gsmile.smileHead(matrix, 3)
Matrix tail = Gsmile.smileTail(matrix, 3)
Matrix info = Gsmile.smileInfo(matrix)
Matrix freq = Gsmile.smileFrequency(matrix, 'category')
Matrix sample = Gsmile.smileSample(matrix, 10)

// DataFrame extensions
Matrix converted = Gsmile.toMatrix(df)
DataFrame filtered = Gsmile.filter(df) { row -> (row['age'] as Integer) > 30 }
```

### Statistical Analysis with SmileStats

```groovy
import se.alipsa.matrix.smile.stats.SmileStats

// Fit distributions from double[], List<Number>, or Matrix column
def dist = SmileStats.normalFit([1.2, 2.3, 1.8, 2.1, 1.9] as double[])
def dist2 = SmileStats.normalFit([1.2, null, 1.8, 2.1])  // nulls excluded
def dist3 = SmileStats.normalFit(matrix, 'values')         // from column

// Hypothesis tests
def tTest = SmileStats.tTestOneSample(data, 0.0)
def corr = SmileStats.correlation(x, y, CorrelationMethod.PEARSON)
```

## Supported Data Types

The converter supports the following data types:

| Matrix Type     | Smile Type        |
|-----------------|-------------------|
| Integer/int     | IntType           |
| Long/long       | LongType          |
| Double/double   | DoubleType        |
| Float/float     | FloatType         |
| Short/short     | ShortType         |
| Byte/byte       | ByteType          |
| Boolean/boolean | BooleanType       |
| Character/char  | CharType          |
| String          | StringType        |
| BigDecimal      | DecimalType       |
| LocalDate       | DateType          |
| LocalDateTime   | DateTimeType      |
| LocalTime       | TimeType          |
| Timestamp       | TimestampType     |
| Instant         | InstantType       |
| ZonedDateTime   | ZonedDateTimeType |
| OffsetTime      | OffsetTimeType    |
| Enum            | NominalType       |

## API Reference

### DataframeConverter

| Method                   | Description                       |
|--------------------------|-----------------------------------|
| `convert(DataFrame df)`  | Convert Smile DataFrame to Matrix |
| `convert(Matrix matrix)` | Convert Matrix to Smile DataFrame |
| `getType(DataType type)` | Get Java class for Smile DataType |

### SmileUtil

| Method                                    | Description                               |
|-------------------------------------------|-------------------------------------------|
| `toMatrix(DataFrame df)`                  | Convert Smile DataFrame to Matrix         |
| `toDataFrame(Matrix matrix)`              | Convert Matrix to Smile DataFrame         |
| `describe(Matrix matrix)`                 | Statistical summary of numeric columns              |
| `info(Matrix matrix)`                     | *(deprecated)* Use `Matrix.info()` instead           |
| `frequency(Matrix matrix, String column)` | *(deprecated)* Use `Stat.frequency()` instead        |
| `sample(Matrix matrix, int n)`            | *(deprecated)* Use `Matrix.sample()` instead         |
| `sample(Matrix matrix, double fraction)`  | *(deprecated)* Use `Matrix.sampleFraction()` instead |
| `head(Matrix matrix, int n)`              | *(deprecated)* Use `Matrix.top()` instead            |
| `tail(Matrix matrix, int n)`              | *(deprecated)* Use `Matrix.bottom()` instead         |
| `hasNulls(List values)`                   | Check if list contains nulls                         |
| `countNulls(List values)`                 | Count null values in list                            |

### SmileFeatures

| Method / Class | Description |
|----------------|-------------|
| `standardize(Matrix, List<String>)` | Z-score standardization (mean=0, std=1) |
| `normalize(Matrix, List<String>, min, max)` | Min-max normalization |
| `oneHotEncode(Matrix, String, boolean)` | One-hot encoding (throws on null) |
| `labelEncode(Matrix, String)` | Label encoding (throws on null) |
| `standardScaler()` | Create a reusable StandardScaler |
| `minMaxScaler()` | Create a reusable MinMaxScaler |
| `labelEncoder()` | Create a stateful LabelEncoder |
| `oneHotEncoder()` | Create a stateful OneHotEncoder |

### LabelEncoder

| Method | Description |
|--------|-------------|
| `fit(Matrix, String)` | Learn label mapping from column |
| `transform(Matrix, String)` | Apply mapping (throws on null/unseen) |
| `fitTransform(Matrix, String)` | Fit and transform in one step |
| `getLabels()` | Ordered list of labels |
| `inverse(int)` | Reverse lookup: index to label |

### OneHotEncoder

| Method | Description |
|--------|-------------|
| `fit(Matrix, String)` | Learn categories from column |
| `transform(Matrix, String, boolean)` | Produce binary columns (throws on null/unseen) |
| `fitTransform(Matrix, String, boolean)` | Fit and transform in one step |
| `getCategories()` | Ordered list of categories |

## License

This project is licensed under the MIT License.
