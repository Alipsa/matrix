# Matrix-Smile

Integration between [Matrix](https://github.com/Alipsa/matrix) and [Smile](https://haifengl.github.io/) (Statistical Machine Intelligence and Learning Engine).

## Requirements

- Java 21 or earlier (Smile 4.x is not compatible with Java 22+)
- Groovy 5.0+ (required for modern switch expression syntax)

## Installation

Add the dependency to your build.gradle:

```groovy
implementation 'org.apache.groovy:groovy:5.0.4'
implementation 'se.alipsa.matrix:matrix-core:3.6.0'
implementation 'se.alipsa.matrix:matrix-smile:0.1.0'
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
| `describe(Matrix matrix)`                 | Statistical summary of numeric columns    |
| `info(Matrix matrix)`                     | Column information (type, nulls, uniques) |
| `frequency(Matrix matrix, String column)` | Frequency table for a column              |
| `sample(Matrix matrix, int n)`            | Random sample of n rows                   |
| `sample(Matrix matrix, double fraction)`  | Random sample by fraction                 |
| `head(Matrix matrix, int n)`              | First n rows (default 5)                  |
| `tail(Matrix matrix, int n)`              | Last n rows (default 5)                   |
| `hasNulls(List values)`                   | Check if list contains nulls              |
| `countNulls(List values)`                 | Count null values in list                 |
| `round(double value, int decimals)`       | Round to specified decimals               |

## License

This project is licensed under the MIT License.
