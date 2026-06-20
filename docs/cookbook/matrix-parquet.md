# Matrix Parquet

Focused recipes for Parquet import/export, compression, numeric metadata, stream naming, and index metadata.

## Inspect the available Parquet options at runtime

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.ParquetReadOptions
import se.alipsa.matrix.parquet.ParquetWriteOptions

println Matrix.listReadOptions('parquet')
println Matrix.listWriteOptions('parquet')
println ParquetReadOptions.describe()
println ParquetWriteOptions.describe()
```

## Read and write with the generic Matrix API

```groovy
import se.alipsa.matrix.core.Matrix

Matrix data = Matrix.read(new File('orders.parquet'))

data.write([
    compressionCodec: 'GZIP',
    decimalMeta     : [amount: [12, 2]]
], new File('orders-copy.parquet'))
```

`decimalMeta` accepts Groovy lists, `Number[]`, and `int[]` values shaped as `[precision, scale]`.

## Write with explicit compression

```groovy
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import se.alipsa.matrix.parquet.MatrixParquetWriter

MatrixParquetWriter.builder(data)
    .compressionCodec(CompressionCodecName.ZSTD)
    .write(new File('orders-zstd.parquet'))

MatrixParquetWriter.builder(data)
    .compressionCodec('UNCOMPRESSED')
    .write(new File('orders-plain.parquet'))
```

`SNAPPY` is the default. Reading compressed files is transparent.

## Force decimal metadata for selected columns

```groovy
import se.alipsa.matrix.parquet.MatrixParquetWriter

MatrixParquetWriter.write(data, new File('orders-decimal.parquet'), [
    amount: [12, 2],
    tax   : [8, 2]
])
```

Validation is shared by direct, builder, and SPI writes: precision must be greater than 0, scale must be at least 0, and scale must not exceed precision.

## Store very large integer identifiers safely

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

Matrix ids = Matrix.builder('ids')
    .columns(id: [new BigInteger('123456789012345678901234567890')])
    .types(BigInteger)
    .build()

MatrixParquetWriter.write(ids, new File('ids.parquet'))
Matrix restored = MatrixParquetReader.read(new File('ids.parquet'))

assert restored[0, 'id'] == ids[0, 'id']
```

`BigInteger` is written as Parquet `DECIMAL(precision, 0)` rather than being narrowed to `Long`.

## Preserve a matrix name through bytes or streams

```groovy
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

byte[] bytes = MatrixParquetWriter.writeBytes(data.withMatrixName('Orders'))

Matrix restored = MatrixParquetReader.read(bytes)
assert restored.matrixName == 'Orders'

Matrix explicit = MatrixParquetReader.read(bytes, 'OrdersFromApi')
assert explicit.matrixName == 'OrdersFromApi'
```

When no explicit name is supplied for byte-array, stream, or URL reads, the reader uses the Parquet schema/message name.

## Round-trip `java.util.Date`

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

Matrix events = Matrix.builder('events')
    .columns(event: ['created'], happenedAt: [new Date(1_735_689_600_000L)])
    .types(String, Date)
    .build()

MatrixParquetWriter.write(events, new File('events.parquet'))
Matrix restored = MatrixParquetReader.read(new File('events.parquet'))

assert restored[0, 'happenedAt'] == events[0, 'happenedAt']
```

Matrix metadata lets the reader restore `java.util.Date` values from epoch milliseconds.

## Preserve indexes whose column names contain commas

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

Matrix sales = Matrix.builder('sales')
    .columns(
        'country,region': ['SE,Stockholm', 'US,West'],
        quarter: ['Q1', 'Q2'],
        revenue: [100, 200]
    )
    .types(String, String, Integer)
    .build()

sales.createIndex('country,region')
MatrixParquetWriter.write(sales, new File('sales.parquet'))

Matrix restored = MatrixParquetReader.read(new File('sales.parquet'))
assert restored.indexedColumns() == ['country,region']
assert restored.lookup('SE,Stockholm').rowCount() == 1
```

New writes store `matrix.indexColumns` as a JSON string array. The reader also accepts legacy comma-delimited metadata, but older readers and external tools that split this metadata value on commas will not parse newly-written index metadata correctly.

## Read external string-encoded decimals

When an external file stores a Matrix-declared `BigDecimal` column as plain UTF-8 `BINARY` without a Parquet `DECIMAL` annotation, the reader parses the text as `BigDecimal` instead of treating the field as a double value.

For files with no Matrix metadata at all, Parquet `DECIMAL` columns are inferred as `BigDecimal`. Scale-0 decimals from external files cannot be distinguished from original `BigInteger` columns without Matrix metadata.

## Common troubleshooting

- Large file uses too much memory: Matrix reads the complete result into memory; reduce upstream or increase heap size.
- `BigInteger` reads as `BigDecimal` from an external file: expected when Matrix metadata is absent.
- An older consumer misreads `matrix.indexColumns`: 0.6.0 writes JSON metadata for indexed column names.
- Compression setting appears ignored on read: expected; Parquet stores codec metadata in the file footer, and the reader auto-detects it.

---
[Back to index](cookbook.md)
