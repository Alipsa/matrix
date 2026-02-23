[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-csv/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-csv)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-csv/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-csv)
# matrix-csv
Comprehensive support for creating a Matrix from structured text files (CSV files) and writing a Matrix to
a CSV file in the format of choice.

To use it in your project, add the following dependencies to your code
```groovy
implementation 'se.alipsa.matrix:matrix-core:3.6.0'
implementation 'se.alipsa.matrix:matrix-csv:2.2.2'
```

## Import a CSV file into a Matrix

### Using CsvFormat (recommended)

`CsvFormat` is an immutable configuration class that decouples your code from the
underlying CSV implementation. Use the builder pattern to customize parsing behavior:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvFormat
import se.alipsa.matrix.csv.CsvReader

// Simple read with default format
URL url = getClass().getResource("/basic.csv")
Matrix basic = CsvReader.read(url, CsvFormat.DEFAULT)

// Custom format with semicolon delimiter
CsvFormat format = CsvFormat.builder()
    .delimiter(';' as char)
    .trim(true)
    .ignoreEmptyLines(true)
    .quoteCharacter('"' as Character)
    .build()
Matrix matrix = CsvReader.read(url, format)

// Read from a String
String csvContent = "name,age\nAlice,30\nBob,25"
Matrix m = CsvReader.readString(csvContent, CsvFormat.DEFAULT)
```

Predefined format constants are available:
- `CsvFormat.DEFAULT` — comma-delimited, trimmed, ignoring empty lines
- `CsvFormat.EXCEL` — Excel-compatible with CRLF line endings
- `CsvFormat.TDF` — tab-delimited
- `CsvFormat.RFC4180` — RFC 4180 compliant with CRLF line endings

### Using named arguments (Map-based API)

You can also use Groovy named arguments for concise format configuration:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReader

URL url = getClass().getResource("/colonQuotesEmptyLine.csv")
Matrix matrix = CsvReader.read(
    Trim: true,
    Delimiter: ';',
    IgnoreEmptyLines: true,
    Quote: '"',
    Header: ['id', 'name', 'date', 'amount'],
    url)
```

### Using CSVFormat (deprecated)

The methods accepting Apache Commons CSV `CSVFormat` directly are deprecated.
Migrate to `CsvFormat` to decouple from the third-party library:

```groovy
// Before (deprecated)
import org.apache.commons.csv.CSVFormat
CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
Matrix basic = CsvReader.read(url, format)

// After (recommended)
import se.alipsa.matrix.csv.CsvFormat
CsvFormat format = CsvFormat.builder().trim(true).build()
Matrix basic = CsvReader.read(url, format)
```

### Type conversion

The resulting Matrix will be all strings. To convert the content to the appropriate type, use the `convert` method e.g.
```groovy
Matrix table = matrix.convert(
  [
      "id": Integer,
      "name": String,
      "date": LocalDate,
      "amount": BigDecimal
  ],
  DateTimeFormatter.ofPattern("yyyy-MMM-dd"),
  NumberFormat.getInstance(Locale.GERMANY)
)
//the following assertions then applies
assert 4 == table.rowCount() // Number of rows
assert ['id', 'name', 'date', 'amount'] == table.columnNames() // Column names
assert [4, 'Arne', LocalDate.parse('2023-07-01'), 222.99] == table.row(3) // last row
```

## Exporting a Matrix to a CSV file

### Using CsvFormat (recommended)

```groovy
import se.alipsa.matrix.csv.CsvFormat
import se.alipsa.matrix.csv.CsvWriter

// Write with default format
File file = new File('output.csv')
CsvWriter.write(matrix, file, CsvFormat.DEFAULT)

// Write to string
String csvContent = CsvWriter.writeString(matrix, CsvFormat.DEFAULT)

// Write with custom format
CsvFormat format = CsvFormat.builder()
    .delimiter(';' as char)
    .build()
CsvWriter.write(matrix, file, format)

// Convenience methods for Excel CSV and TSV
CsvWriter.writeExcelCsv(matrix, file)
CsvWriter.writeTsv(matrix, file)
```

### Using CSVFormat (deprecated)

```groovy
// Before (deprecated)
import org.apache.commons.csv.CSVFormat
CsvWriter.write(matrix, file, CSVFormat.DEFAULT)

// After (recommended)
import se.alipsa.matrix.csv.CsvFormat
CsvWriter.write(matrix, file, CsvFormat.DEFAULT)
```

## CsvFormat builder options

| Option                   | Default | Description                                       |
|--------------------------|---------|---------------------------------------------------|
| `delimiter(char)`        | `,`     | Field separator character                          |
| `quoteCharacter(Character)` | `"`  | Quote character for enclosing fields               |
| `escapeCharacter(Character)` | `null` | Escape character for special characters          |
| `commentMarker(Character)` | `null` | Character marking comment lines                   |
| `trim(boolean)`          | `true`  | Trim whitespace from values                       |
| `ignoreEmptyLines(boolean)` | `true` | Skip blank lines when reading                    |
| `ignoreSurroundingSpaces(boolean)` | `true` | Ignore spaces around quoted values       |
| `nullString(String)`     | `null`  | String to interpret as null when reading           |
| `recordSeparator(String)` | `\n`   | Record separator for writing                      |


# Release version compatibility matrix
See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended combinations of matrix library versions.
