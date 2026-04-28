[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-json/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-json)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-json/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-json)
# matrix-json
JSON import and export functionality to and from a Matrix.

## Setup
Matrix-json should work with any 5.x version of Groovy.
It requires version 2.0.0 or later of the Matrix-core package (recommend se.alipsa.groovy:matrix-core:3.6.0).
Binary builds can be downloaded
from the [Matrix-json project release page](https://github.com/Alipsa/matrix-json/releases) but if you use a build system that
handles dependencies via maven central (gradle, maven ivy etc.) you can do the following for Gradle
```groovy
implementation 'org.apache.groovy:groovy:5.0.5'
implementation 'se.alipsa.matrix:matrix-core:3.6.0'
implementation 'se.alipsa.matrix:matrix-json:2.2.0'
```
...and the following for Maven
```xml
<dependencies>
  <dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>5.0.5</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.6.0</version>
  </dependency>  
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-json</artifactId>
      <version>2.2.0</version>
  </dependency>
</dependencies>
```

The JVM should be JDK 21 or higher.

Note: `groovy-json` is no longer required as a dependency. Since v2.2.0, matrix-json uses
Jackson for both reading and writing JSON.

## Using Matrix.read() / matrix.write()

If `matrix-json` is on the classpath, `.json` files can be handled through the generic Matrix API:

```groovy
import se.alipsa.matrix.core.Matrix

// Read JSON from a file
Matrix data = Matrix.read(new File('data.json'))

// Read with options
Matrix utf16 = Matrix.read([charset: 'UTF-16'], new File('data.json'))

// Read with type conversion
Matrix typed = Matrix.read([types: [Integer, String, LocalDate], dateTimeFormat: 'yyyy-MM-dd'], new File('data.json'))

// Write JSON to a file
data.write([indent: true], new File('pretty.json'))
data.write([dateFormat: 'yyyy/MM/dd'], new File('custom.json'))

// Discover available options
println Matrix.listReadOptions('json')
println Matrix.listWriteOptions('json')
```

Note: from `matrix-json` 2.1.3 onward, JSON floating-point values are read as `BigDecimal` instead
of `Double` so decimal values preserve their exact text precision when imported through
`JsonReader` / `Matrix.read(...)`.

## Writing JSON with JsonWriter

The recommended way to write JSON is via the fluent `WriteBuilder` API:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonWriter
import java.time.LocalDate
import static se.alipsa.matrix.core.ListConverter.toLocalDates

def empData = Matrix.builder().data(
        emp_id: 1..3,
        emp_name: ["Rick","Dan","Michelle"],
        salary: [623.3,515.2,611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
    .types(int, String, Number, LocalDate)
    .build()

// Write to a file with pretty-printing
JsonWriter.write(empData).indent().to(new File('employees.json'))

// Write to a string
String json = JsonWriter.write(empData).indent().asString()
println json
```
will output
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

### Custom formatting

You can apply column formatters and date format patterns via the builder:

```groovy
import se.alipsa.matrix.json.JsonWriter

// Custom date format
String json = JsonWriter.write(empData)
    .dateFormat('yy/dd/MM')
    .indent()
    .asString()

// Column formatters
String json = JsonWriter.write(empData)
    .formatter('salary') { it * 10 + ' kr' }
    .formatter('start_date') { DateTimeFormatter.ofPattern('yy/dd/MM').format(it) }
    .indent()
    .asString()
```

### Write targets

The builder supports multiple output targets:

```groovy
JsonWriter.write(matrix).to(new File('out.json'))         // File
JsonWriter.write(matrix).to(Path.of('out.json'))          // Path
JsonWriter.write(matrix).to('/path/to/out.json')          // String path
JsonWriter.write(matrix).to(writer)                       // Writer (streams directly)
String json = JsonWriter.write(matrix).asString()         // String
```

## Reading JSON with JsonReader

`JsonReader` reads JSON arrays into a Matrix using Jackson's streaming API for constant memory usage:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader
import java.time.LocalDate

// Read from a string
Matrix table = JsonReader.read('''[
    {"emp_id": 1, "emp_name": "Rick", "salary": 623.3, "start_date": "2012-01-01"},
    {"emp_id": 2, "emp_name": "Dan", "salary": 515.2, "start_date": "2013-09-23"},
    {"emp_id": 3, "emp_name": "Michelle", "salary": 611.0, "start_date": "2014-11-15"}
]''')

// Convert columns to specific types
Matrix typed = table.convert([int, String, Number, LocalDate])
```

### Reading from various sources

```groovy
Matrix m = JsonReader.read(new File('data.json'))                              // File
Matrix m = JsonReader.read(Path.of('data.json'))                               // Path
Matrix m = JsonReader.readFile('/path/to/data.json')                           // String file path
Matrix m = JsonReader.read(new URL('https://api.example.com/data.json'))       // URL
Matrix m = JsonReader.readUrl('https://api.example.com/data.json')             // String URL
Matrix m = JsonReader.read(inputStream)                                        // InputStream
Matrix m = JsonReader.read(reader)                                             // Reader
Matrix m = JsonReader.readString('[{"id":1}]')                                 // Alias for read(String)
```

When reading from a `File` or `URL`, the matrix name is automatically derived from the filename
(e.g., `employees.json` produces a matrix named `"employees"`).

### Nested JSON

Nested objects are automatically flattened to dot-notation keys. Arrays use bracket notation:

```groovy
// {"person": {"name": "Alice"}, "scores": [90, 95]}
// becomes columns: person.name, scores[0], scores[1]

Matrix m = JsonReader.read('[{"person": {"name": "Alice"}, "scores": [90, 95]}]')
println m.columnNames()  // [person.name, scores[0], scores[1]]
```

## Deprecated API

The `JsonExporter` and `JsonImporter` classes are deprecated since v2.1.2.
Use `JsonWriter` and `JsonReader` instead.

# Release version compatibility

The simplest way to get compatible versions of all matrix modules is to use the
[matrix-bom](https://github.com/Alipsa/matrix/tree/main/matrix-bom):

```groovy
implementation platform('se.alipsa.matrix:matrix-bom:2.4.0')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-json'
```

For reference, the following table shows the version compatibility of matrix-json and matrix-core:

| Matrix json |    Matrix core | 
|------------:|---------------:|
|       1.0.0 | 2.0.0 -> 2.1.1 |
|       1.1.0 | 2.2.0 -> 2.2.1 |
|       2.0.0 |          3.0.0 |
|       2.1.0 | 3.1.0 -> 3.3.0 |
