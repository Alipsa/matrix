[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-csv/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-csv)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-csv/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-csv)
# matrix-csv

CSV, TSV, and TAB-delimited read/write support for Matrix.

## At A Glance

- read delimited text from `File`, `Path`, `URL`, `InputStream`, `Reader`, and `String`
- write delimited text to `File`, `Path`, `Writer`, and `String`
- support `.csv`, `.tsv`, and `.tab` through both direct APIs and the generic Matrix SPI
- use `CsvReader` / `CsvWriter` for the direct API
- use `Matrix.read(...)` / `matrix.write(...)` for SPI-based loading and saving
- inspect runtime options with `Matrix.listReadOptions('csv')`, `Matrix.listWriteOptions('csv')`, `CsvReadOptions.describe()`, and `CsvWriteOptions.describe()`

## Getting Started

Versions below are examples. If you use multiple Matrix modules, prefer the Matrix BOM.

```groovy
dependencies {
  implementation platform("se.alipsa.matrix:matrix-bom:2.5.0-SNAPSHOT")
  implementation "se.alipsa.matrix:matrix-core"
  implementation "se.alipsa.matrix:matrix-csv"
}
```

## API Surface

Direct API entry points:

- `CsvReader.read()` for the fluent read API
- `CsvReader.read(source, CsvReadOptions)` and `CsvReader.readString(content, CsvReadOptions)` for typed read options
- `CsvWriter.write(matrix)` for the fluent write API
- `CsvWriter.write(matrix, target, CsvWriteOptions)` and `CsvWriter.writeString(matrix, CsvWriteOptions)` for typed write options
- `CsvReadOptions` and `CsvWriteOptions` for explicit configuration objects

Generic Matrix SPI entry points:

- `Matrix.read(new File('data.csv'))`
- `Matrix.read(new File('data.tsv'))`
- `matrix.write(new File('copy.csv'))`
- `matrix.write([delimiter: ';'], new File('copy.csv'))`
- `Matrix.listReadOptions('csv')`
- `Matrix.listWriteOptions('csv')`
- `CsvReadOptions.describe()`
- `CsvWriteOptions.describe()`

Runtime option discovery:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvWriteOptions

println Matrix.listReadOptions('csv')
println Matrix.listWriteOptions('csv')
println CsvReadOptions.describe()
println CsvWriteOptions.describe()
```

## Default Behavior

Read defaults:

- delimiter defaults to `,`
- quote defaults to `"`
- `trim(true)`, `ignoreEmptyLines(true)`, and `firstRowAsHeader(true)` are enabled by default
- `ignoreSurroundingSpaces(true)` is enabled by default except when the `excel()` preset is applied
- charset defaults to UTF-8 for byte-based sources (`File`, `Path`, `URL`, `InputStream`)
- charset has no effect for `Reader` or `String` content because characters are already decoded
- matrix naming precedence is `CsvReadOptions.tableName(...)`, then a source-derived name for `File`/`Path`/`URL`, then the fallback name `matrix` for `InputStream`/`Reader`/`String`
- `.tsv` and `.tab` file names auto-select tab delimiters for typed direct reads and SPI reads unless the delimiter was explicitly configured

Write defaults:

- delimiter defaults to `,`
- quote defaults to `"`
- header output is enabled by default
- charset defaults to UTF-8 for file/path targets
- `.tsv` and `.tab` targets auto-select tab delimiters for typed direct writes and SPI writes unless the delimiter was explicitly configured

Preset behavior:

- `excel()` applies Apache Excel semantics: `recordSeparator('\r\n')`, `QuoteMode.ALL_NON_NULL` on writes, `allowMissingColumnNames(true)`, and read-side `trim(false)`, `ignoreEmptyLines(false)`, `ignoreSurroundingSpaces(false)`
- `rfc4180()` applies `recordSeparator('\r\n')`
- `tsv()` applies `delimiter('\t')`

## Fluent Read API

The fluent API is the primary direct API for CSV reads.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReader

