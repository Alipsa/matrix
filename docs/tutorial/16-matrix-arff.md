# Matrix ARFF Module

The matrix-arff module provides reading and writing of ARFF (Attribute-Relation File Format) files. ARFF is a standard format used by machine learning tools, particularly Weka, making this module essential for ML workflows and data interchange between different ML tools.

## What is ARFF?

ARFF (Attribute-Relation File Format) is a text-based format developed for the Weka machine learning toolkit. It's widely used in the machine learning community because it:

- Explicitly defines data types for each column
- Supports both numeric and categorical (nominal) data
- Includes metadata about the dataset (relation name, attribute definitions)
- Is human-readable and easy to inspect

An ARFF file consists of three main sections:

1. **Header comments** - Lines starting with `%` containing metadata
2. **Relation and attribute declarations** - `@RELATION` and `@ATTRIBUTE` directives
3. **Data section** - `@DATA` followed by comma-separated values

## Installation

To use the matrix-arff module, add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.3'
implementation "se.alipsa.matrix:matrix-core:3.5.0"
implementation "se.alipsa.matrix:matrix-arff:0.1.0"
```

### Maven Configuration

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.3</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
        <version>3.5.0</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-arff</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

## Reading ARFF Files

The `MatrixArffReader` class provides several methods to read ARFF files from different sources.

### Reading from a File

```groovy
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.core.Matrix

// Read the classic iris dataset
Matrix iris = MatrixArffReader.read(new File("iris.arff"))

println "Dataset: ${iris.matrixName}"
println "Rows: ${iris.rowCount()}"
println "Columns: ${iris.columnNames()}"
```

Output:
```
Dataset: iris
Rows: 150
Columns: [sepallength, sepalwidth, petallength, petalwidth, class]
```

### Reading from Different Sources

```groovy
import se.alipsa.matrix.arff.MatrixArffReader
import java.nio.file.Paths

// Read from a Path
Matrix data1 = MatrixArffReader.read(Paths.get("data/dataset.arff"))

// Read from a file path string
Matrix data1b = MatrixArffReader.read("data/dataset.arff")

// Read from a URL
Matrix data2 = MatrixArffReader.read(new URL("https://example.com/dataset.arff"))

// Read from a URL string
Matrix data2b = MatrixArffReader.readFromUrl("https://example.com/dataset.arff")

// Read from an InputStream (useful for classpath resources)
InputStream stream = getClass().getResourceAsStream("/data/iris.arff")
Matrix data3 = MatrixArffReader.read(stream, "iris")

// Read from a String containing ARFF content
String arffContent = "... ARFF ..."
Matrix data4 = MatrixArffReader.readString(arffContent)
```

### Exploring the Loaded Data

```groovy
import se.alipsa.matrix.arff.MatrixArffReader

Matrix iris = MatrixArffReader.read(new File("iris.arff"))

// Check column types
iris.columnNames().each { colName ->
    println "${colName}: ${iris.type(colName)}"
}

// View first few rows
println iris.head(5)
```

Output:
```
sepallength: class java.math.BigDecimal
sepalwidth: class java.math.BigDecimal
petallength: class java.math.BigDecimal
petalwidth: class java.math.BigDecimal
class: class java.lang.String

Matrix (iris, 5 x 5)
sepallength	sepalwidth	petallength	petalwidth	class
5.1        	3.5       	1.4        	0.2       	Iris-setosa
4.9        	3.0       	1.4        	0.2       	Iris-setosa
4.7        	3.2       	1.3        	0.2       	Iris-setosa
4.6        	3.1       	1.5        	0.2       	Iris-setosa
5.0        	3.6       	1.4        	0.2       	Iris-setosa
```

## ARFF Attribute Types

The ARFF format supports several attribute types, which are automatically converted to appropriate Java/Groovy types:

| ARFF Type | Description | Java Type |
|-----------|-------------|-----------|
| `NUMERIC` | Floating-point numbers | `BigDecimal` |
| `REAL` | Floating-point numbers | `BigDecimal` |
| `INTEGER` | Whole numbers | `Integer` |
| `STRING` | Text strings | `String` |
| `DATE` | Date/time values | `Date` |
| `{val1,val2,...}` | Nominal (categorical) | `String` |

**Note:** When writing a Matrix, `Long` and `BigInteger` columns are stored as `NUMERIC` and are read back as `BigDecimal`.

### Example ARFF with Various Types

```
@RELATION employees

