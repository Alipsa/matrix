# Matrix JSON Module

The matrix-json module provides functionality for reading JSON data into Matrix objects and writing Matrix objects to JSON format. This module is particularly useful when working with web APIs or when you need to exchange data with JavaScript applications.

## Installation

To use the matrix-json module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.5'
implementation platform('se.alipsa.matrix:matrix-bom:2.4.0')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-json'
```

### Maven Configuration

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.4.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>5.0.5</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
    </dependency>  
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-json</artifactId>
    </dependency>
  </dependencies>
</project>
```

Note that the matrix-json module requires JDK 21 or higher. The `groovy-json` dependency is no longer required since v2.2.0; matrix-json uses Jackson for both reading and writing.

## Writing Matrix to JSON

The `JsonWriter` class provides a fluent builder API for converting Matrix objects to JSON format.

### Basic JSON Export

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonWriter
import java.time.LocalDate
import static se.alipsa.matrix.core.ListConverter.toLocalDates

// Create a Matrix with employee data
def empData = Matrix.builder().data(
        emp_id: 1..3,
        emp_name: ["Rick", "Dan", "Michelle"],
        salary: [623.3, 515.2, 611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
    .types(Integer, String, Number, LocalDate)
    .build()

// Write to a file with pretty-printing
JsonWriter.write(empData).indent().to(new File('employees.json'))

// Or get the JSON as a string
String json = JsonWriter.write(empData).indent().asString()
println json
```

The output will be:

```json
[
    {
        "emp_id": 1,
        "emp_name": "Rick",
        "salary": 623.3,
        "start_date": "2012-01-01"
    },
    {
        "emp_id": 2,
        "emp_name": "Dan",
        "salary": 515.2,
        "start_date": "2013-09-23"
    },
    {
        "emp_id": 3,
        "emp_name": "Michelle",
        "salary": 611.0,
        "start_date": "2014-11-15"
    }
]
```

### Custom Data Transformation

You can apply formatters to individual columns and set custom date format patterns:

```groovy
import se.alipsa.matrix.json.JsonWriter
import java.time.format.DateTimeFormatter

// Custom date format for all temporal columns
String json = JsonWriter.write(empData)
    .dateFormat('yy/dd/MM')
    .indent()
    .asString()

// Per-column formatters
String json = JsonWriter.write(empData)
    .formatter('salary') { it * 10 + ' kr' }
    .formatter('start_date') { DateTimeFormatter.ofPattern('yy/dd/MM').format(it) }
    .indent()
    .asString()
println json
```

The output with column formatters will be:

```json
[
    {
        "emp_id": 1,
        "emp_name": "Rick",
        "salary": "6233.0 kr",
        "start_date": "12/01/01"
    },
    {
        "emp_id": 2,
        "emp_name": "Dan",
        "salary": "5152.0 kr",
        "start_date": "13/23/09"
    },
    {
        "emp_id": 3,
        "emp_name": "Michelle",
        "salary": "6110.0 kr",
        "start_date": "14/15/11"
    }
]
```

### Writing to Different Targets

The builder supports multiple output targets:

```groovy
import se.alipsa.matrix.json.JsonWriter

// Write to a File
JsonWriter.write(data).indent().to(new File('data.json'))

// Write to a Path
JsonWriter.write(data).to(Path.of('data.json'))

// Write to a String file path
JsonWriter.write(data).to('/path/to/data.json')

// Write directly to a Writer (streams without intermediate String)
JsonWriter.write(data).to(writer)

// Get as a String
String json = JsonWriter.write(data).asString()
```

## Reading JSON into a Matrix

The `JsonReader` class reads JSON arrays into Matrix objects using Jackson's streaming API, which provides constant memory usage regardless of JSON size.

### Basic JSON Import

The JSON needs to be in the format of a list (`[]`) with each row represented as an object (`{}`):

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader
import java.time.LocalDate

// Read from a JSON string
Matrix table = JsonReader.read('''[
    {"emp_id": 1, "emp_name": "Rick", "salary": 623.3, "start_date": "2012-01-01"},
    {"emp_id": 2, "emp_name": "Dan", "salary": 515.2, "start_date": "2013-09-23"},
    {"emp_id": 3, "emp_name": "Michelle", "salary": 611.0, "start_date": "2014-11-15"}
]''')

// Convert columns to appropriate types
Matrix typedTable = table.convert([Integer, String, Number, LocalDate])
println typedTable.content()
```

### Reading from Files and URLs

```groovy
import se.alipsa.matrix.json.JsonReader

// Read from a File (matrix name is derived from filename, e.g. "employees")
Matrix m = JsonReader.read(new File('employees.json'))

// Read from a Path
Matrix m = JsonReader.read(Path.of('data.json'))

// Read from a file path string
Matrix m = JsonReader.readFile('/path/to/data.json')

// Read from a URL
Matrix m = JsonReader.read(new URL('https://api.example.com/data.json'))

// Read from a URL string
Matrix m = JsonReader.readUrl('https://api.example.com/data.json')

// Read from an InputStream or Reader
Matrix m = JsonReader.read(inputStream)
Matrix m = JsonReader.read(reader)
```

### Type Conversion via SPI

When using the generic `Matrix.read()` API, you can specify types and date format directly:

```groovy
import se.alipsa.matrix.core.Matrix
import java.time.LocalDate

Matrix typed = Matrix.read(
    [types: [Integer, String, Number, LocalDate], dateTimeFormat: 'yyyy-MM-dd'],
    new File('employees.json')
)
```

### Nested JSON

Nested objects are automatically flattened to dot-notation keys, and arrays use bracket notation:

```groovy
Matrix m = JsonReader.read('''[
    {"person": {"name": "Alice", "age": 30}, "scores": [90, 95]}
]''')

println m.columnNames()  // [person.name, person.age, scores[0], scores[1]]
```

Duplicate keys after flattening (e.g., both `"a.b"` as a literal key and `"a": {"b": ...}`)
will throw an `IllegalArgumentException`.

## Working with Web APIs

The matrix-json module is useful when working with web APIs that return JSON data:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader

// Read directly from a URL
Matrix data = JsonReader.readUrl('https://api.example.com/data.json')

// Process the data
println "Imported ${data.rowCount()} rows of data"
println data.head()
```

## Best Practices

1. **Data Types**: When reading JSON, values are imported as their JSON types (String, BigDecimal, Boolean, etc.). Use the `convert` method or the `types` read option to convert columns to appropriate Groovy types.

2. **BigDecimal Precision**: Since v2.1.3, JSON floating-point values are read as `BigDecimal` instead of `Double`, preserving exact text precision.

3. **Date Formatting**: When writing dates, they are formatted using `yyyy-MM-dd` by default. Use `.dateFormat(pattern)` on the builder for custom formats.

4. **Nested JSON**: Nested objects and arrays are automatically flattened. For deeply nested structures, check the resulting column names after reading.

5. **Memory Efficiency**: Both `JsonReader` and `JsonWriter` use Jackson streaming, keeping memory usage constant regardless of data size.

## Deprecated API

The `JsonImporter` and `JsonExporter` classes are deprecated since v2.1.2. Use `JsonReader` and `JsonWriter` instead.

Go to [previous section](6-matrix-csv.md) | Go to [next section](8-matrix-xchart.md) | Back to [outline](outline.md)
