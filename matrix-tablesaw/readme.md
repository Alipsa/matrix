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
implementation platform('se.alipsa.matrix:matrix-bom:2.5.0')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-stats'
implementation 'se.alipsa.matrix:matrix-tablesaw'
```

Or use `matrix-all` if you want every Matrix module:

```groovy
implementation 'se.alipsa.matrix:matrix-all:2.5.0'
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

## Documentation

See the [Tablesaw tutorial](../docs/tutorial/14-matrix-tablesaw.md) for a full walk-through, and the tests in `src/test/groovy/test/alipsa/groovy/matrix/tablesaw/` for executable examples.

## Version history

See [release.md](release.md).