@ATTRIBUTE name STRING
@ATTRIBUTE age INTEGER
@ATTRIBUTE salary REAL
@ATTRIBUTE hired DATE 'yyyy-MM-dd'
@ATTRIBUTE department {Engineering,Sales,HR,Marketing}
@ATTRIBUTE active {true,false}

@DATA
'John Doe',35,75000.50,'2020-01-15',Engineering,true
'Jane Smith',28,65000.00,'2021-06-01',Sales,true
'Bob Wilson',42,90000.00,'2015-03-20',Engineering,false
```

### Reading and Working with Types

```groovy
import se.alipsa.matrix.arff.MatrixArffReader

// Assuming the above ARFF content is in employees.arff
Matrix employees = MatrixArffReader.read(new File("employees.arff"))

// Access data with proper types
String name = employees[0, 'name']           // "John Doe"
Integer age = employees[0, 'age']            // 35
BigDecimal salary = employees[0, 'salary']   // 75000.50
Date hired = employees[0, 'hired']           // Date object
String dept = employees[0, 'department']     // "Engineering"

// Filter by department
def engineers = employees.subset { row ->
    row['department'] == 'Engineering'
}
println "Engineers: ${engineers.rowCount()}"
```

## Writing ARFF Files

The `MatrixArffWriter` class converts Matrix objects to ARFF format.

### Basic Writing

```groovy
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix

// Create a Matrix
Matrix employees = Matrix.builder('employees')
    .data(
        name: ['Alice', 'Bob', 'Charlie'],
        age: [25, 30, 35],
        salary: [50000.0, 60000.0, 75000.0],
        department: ['Engineering', 'Sales', 'Engineering']
    )
    .types([String, Integer, BigDecimal, String])
    .build()

// Write to file
MatrixArffWriter.write(employees, new File("employees.arff"))
```

The generated ARFF file will look like:

```
@RELATION employees

@ATTRIBUTE name STRING
@ATTRIBUTE age INTEGER
@ATTRIBUTE salary REAL
@ATTRIBUTE department {Engineering,Sales}

@DATA
'Alice',25,50000.0,Engineering
'Bob',30,60000.0,Sales
'Charlie',35,75000.0,Engineering
```

### Writing to Different Destinations

```groovy
import se.alipsa.matrix.arff.MatrixArffWriter
import java.nio.file.Paths

// Write to a Path
MatrixArffWriter.write(matrix, Paths.get("output/data.arff"))

// Write to an OutputStream
FileOutputStream fos = new FileOutputStream("output.arff")
MatrixArffWriter.write(matrix, fos)
fos.close()

// Write to a Writer (for custom handling)
StringWriter sw = new StringWriter()
MatrixArffWriter.write(matrix, sw)
String arffContent = sw.toString()
```

### Handling Null Values

Null values in the Matrix are automatically converted to the ARFF missing value indicator `?`:

```groovy
Matrix data = Matrix.builder('data')
    .data(
        name: ['Alice', 'Bob', null],
        age: [25, null, 35],
        salary: [50000.0, 60000.0, null]
    )
    .types([String, Integer, BigDecimal])
    .build()

MatrixArffWriter.write(data, new File("data_with_nulls.arff"))
```

Output in ARFF:
```
@DATA
'Alice',25,50000.0
'Bob',?,60000.0
?,35,?
```

## Custom Nominal Mappings

By default, the writer automatically detects categorical columns based on the number of unique values. You can also explicitly define which columns should be treated as nominal and what their allowed values are.

### Automatic Detection

The writer uses these heuristics to determine if a String column should be nominal:
- The column has unique values
- The number of unique values is 50 or fewer
- The unique values represent less than 10% of the total rows (or the dataset has fewer than 10 rows)

### Explicit Nominal Mappings

```groovy
import se.alipsa.matrix.arff.MatrixArffWriter

