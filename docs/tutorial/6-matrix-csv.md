# Matrix CSV Module

The matrix-csv module provides comprehensive support for creating Matrix objects from structured text files (CSV files) and writing Matrix objects to CSV files in the format of choice. It leverages Apache Commons CSV for parsing and writing CSV files, providing flexibility in handling various CSV formats.

## Installation

To use the matrix-csv module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'se.alipsa.matrix:matrix-core:3.0.0'
implementation 'se.alipsa.matrix:matrix-csv:2.0.0'
```

### Maven Configuration

```xml
<dependencies>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.0.0</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-csv</artifactId>
      <version>2.0.0</version>
  </dependency>
</dependencies>
```

## Importing CSV Files

The matrix-csv module uses Apache Commons CSV to parse CSV files. This provides a high degree of flexibility in handling different CSV formats, including custom delimiters, quote characters, and header configurations.

### Basic CSV Import

Here's a simple example of importing a basic CSV file:

```groovy
import org.apache.commons.csv.CSVFormat
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvImporter

// Import a CSV file with default settings
URL url = getClass().getResource("/basic.csv")
CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
Matrix basic = CsvImporter.importCsv(url, format)

// Print the imported data
println(basic.content())
```

### Customizing CSV Import

For more complex CSV files, you can customize the CSV format using the Apache Commons CSV builder:

```groovy
import org.apache.commons.csv.CSVFormat
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvImporter

// Import a CSV file with custom settings
URL url = getClass().getResource("/data.csv")
CSVFormat format = CSVFormat.Builder.create()
    .setDelimiter(',')           // Set the delimiter (comma is default)
    .setQuote('"' as Character)  // Set the quote character
    .setTrim(true)               // Trim whitespace from values
    .setIgnoreEmptyLines(true)   // Skip empty lines
    .setHeader()                 // Use first row as header
    .build()
    
Matrix data = CsvImporter.importCsv(url, format)
```

### Handling Complex CSV Formats

Let's look at a more complex example with semicolon-delimited fields, quoted strings, and empty lines:

```groovy
import org.apache.commons.csv.CSVFormat
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvImporter

// Sample CSV content:
// 1;"Per";"2023-Apr-30";234,12
// 
// 2;"Karin";"2023-May-10";345,22
// 3;"Tage";"2023-Jun-20";3489,01
// 4;"Arne";"2023-Jul-01";222,99

URL url = getClass().getResource("/colonQuotesEmptyLine.csv")
CSVFormat format = CSVFormat.Builder.create()
    .setTrim(true)
    .setDelimiter(';')
    .setIgnoreEmptyLines(true)
    .setQuote('"' as Character)
    .setHeader('id', 'name', 'date', 'amount')  // Explicitly set column names
    .build()
    
Matrix matrix = CsvImporter.importCsv(url, format)
```

### Converting Data Types

When importing CSV files, all values are initially imported as strings. To convert the content to appropriate data types, use the `convert` method:

```groovy
import org.apache.commons.csv.CSVFormat
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvImporter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

// Import the CSV file
URL url = getClass().getResource("/data.csv")
CSVFormat format = CSVFormat.Builder.create()
    .setDelimiter(';')
    .setQuote('"' as Character)
    .setIgnoreEmptyLines(true)
    .setHeader('id', 'name', 'date', 'amount')
    .build()
    
Matrix matrix = CsvImporter.importCsv(url, format)

// Convert columns to appropriate types
Matrix table = matrix.convert(
    [
        "id": Integer,
        "name": String,
        "date": LocalDate,
        "amount": BigDecimal
    ],
    DateTimeFormatter.ofPattern("yyyy-MMM-dd"),
    NumberFormat.getInstance(Locale.GERMANY)  // For parsing numbers with comma as decimal separator
)

// Verify the conversion
assert 4 == table.rowCount()
assert ['id', 'name', 'date', 'amount'] == table.columnNames()
assert [4, 'Arne', LocalDate.parse('2023-07-01'), 222.99] == table.row(3)
```

## Exporting to CSV Files

The matrix-csv module also provides functionality to export Matrix objects to CSV files using the `CsvExporter` class.

### Basic CSV Export

Here's a simple example of exporting a Matrix to a CSV file:

```groovy
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.matrixcsv.CsvExporter
import org.apache.commons.csv.CSVFormat

// Load a sample dataset
Matrix mtcars = Dataset.mtcars()

// Create a temporary file for the export
File file = File.createTempFile('mtcars', '.csv')

// Export the Matrix to a CSV file with default settings
CsvExporter.exportToCsv(mtcars, CSVFormat.DEFAULT, file)

