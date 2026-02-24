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

### Using the fluent API (recommended)

The fluent API provides a chainable builder pattern for reading CSV files:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReader

// Simple read with defaults
Matrix m = CsvReader.read().from(file)

// Read with custom delimiter
Matrix m = CsvReader.read()
    .delimiter(';')
    .from(file)

// Read from a String
String csvContent = "name,age\nAlice,30\nBob,25"
Matrix m = CsvReader.read().fromString(csvContent)

// Read from a URL
Matrix m = CsvReader.read().from(url)

// Read TSV preset
Matrix m = CsvReader.read().tsv().from(file)

// Read with explicit header (no header row in data)
Matrix m = CsvReader.read()
    .header(['id', 'name', 'amount'])
    .from(file)

// Read with matrix name and charset
Matrix m = CsvReader.read()
    .matrixName('myData')
    .charset(StandardCharsets.ISO_8859_1)
    .from(file)

// Read with type conversion (types by position)
Matrix m = CsvReader.read()
    .types(Integer, String, LocalDate, BigDecimal)
    .dateTimeFormat('yyyy-MM-dd')
    .from(file)

// Read with columns() — sets both header and types in one call
Matrix m = CsvReader.read()
    .columns(id: Integer, name: String, date: LocalDate, amount: BigDecimal)
    .dateTimeFormat('yyyy-MM-dd')
    .from(file)

// Read with header + types separately
Matrix m = CsvReader.read()
    .header(['id', 'name', 'amount'])
    .types(Integer, String, BigDecimal)
    .from(file)
```

### Using named arguments (Map-based API)

You can also use Groovy named arguments for concise format configuration.
Keys are case-insensitive, so both `delimiter: ';'` and `Delimiter: ';'` work:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReader

URL url = getClass().getResource("/colonQuotesEmptyLine.csv")
Matrix matrix = CsvReader.read(
    trim: true,
    delimiter: ';',
    ignoreEmptyLines: true,
    quote: '"',
    header: ['id', 'name', 'date', 'amount'],
    url)

// With type conversion
Matrix m = CsvReader.read(
    types: [Integer, String, LocalDate, BigDecimal],
    dateTimeFormat: 'yyyy-MM-dd',
    file)
```

### Using CSVFormat (deprecated)

The methods accepting Apache Commons CSV `CSVFormat` directly are deprecated.
Migrate to the fluent API to decouple from the third-party library:

```groovy
// Before (deprecated)
import org.apache.commons.csv.CSVFormat
CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
Matrix basic = CsvReader.read(url, format)

// After (recommended)
Matrix basic = CsvReader.read().trim(true).from(url)
```

### Type conversion

Type conversion can be done inline during reading using `types()`, `columns()`,
`dateTimeFormat()`, and `numberFormat()`:

```groovy
// Integrated approach (recommended)
Matrix table = CsvReader.read()
    .types(Integer, String, LocalDate, BigDecimal)
    .dateTimeFormat('yyyy-MM-dd')
    .from(file)

// Or with columns() for header + types in one call
Matrix table = CsvReader.read()
    .columns(id: Integer, name: String, date: LocalDate, amount: BigDecimal)
    .dateTimeFormat('yyyy-MM-dd')
    .from(file)
```

Alternatively, you can convert after reading using the Matrix `convert` method:
```groovy
Matrix raw = CsvReader.read().from(file)
Matrix table = raw.convert(
  [
      "id": Integer,
      "name": String,
      "date": LocalDate,
      "amount": BigDecimal
  ],
  DateTimeFormatter.ofPattern("yyyy-MMM-dd"),
  NumberFormat.getInstance(Locale.GERMANY)
)
```

## Exporting a Matrix to a CSV file

### Using the fluent API (recommended)

```groovy
import se.alipsa.matrix.csv.CsvWriter

// Write to file with defaults
CsvWriter.write(matrix).to(file)

// Write to string
String csv = CsvWriter.write(matrix).asString()

// Write with custom delimiter
CsvWriter.write(matrix)
    .delimiter(';')
    .to(file)

// Write without header
CsvWriter.write(matrix).withHeader(false).to(file)

// Write Excel CSV
CsvWriter.write(matrix).excel().to(file)

// Write TSV
CsvWriter.write(matrix).tsv().to(file)

// Convenience methods (also available)
CsvWriter.writeExcelCsv(matrix, file)
CsvWriter.writeTsv(matrix, file)
```

### Using CSVFormat (deprecated)

```groovy
// Before (deprecated)
import org.apache.commons.csv.CSVFormat
CsvWriter.write(matrix, file, CSVFormat.DEFAULT)

// After (recommended)
CsvWriter.write(matrix).to(file)
```

## Fluent builder options

### Format configuration (read and write)

| Method                             | Default | Description                              |
|------------------------------------|---------|------------------------------------------|
| `delimiter(char)`                  | `,`     | Field separator character                |
| `quoteCharacter(Character)`        | `"`     | Quote character for enclosing fields     |
| `escapeCharacter(Character)`       | `null`  | Escape character for special characters  |
| `commentMarker(Character)`         | `null`  | Character marking comment lines          |
| `trim(boolean)`                    | `true`  | Trim whitespace from values              |
| `ignoreEmptyLines(boolean)`        | `true`  | Skip blank lines when reading            |
| `ignoreSurroundingSpaces(boolean)` | `true`  | Ignore spaces around quoted values       |
| `nullString(String)`               | `null`  | String to interpret as null when reading |
| `recordSeparator(String)`          | `\n`    | Record separator for writing             |

### Read-specific options

| Method                          | Default | Description                                             |
|---------------------------------|---------|---------------------------------------------------------|
| `firstRowAsHeader(boolean)`     | `true`  | Whether the first row contains column names             |
| `header(List<String>)`          | `null`  | Explicit header; sets firstRowAsHeader to false          |
| `charset(Charset)`              | `UTF-8` | Character encoding                                      |
| `matrixName(String)`            | `''`    | Name for the resulting Matrix                           |
| `types(List<Class>)`            | `null`  | Column types by position for automatic type conversion  |
| `types(Class...)`               | `null`  | Column types (varargs), e.g. `types(Integer, String)`   |
| `columns(Map<String, Class>)`   | `null`  | Sets both header and types from a name-to-type map      |
| `dateTimeFormat(String)`        | `null`  | Date/time parse pattern (e.g. `'yyyy-MM-dd'`)           |
| `numberFormat(NumberFormat)`    | `null`  | Locale-aware number parsing format                      |

### Write-specific options

| Method                | Default | Description                                        |
|-----------------------|---------|----------------------------------------------------|
| `withHeader(boolean)` | `true`  | Include column names in the first row              |

### Preset methods (read and write)

| Method       | Effect                                         |
|--------------|-------------------------------------------------|
| `excel()`    | Sets `recordSeparator('\r\n')`                  |
| `tsv()`      | Sets `delimiter('\t')`                          |
| `rfc4180()`  | Sets `recordSeparator('\r\n')`                  |


# Release version compatibility matrix
See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended combinations of matrix library versions.