Matrix data = Matrix.builder('survey')
    .data(
        respondent: ['R001', 'R002', 'R003'],
        rating: ['Good', 'Excellent', 'Good'],
        category: ['A', 'B', 'A']
    )
    .types([String, String, String])
    .build()

// Define explicit nominal values (including values not in current data)
Map<String, List<String>> nominalMappings = [
    rating: ['Poor', 'Fair', 'Good', 'Excellent'],
    category: ['A', 'B', 'C', 'D']
]

// Write with custom mappings - respondent will be STRING, others NOMINAL
MatrixArffWriter.write(data, new File("survey.arff"), nominalMappings)
```

Generated ARFF:
```
@RELATION survey

@ATTRIBUTE respondent STRING
@ATTRIBUTE rating {Poor,Fair,Good,Excellent}
@ATTRIBUTE category {A,B,C,D}

@DATA
'R001',Good,A
'R002',Excellent,B
'R003',Good,A
```

This is useful when:
- You need to ensure consistent category encoding across different datasets
- Your current data doesn't contain all possible values
- You want to override the automatic detection

## Working with Dates

The matrix-arff module supports date handling with customizable formats.

### Default Date Format

The default date format is ISO 8601: `yyyy-MM-dd'T'HH:mm:ss`

### Reading Dates

```groovy
// ARFF file with dates:
// @ATTRIBUTE event_date DATE 'yyyy-MM-dd'
// @DATA
// '2024-01-15'

Matrix events = MatrixArffReader.read(new File("events.arff"))
Date eventDate = events[0, 'event_date']
println eventDate  // Mon Jan 15 00:00:00 CET 2024
```

### Writing Dates

The writer supports various Java date types:

```groovy
import java.time.LocalDate
import java.time.LocalDateTime

Matrix events = Matrix.builder('events')
    .data(
        event: ['Conference', 'Meeting', 'Workshop'],
        date: [new Date(), LocalDate.now(), LocalDateTime.now()]
    )
    .types([String, Date])
    .build()

MatrixArffWriter.write(events, new File("events.arff"))
```

Supported date types:
- `java.util.Date`
- `java.sql.Date`
- `java.sql.Timestamp`
- `java.time.LocalDate`
- `java.time.LocalDateTime`
- `java.time.Instant`

## Integration with Machine Learning

The matrix-arff module is particularly useful for machine learning workflows, enabling data interchange between Matrix and ML tools.

### Preparing Data for Weka

```groovy
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Normalize

// Load and preprocess data
Matrix raw = MatrixArffReader.read(new File("raw_data.arff"))

// Normalize numeric columns
def numericCols = ['feature1', 'feature2', 'feature3']
Matrix processed = raw
numericCols.each { col ->
    processed = processed.apply(col, BigDecimal) { row, val ->
        if (val == null) return null
        // Min-max normalization to 0-1 range
        def colData = raw[col] as List<BigDecimal>
        def min = colData.findAll { it != null }.min()
        def max = colData.findAll { it != null }.max()
        if (max == min) return 0.5
        (val - min) / (max - min)
    }
}

// Export for Weka
MatrixArffWriter.write(processed, new File("normalized_data.arff"))
```

### Pipeline: ARFF to Smile to ARFF

```groovy
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.smile.SmileUtil
import smile.data.DataFrame

// Read ARFF data
Matrix trainingData = MatrixArffReader.read(new File("train.arff"))

// Convert to Smile DataFrame for ML operations
DataFrame df = SmileUtil.toDataFrame(trainingData)

// Perform ML operations with Smile...
// (training, prediction, etc.)

// Convert results back to Matrix
Matrix results = SmileUtil.toMatrix(df)

