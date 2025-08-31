# Matrix JSON Module

The matrix-json module provides functionality for importing JSON data into Matrix objects and exporting Matrix objects to JSON format. This module is particularly useful when working with web APIs or when you need to exchange data with JavaScript applications.

## Installation

To use the matrix-json module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
def groovyVersion = '5.0.0' // any 4.x version should work
implementation "org.apache.groovy:groovy:$groovyVersion"
implementation "org.apache.groovy:groovy-json:$groovyVersion"
implementation platform('se.alipsa.matrix:matrix-bom:2.2.3')
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
        <version>2.2.3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>5.0.0</version>
    </dependency>
    <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy-json</artifactId>
      <version>5.0.0</version>
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

Note that the matrix-json module requires JDK 21 or higher.

## Exporting Matrix to JSON

The matrix-json module provides a `JsonExporter` class that makes it easy to convert Matrix objects to JSON format.

### Basic JSON Export

Here's a simple example of exporting a Matrix to JSON:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonExporter
import java.time.LocalDate
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import groovy.json.JsonOutput

// Create a Matrix with employee data
def empData = Matrix.builder().data(
        emp_id: 1..3,
        emp_name: ["Rick", "Dan", "Michelle"],
        salary: [623.3, 515.2, 611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
    .types(Integer, String, Number, LocalDate)
    .build()

// Create a JsonExporter and convert the Matrix to JSON
def exporter = new JsonExporter(empData)
def json = exporter.toJson()

// Pretty print the JSON for better readability
println JsonOutput.prettyPrint(json)
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

Sometimes you need to transform the data in specific ways before exporting to JSON. The `toJson` method accepts a map of closures that can be used to transform the data for each column:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixjson.JsonExporter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import groovy.json.JsonOutput

// Create a Matrix with employee data
def empData = Matrix.builder().data(
        emp_id: 1..3,
        emp_name: ["Rick", "Dan", "Michelle"],
        salary: [623.3, 515.2, 611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
    .types(Integer, String, Number, LocalDate)
    .build()

// Create a JsonExporter
def exporter = new JsonExporter(empData)

// Define custom transformations for specific columns
DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern('yy/dd/MM')
def json = exporter.toJson([
        'salary': {it * 10 + ' kr'}, 
        'start_date': {dateTimeFormatter.format(it)}
])

// Pretty print the JSON
println JsonOutput.prettyPrint(json)
```

The output will be:

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

### Exporting to a File

You can also export the JSON to a file:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixjson.JsonExporter
import groovy.json.JsonOutput
import java.nio.file.Files
import java.nio.file.Paths

// Create a Matrix
def data = Matrix.builder().data(
    id: [1, 2, 3],
    name: ["Alice", "Bob", "Charlie"],
    score: [85, 92, 78]
).build()

// Export to JSON
def exporter = new JsonExporter(data)
def json = exporter.toJson()

// Pretty print and write to file
def prettyJson = JsonOutput.prettyPrint(json)
def file = new File("data.json")
file.text = prettyJson

println("JSON exported to: ${file.absolutePath}")
```

## Importing JSON into a Matrix

The matrix-json module also provides a `JsonImporter` class for importing JSON data into a Matrix object.

### Basic JSON Import

The JSON needs to be in the format of a list (`[]`) with each row represented as an object (`{}`):

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixjson.*
import java.time.LocalDate

// Create a JsonImporter
def importer = new JsonImporter()

// Parse JSON string into a Matrix
def table = importer.parse('''[
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
    ]''')

// Convert columns to appropriate types
def typedTable = table.convert([Integer, String, Number, LocalDate])

// Print the imported Matrix
println(typedTable.content())
```

### Importing from a File

You can also import JSON from a file:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixjson.*
import java.time.LocalDate

// Create a JsonImporter
def importer = new JsonImporter()

// Read JSON from a file
def file = new File("data.json")
def jsonString = file.text

// Parse JSON into a Matrix
def table = importer.parse(jsonString)

// Convert columns to appropriate types if needed
def typedTable = table.convert([
    "id": Integer,
    "name": String,
    "score": Integer
])

// Print the imported Matrix
println(typedTable.content())
```

### Complex Type Conversion

For more complex scenarios, you can use different conversion methods:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixjson.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

// Create a JsonImporter
def importer = new JsonImporter()

// Parse JSON string into a Matrix
def table = importer.parse('''[
        {
          "id": 1,
          "name": "Product A",
          "price": "1.234,56",
          "date": "2023-01-15"
        },
        {
          "id": 2,
          "name": "Product B",
          "price": "2.345,67",
          "date": "2023-02-20"
        }
    ]''')

// Convert with specific formatters for dates and numbers
def typedTable = table.convert(
    [
        "id": Integer,
        "name": String,
        "price": BigDecimal,
        "date": LocalDate
    ],
    "yyyy-MM-dd",  // Date format
    NumberFormat.getInstance(Locale.GERMANY)  // For parsing numbers with comma as decimal separator
)

// Print the imported Matrix
println(typedTable.content())
```

## Working with Web APIs

The matrix-json module is particularly useful when working with web APIs that return JSON data:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixjson.*
import groovy.json.JsonSlurper

// Make an HTTP request to a JSON API
def url = new URL("https://api.example.com/data")
def connection = url.openConnection()
connection.requestMethod = "GET"
connection.setRequestProperty("Accept", "application/json")

// Read the response
def responseCode = connection.responseCode
if (responseCode == 200) {
    def jsonText = connection.inputStream.text
    
    // Parse the JSON into a Matrix
    def importer = new JsonImporter()
    def matrix = importer.parse(jsonText)
    
    // Process the data
    println("Imported ${matrix.rowCount()} rows of data")
    println(matrix.head())
} else {
    println("HTTP request failed with response code: ${responseCode}")
}
```

## Best Practices

1. **Data Types**: When importing JSON, all values are initially imported as their JSON types (String, Number, Boolean, etc.). Use the `convert` method to convert columns to appropriate Groovy types.

2. **Date Formatting**: When exporting dates to JSON, they are converted to ISO-8601 format by default. Use custom transformations if you need different date formats.

3. **Nested JSON**: The current implementation works best with flat JSON structures. For nested JSON, you may need to pre-process the data before importing.

4. **Large JSON Files**: For large JSON files, consider streaming the JSON or processing it in chunks to avoid memory issues.

5. **Error Handling**: Implement proper error handling when parsing JSON, as malformed JSON will cause exceptions.

## Conclusion

The matrix-json module provides a convenient way to work with JSON data in your Groovy applications. Whether you're consuming data from web APIs or need to exchange data with JavaScript applications, this module makes it easy to convert between Matrix objects and JSON format.

In the next section, we'll explore the matrix-xchart module, which provides functionality for creating charts and visualizations from Matrix data.

Go to [previous section](6-matrix-csv.md) | Go to [next section](8-matrix-xchart.md) | Back to [outline](outline.md)