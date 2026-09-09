# Matrix-Tablesaw

Provides interoperability between [Tablesaw](https://github.com/jtablesaw/tablesaw) and Matrix as well as
various extensions to Tablesaw such as `BigDecimalColumn`,
`Gtable` (which makes Tablesaw Groovier) and
complementary operations to deal with Tablesaw data — e.g. frequency tables,
column normalization, and easy Matrix conversion.

## Dependencies

This module depends on `tablesaw-core` and related libraries. It references
`matrix-core` and `matrix-stats` as `compileOnly` dependencies; users should
bring those in explicitly. The easiest way to get aligned versions is via the
Matrix BOM:

```groovy
implementation platform('se.alipsa.matrix:matrix-bom:2.5.1')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-stats'
implementation 'se.alipsa.matrix:matrix-tablesaw'
```

Or use `matrix-all` if you want every Matrix module:

```groovy
implementation 'se.alipsa.matrix:matrix-all:2.5.1'
```

## Quick examples

### Matrix ↔ Tablesaw conversion

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.TableUtil
import static se.alipsa.matrix.tablesaw.gtable.Gtable.*

// Create a Matrix and convert to a Gtable
def matrix = Matrix.builder().data(
    name: ["Alice", "Bob", "Charlie"],
    salary: [50000, 60000, 70000]
).types(String, BigDecimal).build()

def gTable = TableUtil.fromMatrix(matrix)

// Convert back to Matrix
def back = TableUtil.toMatrix(gTable)
```

### Create a Gtable with inferred types

```groovy
import se.alipsa.matrix.tablesaw.gtable.Gtable

def table = Gtable.create([
    name: ['Alice', 'Bob'],
    age: [25, 30],
    salary: [50000.0, 60000.0]
])
// Types are inferred: STRING, INTEGER, BIGDECIMAL
```

### BigDecimalColumn arithmetic (non-mutating)

```groovy
import tech.tablesaw.api.BigDecimalColumn

def col1 = BigDecimalColumn.create('a', [1.0, 2.0, 3.0])
def col2 = BigDecimalColumn.create('b', [10.0, 20.0, 30.0])

// plus, subtract, multiply, divide return NEW columns
def sum = col1 + col2        // or col1.plus(col2)
def diff = col1 - col2       // or col1.subtract(col2)
def prod = col1 * col2       // or col1.multiply(col2)
def quot = col1 / col2       // or col1.divide(col2)

// Mutating variants (change col1 in place)
col1.addTo(col2)
col1.subtractBy(col2)
col1.multiplyBy(col2)
col1.divideBy(col2)
```

### Table-level normalization

```groovy
import se.alipsa.matrix.tablesaw.gtable.Gtable

def table = Gtable.create([
    value: [10.0, 20.0, 30.0, 40.0]
])

// Non-destructive: returns a new table
def minMax = table.normalizeMinMax('value', 'value_norm', 4)
def zScore = table.normalizeStdScale('value', 'value_z')
def meanNorm = table.normalizeMean('value')

// Replace the source column in the returned copy (non-destructive)
def replaced = table.normalizeMinMax('value')
```

### Reading ODS, XLSX, CSV, XML, JSON

Use `Gtable.read()` instead of `Table.read()` so every overload — path, `File`,
`InputStream`, `URL`, `Reader`, or `CsvReadOptions` — returns a `Gtable`
rather than a plain Tablesaw `Table`:

```groovy
import se.alipsa.matrix.tablesaw.gtable.Gtable

Gtable odsTable = Gtable.read().ods('data.ods')
Gtable xlsxTable = Gtable.read().xlsx('data.xlsx')
Gtable csvTable = Gtable.read().csv('data.csv')
Gtable xmlTable = Gtable.read().xml('data.xml')
Gtable jsonTable = Gtable.read().json('data.json')
```

### Joining tables

`Gtable.joinOn(...)` returns a `GdataFrameJoiner` whose fluent builder methods
keep returning `GdataFrameJoiner`, and whose terminal `join()` returns a
`Gtable`:

```groovy
def joined = employees.joinOn('id')
    .type(JoinType.INNER)
    .with(performance)
    .join()
// joined is a Gtable
```

## Version 0.4.0 behavior notes

### Secure XML reading

`XmlReader` rejects any XML containing a `DOCTYPE` declaration and disables external entities and
external DTD loading, so files cannot trigger external resource reads (XXE):

```groovy
// Throws RuntimeIOException for documents with a DOCTYPE or that reference
// external entities; the external resource is never read.
Gtable table = Gtable.read().xml('data.xml')
```

`Reader` and `InputStream` sources are owned by the reader: they are closed after parsing, on both
success and failure (matching the ODS reader). Do not reuse a reader you have handed to
`XmlReadOptions.builder(reader)`.

### XML shape validation and duplicate column names

Every first-row `<td>` must carry a non-blank `name` attribute, and every later row must contain
exactly as many `<td>` elements as the first row; otherwise reading fails with a
`RuntimeIOException` naming the one-based data row and the expected/actual cell count.

Duplicate column names are compared case-insensitively. By default they are rejected; when you
opt in, later occurrences are renamed deterministically and collision-safely:

```groovy
// Rejects 'Name' + 'name' with RuntimeIOException:
Gtable.read().usingOptions(XmlReadOptions.builder('data.xml').build())

// Allows them. For input 'Name', 'name', 'name-2', columns become
// 'Name', 'name-3', 'name-2': original names are reserved before suffixing.
def options = XmlReadOptions.builder('data.xml')
    .allowDuplicateColumnNames(true)
    .build()
Gtable table = Gtable.read().usingOptions(options)
```

### Writers preserve missing values

The XLSX, ODS, and XML writers emit missing cells as blank/empty cells — no numeric or boolean
sentinels are serialized, so a missing `SHORT` never becomes `-32768` in the output file, and a
missing `BOOLEAN` never becomes `false`. Ordinary finite values and `false` remain
distinguishable from missing. XML string values preserve leading, trailing, repeated, tab, and
whitespace-only content exactly.

### XLSX is binary-only; worksheet names are sanitized

```groovy
// Writing to a Writer-backed destination is rejected before the workbook is built:
table.write().usingOptions(XlsxWriteOptions.builder(new StringWriter()).build())
// -> IllegalArgumentException("XLSX requires a binary OutputStream destination")

// Use a stream, File, or file name instead. Names longer than 31 characters or containing
// []:*?/\ are sanitized to a safe deterministic sheet name. Null or blank names, including
// non-blank names that become blank after sanitizing (such as '[]'), use 'Sheet1':
table.write().usingOptions(XlsxWriteOptions.builder('report.xlsx').build())
```

`XlsxWriteOptions.builder(Writer)` still compiles but is deprecated: `build()` succeeds and
`XlsxWriter.write` rejects the destination.

### File destinations open lazily

For all three formats, `builder(File)` and `builder(String)` defer opening the output file until
writing starts — merely building options neither creates nor truncates the target, and I/O errors
surface as `RuntimeIOException` from the write call. A successful write closes the stream:

```groovy
def options = XmlWriteOptions.builder('out.xml').build()
assert !new File('out.xml').exists()   // not created yet
table.write().usingOptions(options)    // file created, written, and closed here
```

### BigDecimalColumn conversion rules

```groovy
def col = BigDecimalColumn.create('x')
col.append(Double.NaN)          // becomes a missing value
col.append(Float.NaN)           // becomes a missing value
col.append(1.1f)                // exactly 1.1 (Float.toString, no double widening)
col.append(0.1d)                // BigDecimal.valueOf(0.1) -> 0.1
col.append(Double.POSITIVE_INFINITY)  // IllegalArgumentException: not representable
```

### Division defaults and explicit contexts

No-context division uses `MathContext.DECIMAL64`; pass an explicit context when you need another
precision:

```groovy
def a = BigDecimalColumn.create('a', [10.0])
def b = BigDecimalColumn.create('b', [3.0])
def q = a.divide(b)                                  // DECIMAL64: 3.333333333333333
def qp = a.divide(b, new MathContext(12))            // caller-chosen precision
a.divideBy(b)                                        // mutates a in place
```

### Strict Gtable type validation

Lossless widenings are still accepted and converted (for example integer values in a
`BigDecimal`-typed column, floating-point values through `BigDecimalColumn.toBigDecimal` where NaN
becomes missing and infinities are rejected, or a `GString` in a `STRING` column); only values
that cannot be represented exactly are rejected:

```groovy
// Throws IllegalArgumentException naming the type index and column — no silent
// coercion or missing-value insertion:
Gtable.create([age: [1, 'x']], [ColumnType.INTEGER])   // mismatched value type

// These also fail fast instead of being ignored:
Gtable.create([a: [1]], [null])          // null type entry
Gtable.create([a: [1]], [ColumnType.SKIP])
Gtable.create([a: [1], b: [2]], [ColumnType.INTEGER])  // wrong number of types
Gtable.create([a: [1], b: [2]], null)                  // null type list
```

### Non-mutating rounding

`TableUtil.round` returns a rounded copy and leaves the source column untouched (HALF_EVEN by
default). You must use the return value:

```groovy
NumberColumn source = table.numberColumn('amount')
NumberColumn rounded = TableUtil.round(source, 2)   // source is unchanged
```

### ODS row handling

Interior all-missing rows round-trip through ODS and XML with their position preserved. Trailing
all-missing rows are dropped by default, because spreadsheet applications commonly declare empty
rows past the actual data range; disable the trim to keep a legitimate trailing all-missing data
row (for example when round-tripping a file this module wrote):

```groovy
Table kept = Table.read().usingOptions(
    OdsReadOptions.builder('data.ods').trimTrailingMissingRows(false).build())
```

## Documentation

See the [Tablesaw tutorial](../docs/tutorial/14-matrix-tablesaw.md) for a full walk-through, and the tests in `src/test/groovy/test/alipsa/groovy/matrix/tablesaw/` for executable examples.

## Version history

See [release.md](release.md).
