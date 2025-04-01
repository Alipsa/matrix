# Matrix Tablesaw Module

The Matrix Tablesaw module provides interoperability between the Matrix library and the Tablesaw library, along with various extensions to Tablesaw such as BigDecimalColumn, GTable (which makes Tablesaw Groovier), and complementary operations for working with Tablesaw data.

> **Note**: As of the time of writing, this module is still a work in progress, and no release version has been published yet.

## What is Tablesaw?

[Tablesaw](https://github.com/jtablesaw/tablesaw) is a Java library for data manipulation and analysis. It provides a DataFrame-like API for working with tabular data, similar to pandas in Python or data.frame in R. Tablesaw offers powerful features for data transformation, filtering, aggregation, and visualization.

## Installation

To use the matrix-tablesaw module, you need to add it as a dependency to your project.

### Gradle Configuration (Future)

```groovy
implementation platform('se.alipsa.matrix:matrix-bom:2.2.0')
implementation 'se.alipsa.matrix:core'
implementation 'se.alipsa.matrix:matrix-tablesaw'
```

### Maven Configuration (Future)

```xml
<dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-tablesaw</artifactId>
</dependency>
<project>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-bom</artifactId>
      <version>2.2.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-tablesaw</artifactId>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-core</artifactId>
  </dependency>
</dependencies>
</project>
```

## Key Components of the Matrix Tablesaw Module

The matrix-tablesaw module consists of several key components:

1. **TableUtil**: Provides utility methods for working with Tablesaw tables
2. **GTable**: A Groovy-friendly wrapper around Tablesaw's Table class
3. **Normalizer**: Utilities for normalizing Tablesaw columns
4. **BigDecimalColumn**: Support for BigDecimal data type in Tablesaw

Let's explore each of these components in detail.

## TableUtil

The `TableUtil` class provides utility methods for working with Tablesaw tables. Some of the key functionalities include:

### Creating Frequency Tables

Frequency tables are useful for counting occurrences of values in a column:

```groovy
import se.alipsa.matrix.tablesaw.TableUtil
import tech.tablesaw.api.Table
import tech.tablesaw.api.StringColumn

// Create a sample table
def data = Table.create("Sample")
def names = StringColumn.create("Name", ["Alice", "Bob", "Alice", "Charlie", "Bob", "Alice"])
data.addColumns(names)

// Create a frequency table for the "Name" column
def freqTable = TableUtil.frequency(data, "Name")

// Print the frequency table
println freqTable
```

The resulting frequency table will have columns for the value, frequency count, and percentage:

```
Value    | Frequency | Percent
---------|-----------|--------
Alice    | 3         | 50.0
Bob      | 2         | 33.33
Charlie  | 1         | 16.67
```

### Rounding Numbers

The `TableUtil` class provides methods for rounding numbers with a specified number of decimal places:

```groovy
import se.alipsa.matrix.tablesaw.TableUtil
import tech.tablesaw.api.Table
import tech.tablesaw.api.DoubleColumn

// Create a sample table with decimal values
def data = Table.create("Sample")
def values = DoubleColumn.create("Value", [1.23456, 2.34567, 3.45678])
data.addColumns(values)

// Round the "Value" column to 2 decimal places
def roundedColumn = TableUtil.round(data.column("Value"), 2)

// Replace the original column with the rounded one
data.replaceColumn("Value", roundedColumn)

println data
```

## GTable

The `GTable` class is a Groovy-friendly wrapper around Tablesaw's `Table` class. It provides a more idiomatic Groovy API for working with tables, including operator overloading and simplified syntax.

### Creating a GTable

You can create a `GTable` from a Tablesaw `Table` or directly from data:

```groovy
import se.alipsa.matrix.tablesaw.gtable.GTable
import tech.tablesaw.api.Table

// Create a GTable from a Tablesaw Table
def table = Table.create("Sample")
// ... add columns to table
def gTable = new GTable(table)

// Or create a GTable directly
def directGTable = GTable.create("Sample")
    .addStringColumn("Name", ["Alice", "Bob", "Charlie"])
    .addDoubleColumn("Score", [85.5, 92.3, 78.9])
```

### Accessing Data with Groovy Syntax

`GTable` allows you to access data using Groovy's subscript operator:

```groovy
import se.alipsa.matrix.tablesaw.gtable.GTable

// Create a sample GTable
def gTable = GTable.create("Students")
    .addStringColumn("Name", ["Alice", "Bob", "Charlie"])
    .addDoubleColumn("Score", [85.5, 92.3, 78.9])

// Access data using row and column indices
def value = gTable[0, 1]  // Value at first row, second column (85.5)

// Access data using row index and column name
def bobScore = gTable[1, "Score"]  // 92.3

// Modify data using the subscript operator
gTable[2, "Score"] = 80.0  // Update Charlie's score
```

### Converting Between GTable and Matrix

The `GTable` class provides methods for converting between `GTable` and `Matrix`:

```groovy
import se.alipsa.matrix.tablesaw.gtable.GTable
import se.alipsa.matrix.core.Matrix

// Create a sample GTable
def gTable = GTable.create("Students")
    .addStringColumn("Name", ["Alice", "Bob", "Charlie"])
    .addDoubleColumn("Score", [85.5, 92.3, 78.9])

// Convert GTable to Matrix
def matrix = TableUtil.toMatrix(gTable)

// Convert Matrix back to GTable
def newGTable = TableUtil.fromMatrix(matrix)
```

## Normalizer

The `Normalizer` class provides utilities for normalizing Tablesaw columns. Normalization is the process of scaling 
numeric values to a standard range, which is useful for many machine learning algorithms.

### Min-Max Normalization

Min-max normalization scales values to a range between 0 and 1:

```groovy
import se.alipsa.matrix.tablesaw.Normalizer
import tech.tablesaw.api.Table
import tech.tablesaw.api.DoubleColumn

// Create a sample table
def data = Table.create("Sample")
def values = DoubleColumn.create("Value", [10, 20, 30, 40, 50])
data.addColumns(values)

// Normalize the "Value" column using min-max normalization
def normalizedColumn = Normalizer.minMaxNorm(data.column("Value"))

// Replace the original column with the normalized one
data.replaceColumn("Value", normalizedColumn)

println data
```

### Z-Score Normalization

Z-score normalization scales values based on the mean and standard deviation:

```groovy
import se.alipsa.matrix.tablesaw.Normalizer
import tech.tablesaw.api.Table
import tech.tablesaw.api.DoubleColumn

// Create a sample table
def data = Table.create("Sample")
def values = DoubleColumn.create("Value", [10, 20, 30, 40, 50])
data.addColumns(values)

// Normalize the "Value" column using z-score normalization
def normalizedColumn = Normalizer.stdScaleNorm(data.column("Value"))

// Replace the original column with the normalized one
data.replaceColumn("Value", normalizedColumn)

println data
```

## BigDecimalColumn

The matrix-tablesaw module adds support for `BigDecimal` data type in Tablesaw through the `BigDecimalColumn` class. This is particularly useful for financial calculations where precision is important.

```groovy
import se.alipsa.matrix.tablesaw.BigDecimalColumn
import tech.tablesaw.api.Table
import java.math.BigDecimal

// Create a sample table
def data = Table.create("Financial")

// Create a BigDecimalColumn
def amounts = BigDecimalColumn.create("Amount", [
    new BigDecimal("1234.56"),
    new BigDecimal("2345.67"),
    new BigDecimal("3456.78")
])

// Add the column to the table
data.addColumns(amounts)

println data
```

## Complete Example: Data Analysis with Matrix and Tablesaw

Here's a complete example that demonstrates how to use the Matrix Tablesaw module for data analysis:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.gtable.Gtable
import se.alipsa.matrix.tablesaw.TableUtil
import se.alipsa.matrix.tablesaw.Normalizer
import tech.tablesaw.api.*

// Create a Matrix with sample data
def matrix = Matrix.builder().data(
    name: ["Alice", "Bob", "Charlie", "David", "Eve"],
    age: [25, 30, 35, 40, 45],
    salary: [50000, 60000, 70000, 80000, 90000],
    department: ["HR", "IT", "Finance", "IT", "HR"]
).types(String, Integer, BigDecimal, String)
    .build()

// Convert Matrix to GTable
def gTable = TableUtil.fromMatrix(matrix)

// Calculate average salary by department
def deptSalary = gTable.summarize("salary", BigDecimalAggregateFunctions.mean)
    .by("department")

println "Average salary by department:"
println deptSalary

// Create a frequency table for the department column
def deptFreq = TableUtil.frequency(gTable, "department")

println "\nDepartment frequency:"
println deptFreq

// Normalize the salary column
var salaryCol = gTable.column("salary") as BigDecimalColumn
def normalizedSalary = Normalizer.minMaxNorm(salaryCol)

// Replace the original column with the normalized one
gTable.replaceColumn("salary", normalizedSalary)

println "\nData with normalized salaries:"
println gTable

// Convert back to Matrix for further analysis
def newMatrix = TableUtil.toMatrix(gTable)

println "\nConverted back to Matrix:"
println newMatrix.content()
```

## Best Practices

When working with the Matrix Tablesaw module, consider these best practices:

1. **Choose the Right Tool**: Use Matrix for simpler tabular data operations and Tablesaw for more complex data manipulation and analysis.

2. **Convert When Needed**: Convert between Matrix and GTable only when necessary, as conversions may have performance implications for large datasets.

3. **Use GTable for Groovy Code**: When working in Groovy, prefer GTable over raw Tablesaw Tables for a more idiomatic experience.

4. **Normalize Data Appropriately**: Choose the appropriate normalization method based on your data and analysis requirements.

5. **Handle Missing Values**: Be aware of how both libraries handle missing values and ensure consistent treatment across conversions.

## Conclusion

The Matrix Tablesaw module bridges the gap between the Matrix library and the Tablesaw library, providing a powerful combination for data manipulation and analysis in Groovy. While still in development, it offers valuable extensions to Tablesaw and seamless interoperability with Matrix.

By leveraging both libraries through this module, you can take advantage of Matrix's simplicity and Tablesaw's advanced data manipulation capabilities in a unified and Groovy-friendly way.

In future releases, we can expect more features and improvements to enhance the integration between these two powerful data libraries.

Go to [previous section](13-matrix-charts.md) | Go to [next section](15-analysis-workflow.md) | Back to [outline](outline.md)