Matrix fromFile = CsvReader.read().from(new File('data.csv'))

Matrix semicolon = CsvReader.read()
    .delimiter(';')
    .from(new File('sales.csv'))

Matrix noHeader = CsvReader.read()
    .firstRowAsHeader(false)
    .fromString("1,Alice,100.00\n2,Bob,200.00\n")

Matrix typed = CsvReader.read()
    .types(Integer, String, BigDecimal)
    .fromString("id,name,amount\n1,Alice,100.00\n2,Bob,200.00\n")

Matrix explicitColumns = CsvReader.read()
    .columns(id: Integer, name: String, amount: BigDecimal)
    .fromString("1,Alice,100.00\n2,Bob,200.00\n")

Matrix excel = CsvReader.read()
    .excel()
    .from(new File('report.csv'))

Matrix tsv = CsvReader.read()
    .tsv()
    .from(new File('report.tsv'))
```

Useful read-only fluent methods:

- `commentMarker(...)`
- `trim(...)`
- `ignoreEmptyLines(...)`
- `ignoreSurroundingSpaces(...)`
- `firstRowAsHeader(...)`
- `header(...)`
- `charset(...)`
- `matrixName(...)`
- `types(...)`
- `columns(...)`
- `dateTimeFormat(...)`
- `numberFormat(...)`

## Fluent Write API

The fluent API is the primary direct API for CSV writes.

```groovy
import se.alipsa.matrix.csv.CsvWriter

CsvWriter.write(matrix).to(new File('copy.csv'))

String csv = CsvWriter.write(matrix).asString()

CsvWriter.write(matrix)
    .delimiter(';')
    .to(new File('copy.csv'))

CsvWriter.write(matrix)
    .withHeader(false)
    .to(new File('data-no-header.csv'))

CsvWriter.write(matrix)
    .excel()
    .to(new File('report.csv'))

CsvWriter.write(matrix)
    .tsv()
    .to(new File('report.tsv'))
```

Supported write-time fluent configuration is intentionally limited to options with observable write behavior:

- `delimiter(...)`
- `quoteCharacter(...)`
- `escapeCharacter(...)`
- `nullString(...)`
- `recordSeparator(...)`
- `withHeader(...)`
- preset helpers such as `excel()`, `tsv()`, and `rfc4180()`

Parse-only options such as `trim`, `ignoreEmptyLines`, and `ignoreSurroundingSpaces` are not part of the write contract.

## Typed Options API

`CsvReadOptions` and `CsvWriteOptions` are the secondary direct API when you want an explicit config object instead of chaining calls.

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

def matrix = CsvReader.read(new File('sales.csv'), readOptions)
```

Supported typed read options cover:

- delimiter, quote, escape, comment marker
- explicit header or `firstRowAsHeader(...)`
- charset, table name, types, date/time pattern, and number format
- trim / empty-line / surrounding-space handling
- `nullString(...)`, `duplicateHeaderMode(...)`, and `recordSeparator(...)`

Source-specific behavior:

- charset only affects `File`, `Path`, `URL`, and `InputStream`
- charset is ignored for `Reader` and `String`
- `tableName(...)` overrides source-derived names

### Typed Writes

```groovy
import se.alipsa.matrix.csv.CsvWriteOptions
import se.alipsa.matrix.csv.CsvWriter

CsvWriteOptions writeOptions = new CsvWriteOptions()
    .delimiter(';')
    .escape('\\')
    .nullString('NULL')
    .withHeader(true)
    .recordSeparator('\r\n')

CsvWriter.write(matrix, new File('sales.csv'), writeOptions)
String csv = CsvWriter.writeString(matrix, writeOptions)
```

Supported typed write options are:

- `delimiter(...)`
- `quote(...)`
- `escape(...)`
- `withHeader(...)`
- `charset(...)`
- `recordSeparator(...)`
- `nullString(...)`

Parse-only write keys such as `trim`, `ignoreEmptyLines`, `ignoreSurroundingSpaces`, and `commentMarker` are rejected at the typed/SPI boundary instead of being silently ignored.