println("CSV file exported to: ${file.absolutePath}")
```

### Customizing CSV Export

You can customize the CSV format for export using the Apache Commons CSV builder:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvExporter
import org.apache.commons.csv.CSVFormat

// Create or obtain a Matrix
Matrix salesData = Matrix.builder().data(
    date: ['2023-01-15', '2023-02-20', '2023-03-10'],
    product: ['Widget A', 'Widget B', 'Widget C'],
    quantity: [120, 85, 150],
    price: [40.00, 60.00, 38.00]
).build()

// Create a custom CSV format
CSVFormat format = CSVFormat.Builder.create()
    .setDelimiter(';')
    .setQuote('"' as Character)
    .setRecordSeparator('\r\n')  // Windows-style line endings
    .build()

// Export to a file
File file = new File('/path/to/sales_data.csv')
CsvExporter.exportToCsv(salesData, format, file)
```

### Exporting to a Writer

Instead of exporting to a file, you can export to a Writer, which is useful for streaming CSV data:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvExporter
import org.apache.commons.csv.CSVFormat

// Create or obtain a Matrix
Matrix data = Matrix.builder().data(
    id: [1, 2, 3],
    name: ['Alice', 'Bob', 'Charlie'],
    score: [85, 92, 78]
).build()

// Export to a StringWriter to get the CSV as a string
StringWriter writer = new StringWriter()
CsvExporter.exportToCsv(data, CSVFormat.DEFAULT, writer)
String csvContent = writer.toString()
println(csvContent)

// Export to a FileWriter
File file = new File('/path/to/output.csv')
FileWriter fileWriter = new FileWriter(file)
CsvExporter.exportToCsv(data, CSVFormat.DEFAULT, fileWriter)
fileWriter.close()
```

## Working with Apache Commons CSV

The matrix-csv module leverages Apache Commons CSV, which provides a rich set of features for handling CSV files. Here are some additional configuration options you might find useful:

### CSV Format Presets

Apache Commons CSV provides several predefined formats:

```groovy
// Standard CSV format
CSVFormat csvFormat = CSVFormat.DEFAULT

// Excel-compatible CSV format
CSVFormat excelFormat = CSVFormat.EXCEL

// Tab-delimited format
CSVFormat tsvFormat = CSVFormat.TDF

// RFC 4180 compliant format
CSVFormat rfc4180Format = CSVFormat.RFC4180
```

### Custom Format Options

You can customize various aspects of the CSV format:

```groovy
CSVFormat customFormat = CSVFormat.Builder.create()
    .setDelimiter(',')                // Character separating fields
    .setQuote('"' as Character)       // Character used to quote fields
    .setEscape('\\' as Character)     // Character used to escape special characters
    .setHeader("ID", "Name", "Value") // Specify header names
    .setSkipHeaderRecord(true)        // Skip writing the header record
    .setIgnoreEmptyLines(true)        // Ignore empty lines
    .setAllowMissingColumnNames(true) // Allow missing column names
    .setTrim(true)                    // Trim leading/trailing whitespace
    .setTrailingDelimiter(false)      // Don't add a delimiter at the end of each record
    .setNullString("NULL")            // String to write for null values
    .build()
```

## Best Practices

1. **Data Type Conversion**: Always convert imported CSV data to appropriate types using the `convert` method for proper data handling.

2. **Character Encoding**: Be aware of character encoding issues when working with CSV files. You may need to specify the encoding when reading from or writing to files.

3. **Headers**: Using headers makes your CSV files more self-documenting and easier to work with. Consider using the `setHeader` method when defining your CSV format.

4. **Error Handling**: Implement proper error handling when importing CSV files, as they may contain unexpected formats or values.

5. **Large Files**: For large CSV files, consider processing them in chunks to avoid memory issues.

## Version Compatibility

The matrix-csv module has specific version compatibility requirements with the matrix-core module. The following table illustrates the version compatibility:

| Matrix CSV | Matrix Core |
|------------|-------------|
| 1.0.0      | 1.2.3 -> 1.2.4 |
| 1.0.1      | 2.0.0 -> 2.1.1 |
| 1.1.0      | 2.2.0 |
| 2.0.0      | 3.0.0 |

Make sure to use compatible versions to avoid potential issues.

## Conclusion

The matrix-csv module provides a powerful and flexible way to import data from CSV files into Matrix objects and export Matrix objects to CSV files. By leveraging Apache Commons CSV, it offers extensive customization options to handle various CSV formats and requirements.

In the next section, we'll explore the matrix-json module, which provides similar functionality for working with JSON data.

Go to [previous section](5-matrix-spreadsheet.md) | Go to [next section](7-matrix-json.md) | Back to [outline](outline.md)