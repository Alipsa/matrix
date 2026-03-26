# Matrix CSV Module

The `matrix-csv` module reads and writes CSV-style tabular data through a fluent API, typed options objects, and the generic Matrix SPI. It supports standard CSV plus `.tsv` and `.tab` files, and uses Apache Commons CSV internally while keeping the recommended API surface Matrix-specific.

## Installation

Use the Matrix BOM when you depend on multiple Matrix modules:

### Gradle

```groovy
implementation platform("se.alipsa.matrix:matrix-bom:2.5.0-SNAPSHOT")
implementation "se.alipsa.matrix:matrix-core"
implementation "se.alipsa.matrix:matrix-csv"
```

### Maven

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.5.0-SNAPSHOT</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-csv</artifactId>
    </dependency>
  </dependencies>
</project>
```

## Fluent API: Reading CSV

The fluent API is the primary way to use `matrix-csv`.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReader

Matrix basic = CsvReader.read().from(new File("basic.csv"))

Matrix semicolon = CsvReader.read()
    .delimiter(';')
    .from(new File("sales.csv"))

Matrix fromString = CsvReader.read()
    .fromString("name,age\nAlice,30\nBob,25\n")
```

### Reading Without Headers

```groovy
Matrix matrix = CsvReader.read()
    .firstRowAsHeader(false)
    .fromString("1,Alice,100.00\n2,Bob,200.00\n")

assert ['c0', 'c1', 'c2'] == matrix.columnNames()
```

If you already know both headers and types, use `columns(...)`:

```groovy
Matrix typed = CsvReader.read()
    .columns(id: Integer, name: String, amount: BigDecimal)
    .fromString("1,Alice,100.00\n2,Bob,200.00\n")
```

### Type Conversion During Read

```groovy
import java.time.LocalDate

Matrix sales = CsvReader.read()
    .types(Integer, String, LocalDate, BigDecimal)
    .dateTimeFormat('yyyy-MM-dd')
    .fromString("""id,name,date,amount
1,Alice,2025-01-15,100.50
2,Bob,2025-01-20,200.75
""")
```

For locale-specific numbers, add `numberFormat(...)`.

### Presets

```groovy
Matrix excel = CsvReader.read()
    .excel()
    .from(new File("report.csv"))

Matrix tsv = CsvReader.read()
    .tsv()
    .from(new File("report.tsv"))
```

`excel()` applies Apache Excel semantics, including CRLF records and more literal read handling (`trim(false)`, `ignoreEmptyLines(false)`, `ignoreSurroundingSpaces(false)`).

## Fluent API: Writing CSV

```groovy
import se.alipsa.matrix.csv.CsvWriter

CsvWriter.write(sales).to(new File("sales-copy.csv"))

String csv = CsvWriter.write(sales).asString()

CsvWriter.write(sales)
    .delimiter(';')
    .to(new File("sales-semicolon.csv"))

CsvWriter.write(sales)
    .withHeader(false)
    .to(new File("sales-no-header.csv"))
```

### Excel and TSV Output

```groovy
CsvWriter.write(sales).excel().to(new File("sales-excel.csv"))
CsvWriter.write(sales).tsv().to(new File("sales.tsv"))
```

`excel()` writes CRLF-separated output and quotes all non-null values.

## Typed Options API

Use typed options when you want an explicit configuration object instead of chained fluent calls.

### Typed Reads

```groovy
import org.apache.commons.csv.DuplicateHeaderMode
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvReader

CsvReadOptions readOptions = new CsvReadOptions()
    .delimiter(';')
    .charset('ISO-8859-1')
    .tableName('sales')
    .types([Integer, String, BigDecimal])
    .duplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)

Matrix sales = CsvReader.read(new File("sales.csv"), readOptions)
```

Read-side source semantics:

- charset affects `File`, `Path`, `URL`, and `InputStream`
- charset has no effect for `Reader` or `String`
- `tableName(...)` overrides source-derived matrix names
- `.tsv` and `.tab` auto-detect tab delimiters unless the delimiter was explicitly configured

### Typed Writes

```groovy
import se.alipsa.matrix.csv.CsvWriteOptions
import se.alipsa.matrix.csv.CsvWriter

CsvWriteOptions writeOptions = new CsvWriteOptions()
    .delimiter(';')
    .escape('\\')
    .nullString('NULL')
    .recordSeparator('\r\n')

CsvWriter.write(sales, new File("sales.csv"), writeOptions)
String csv = CsvWriter.writeString(sales, writeOptions)
```

The typed write contract only includes options with observable write behavior:

- delimiter
- quote
- escape
- withHeader
- charset
- recordSeparator
- nullString

Parse-only keys such as `trim`, `ignoreEmptyLines`, `ignoreSurroundingSpaces`, and `commentMarker` are rejected when used as write options.

### `fromMap(...)` Normalization

The typed options classes can normalize SPI-style maps:

```groovy
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvWriteOptions

CsvReadOptions readOptions = CsvReadOptions.fromMap([
    delimiter          : ';',
    duplicateHeaderMode: 'ALLOW_ALL'
])

CsvWriteOptions writeOptions = CsvWriteOptions.fromMap([
    delimiter  : ';',
    escape     : '\\',
    nullString : 'NULL'
])
```

This keeps direct typed usage and SPI usage aligned.

## Generic Matrix SPI

If `matrix-csv` is on the classpath, Matrix automatically discovers the CSV provider for `.csv`, `.tsv`, and `.tab`.

```groovy
import se.alipsa.matrix.core.Matrix

Matrix csv = Matrix.read(new File("basic.csv"))
Matrix renamed = Matrix.read([tableName: 'sales'], new File("sales.csv"))
Matrix tsv = Matrix.read(new File("report.tsv"))

csv.write(new File("copy.csv"))
csv.write([delimiter: ';', nullString: 'NULL'], new File("copy.csv"))
```

Runtime option discovery is available through:

```groovy
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvWriteOptions
import se.alipsa.matrix.core.Matrix

println Matrix.listReadOptions('csv')
println Matrix.listWriteOptions('csv')
println CsvReadOptions.describe()
println CsvWriteOptions.describe()
```

## Legacy Apache `CSVFormat` Overloads

The old overloads that accept Apache Commons `CSVFormat` directly still exist for compatibility, but they are deprecated. Prefer one of these instead:

- the fluent `CsvReader` / `CsvWriter` API
- the typed `CsvReadOptions` / `CsvWriteOptions` API
- the generic `Matrix.read(...)` / `matrix.write(...)` SPI

## Summary

Use the fluent API by default, reach for typed options when you want explicit reusable configuration, and use the generic Matrix SPI when you want extension-based file handling. The three paths share the same behavior and option normalization.

Go to [previous section](5-matrix-spreadsheet.md) | Go to [next section](7-matrix-json.md) | Back to [outline](outline.md)