### `toMap()` / `fromMap(...)`

Typed options can be normalized at the SPI boundary or persisted in application config:

```groovy
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvWriteOptions

CsvReadOptions readOptions = CsvReadOptions.fromMap([
    delimiter          : ';',
    charset            : 'ISO-8859-1',
    duplicateHeaderMode: 'ALLOW_ALL'
])

CsvWriteOptions writeOptions = CsvWriteOptions.fromMap([
    delimiter      : ';',
    escape         : '\\',
    nullString     : 'NULL',
    recordSeparator: '\r\n'
])

println readOptions.toMap()
println writeOptions.toMap()
```

`fromMap(...)` normalizes case-insensitive keys and keeps direct typed calls aligned with SPI behavior.

## Matrix SPI / Map-Based Usage

If `matrix-csv` is on the classpath, the provider is registered automatically for `.csv`, `.tsv`, and `.tab`.

```groovy
import se.alipsa.matrix.core.Matrix

Matrix csv = Matrix.read(new File('data.csv'))
Matrix tsv = Matrix.read(new File('data.tsv'))
Matrix renamed = Matrix.read([tableName: 'sales'], new File('sales.csv'))

csv.write(new File('copy.csv'))
csv.write([delimiter: ';', nullString: 'NULL'], new File('copy.csv'))
csv.write([withHeader: false], new File('copy-no-header.csv'))
```

Named-argument direct reads are also available in Groovy:

```groovy
import se.alipsa.matrix.csv.CsvReader

def matrix = CsvReader.read(
    delimiter: ';',
    trim: true,
    ignoreEmptyLines: true,
    new File('sales.csv')
)
```

## Deprecated Apache `CSVFormat` API

Legacy overloads that accept Apache Commons CSV `CSVFormat` directly still exist for compatibility, but they are deprecated. Prefer the fluent API or typed options API:

```groovy
// Deprecated
// Matrix matrix = CsvReader.read(file, csvFormat)

// Recommended
def matrix = CsvReader.read().delimiter(';').from(file)
```

## Quick Reference

### Shared fluent methods

| Method | Default | Notes |
|--------|---------|-------|
| `delimiter(...)` | `,` | Read and write |
| `quoteCharacter(...)` | `"` | Read and write |
| `escapeCharacter(...)` | `null` | Read and write |
| `nullString(...)` | `null` | Read and write |
| `recordSeparator(...)` | `\n` | Read and write |

### Read-only fluent methods

| Method | Default | Notes |
|--------|---------|-------|
| `commentMarker(...)` | `null` | Read only |
| `trim(...)` | `true` | Read only, except `excel()` changes this to `false` |
| `ignoreEmptyLines(...)` | `true` | Read only, except `excel()` changes this to `false` |
| `ignoreSurroundingSpaces(...)` | `true` | Read only, except `excel()` changes this to `false` |
| `firstRowAsHeader(...)` | `true` | Read only |
| `header(...)` | `null` | Read only |
| `charset(...)` | `UTF-8` | Only affects byte-based sources |
| `matrixName(...)` | source-derived / `matrix` | Read only |
| `types(...)` | `null` | Read only |
| `columns(...)` | `null` | Read only |
| `dateTimeFormat(...)` | `null` | Read only |
| `numberFormat(...)` | `null` | Read only |

### Write-only fluent methods

| Method | Default | Notes |
|--------|---------|-------|
| `withHeader(...)` | `true` | Write only |

### Presets

| Method | Effect |
|--------|--------|
| `excel()` | Apache Excel semantics, CRLF output, `QuoteMode.ALL_NON_NULL`, missing-header tolerance, and read-side `trim(false)`, `ignoreEmptyLines(false)`, `ignoreSurroundingSpaces(false)` |
| `tsv()` | `delimiter('\t')` |
| `rfc4180()` | `recordSeparator('\r\n')` |

## Release Version Compatibility Matrix

See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended combinations of Matrix library versions.