// Export as ARFF for further processing or sharing
MatrixArffWriter.write(results, new File("results.arff"))
```

### Round-Trip Conversion

You can read an ARFF file, manipulate it with Matrix operations, and write it back:

```groovy
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter

// Read iris dataset
Matrix iris = MatrixArffReader.read(new File("iris.arff"))

// Add a new calculated column
iris = iris.addColumn('sepal_ratio', BigDecimal) { row ->
    row['sepallength'] / row['sepalwidth']
}

// Filter to specific species
Matrix setosa = iris.subset { row ->
    row['class'] == 'Iris-setosa'
}

// Write the filtered and enhanced dataset
MatrixArffWriter.write(setosa, new File("iris_setosa_enhanced.arff"))
```

## Complete Example

Here's a comprehensive example that demonstrates reading, processing, and writing ARFF data:

```groovy
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

// 1. Read the iris dataset
Matrix iris = MatrixArffReader.read(new File("iris.arff"))
println "Loaded ${iris.rowCount()} samples with ${iris.columnCount()} features"

// 2. Explore the data
println "\nColumn types:"
iris.columnNames().each { col ->
    println "  ${col}: ${iris.type(col).simpleName}"
}

// 3. Calculate statistics for numeric columns
println "\nStatistics by species:"
def species = iris['class'].unique()
species.each { sp ->
    def subset = iris.subset { it['class'] == sp }
    println "\n${sp}:"
    println "  Count: ${subset.rowCount()}"
    println "  Avg sepal length: ${Stat.mean(subset['sepallength']).round(2)}"
    println "  Avg petal length: ${Stat.mean(subset['petallength']).round(2)}"
}

// 4. Create a summary dataset
Matrix summary = Matrix.builder('iris_summary')
    .data(
        species: species,
        count: species.collect { sp ->
            iris.subset { it['class'] == sp }.rowCount()
        },
        avg_sepal_length: species.collect { sp ->
            def subset = iris.subset { it['class'] == sp }
            Stat.mean(subset['sepallength']).round(2)
        },
        avg_petal_length: species.collect { sp ->
            def subset = iris.subset { it['class'] == sp }
            Stat.mean(subset['petallength']).round(2)
        }
    )
    .types([String, Integer, BigDecimal, BigDecimal])
    .build()

println "\nSummary:"
println summary

// 5. Write the summary to ARFF
MatrixArffWriter.write(summary, new File("iris_summary.arff"))
println "\nSummary written to iris_summary.arff"
```

Output:
```
Loaded 150 samples with 5 features

Column types:
  sepallength: BigDecimal
  sepalwidth: BigDecimal
  petallength: BigDecimal
  petalwidth: BigDecimal
  class: String

Statistics by species:

Iris-setosa:
  Count: 50
  Avg sepal length: 5.01
  Avg petal length: 1.46

Iris-versicolor:
  Count: 50
  Avg sepal length: 5.94
  Avg petal length: 4.26

Iris-virginica:
  Count: 50
  Avg sepal length: 6.59
  Avg petal length: 5.55

Summary:
Matrix (iris_summary, 3 x 4)
species        	count	avg_sepal_length	avg_petal_length
Iris-setosa    	50   	5.01            	1.46
Iris-versicolor	50   	5.94            	4.26
Iris-virginica 	50   	6.59            	5.55

Summary written to iris_summary.arff
```

## Conclusion

The matrix-arff module provides seamless integration between the Matrix library and the ARFF file format. This enables:

- Easy loading of standard ML datasets
- Data interchange with Weka and other ML tools
- Preprocessing data with Matrix operations before ML workflows
- Exporting results in a format compatible with other tools

Combined with matrix-smile for machine learning operations, matrix-arff forms a complete pipeline for loading, processing, training, and saving ML data.

In the next section, we'll explore the matrix-smile module which provides integration with the Smile machine learning library for classification, regression, clustering, and more.

Go to [previous section](15-analysis-workflow.md) | Go to [next section](17-matrix-smile.md) | Back to [outline](outline.md)
