# Matrix CSV

## Read With Type Conversion

```groovy
import se.alipsa.matrix.csv.CsvReader

def sales = CsvReader.read()
    .types(Integer, String, BigDecimal)
    .from(new File('sales.csv'))
```

Use `dateTimeFormat(...)` or `numberFormat(...)` when the source data is locale-specific or date-heavy.

## Read With a Custom Delimiter

```groovy
import se.alipsa.matrix.csv.CsvReader

def matrix = CsvReader.read()
    .delimiter(';')
    .from(new File('semicolon.csv'))
```

For `.tsv` or `.tab` files you can usually skip the delimiter and rely on auto-detection:

```groovy
def tsv = CsvReader.read().from(new File('report.tsv'))
```

## Read Without Headers

```groovy
import se.alipsa.matrix.csv.CsvReader

def matrix = CsvReader.read()
    .firstRowAsHeader(false)
    .fromString("1,Alice,100.00\n2,Bob,200.00\n")
```

If you already know the column layout, provide it explicitly:

```groovy
def typed = CsvReader.read()
    .columns(id: Integer, name: String, amount: BigDecimal)
    .fromString("1,Alice,100.00\n2,Bob,200.00\n")
```

## Write to a String

```groovy
import se.alipsa.matrix.csv.CsvWriter

String csv = CsvWriter.write(matrix).asString()
String noHeader = CsvWriter.write(matrix).withHeader(false).asString()
```

## TSV Handling

```groovy
import se.alipsa.matrix.csv.CsvReader
import se.alipsa.matrix.csv.CsvWriter

def input = CsvReader.read().tsv().from(new File('input.tsv'))
CsvWriter.write(input).tsv().to(new File('copy.tsv'))
```

Typed and SPI APIs also auto-detect `.tsv` and `.tab` when the delimiter was not explicitly configured:

```groovy
import se.alipsa.matrix.core.Matrix

def tsv = Matrix.read(new File('input.tsv'))
tsv.write(new File('copy.tsv'))
```

## Round-Trip a Matrix

```groovy
import se.alipsa.matrix.csv.CsvReader
import se.alipsa.matrix.csv.CsvWriter

String csv = CsvWriter.write(matrix).asString()
def copy = CsvReader.read().fromString(csv)
```

For Excel-compatible output:

```groovy
String csv = CsvWriter.write(matrix).excel().asString()
def copy = CsvReader.read().excel().fromString(csv)
```

## Use the Generic Matrix SPI

```groovy
import se.alipsa.matrix.core.Matrix

def matrix = Matrix.read(new File('sales.csv'))
def renamed = Matrix.read([tableName: 'sales'], new File('sales.csv'))

matrix.write(new File('copy.csv'))
matrix.write([delimiter: ';', nullString: 'NULL'], new File('copy.csv'))
```

At runtime you can inspect the currently supported options:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvWriteOptions

println Matrix.listReadOptions('csv')
println Matrix.listWriteOptions('csv')
println CsvReadOptions.describe()
println CsvWriteOptions.describe()
```

---
[Back to index](cookbook.md)  |  [Next (Matrix Json)](matrix-json.md)
