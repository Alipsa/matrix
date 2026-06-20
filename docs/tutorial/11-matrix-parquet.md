# Matrix Parquet Module

The Matrix Parquet module reads and writes Apache Parquet files using the native `MatrixParquetReader` and `MatrixParquetWriter` APIs. Parquet is a columnar file format used heavily in Hadoop, Spark, lakehouse, and analytics pipelines.

## Installation

When you use the Matrix BOM, add `matrix-parquet` alongside `matrix-core`:

```groovy
implementation 'org.apache.groovy:groovy:5.0.6'
implementation platform('se.alipsa.matrix:matrix-bom:2.5.0')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-parquet'
```

With Maven:

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.5.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>5.0.6</version>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-parquet</artifactId>
    </dependency>
  </dependencies>
</project>
```

## Reading and Writing

Use the direct APIs when you want the full Parquet-specific surface:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

Matrix cars = Dataset.cars().withMatrixName('cars')
File file = new File('cars.parquet')

MatrixParquetWriter.write(cars, file)
Matrix restored = MatrixParquetReader.read(file)

assert restored.matrixName == 'cars'
assert restored.rowCount() == cars.rowCount()
```

For file reads, the default matrix name comes from the file name unless you pass an explicit name:

```groovy
Matrix named = MatrixParquetReader.read(new File('sales.parquet'), 'quarterly_sales')
```

For stream, byte-array, and URL reads, the reader now uses the Parquet schema/message name when no explicit `matrixName` is supplied. This avoids temporary-file names leaking into matrices read from non-file sources.

```groovy
byte[] content = MatrixParquetWriter.writeBytes(cars)

Matrix fromBytes = MatrixParquetReader.read(content)
assert fromBytes.matrixName == 'cars'
```

## Generic Matrix API

If `matrix-parquet` is on the classpath, `.parquet` files are also available through the generic Matrix SPI:

```groovy
import se.alipsa.matrix.core.Matrix

Matrix data = Matrix.read(new File('cars.parquet'))
Matrix events = Matrix.read([matrixName: 'events', zoneId: 'Europe/Stockholm'], new File('events.parquet'))

data.write([compressionCodec: 'GZIP'], new File('cars-gzip.parquet'))
data.write([decimalMeta: [amount: [12, 2]]], new File('money.parquet'))

println Matrix.listReadOptions('parquet')
println Matrix.listWriteOptions('parquet')
```

The `decimalMeta` option accepts natural Groovy list values such as `[12, 2]` as well as array values. All write paths validate that precision is positive, scale is non-negative, and scale does not exceed precision.

## Builder API

The builder API is useful when multiple options apply to one operation:

```groovy
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

MatrixParquetWriter.builder(cars)
    .compressionCodec(CompressionCodecName.ZSTD)
    .decimalMeta([price: [12, 2]])
    .zoneId('Europe/Stockholm')
    .write(new File('cars-zstd.parquet'))

Matrix restored = MatrixParquetReader.builder()
    .zoneId('Europe/Stockholm')
    .read(new File('cars-zstd.parquet'))
```

## Compression

Parquet files are written with `SNAPPY` compression by default. You can override the codec with the builder, typed options, or SPI map:

```groovy
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import se.alipsa.matrix.parquet.MatrixParquetWriter
import se.alipsa.matrix.parquet.ParquetWriteOptions

MatrixParquetWriter.builder(cars)
    .compressionCodec('GZIP')
    .write(new File('cars-gzip.parquet'))

MatrixParquetWriter.write(cars, new File('cars-zstd.parquet'), new ParquetWriteOptions()
    .compressionCodec(CompressionCodecName.ZSTD)
)

cars.write([compressionCodec: 'UNCOMPRESSED'], new File('cars-uncompressed.parquet'))
```

Supported codec names are the values from `org.apache.parquet.hadoop.metadata.CompressionCodecName`, including `UNCOMPRESSED`, `SNAPPY`, `GZIP`, `ZSTD`, and `LZ4_RAW`. Reading is automatic; no reader-side compression option is needed.

## Numeric and Date Types

`BigDecimal` columns are stored as Parquet `DECIMAL` values. By default, precision and scale are inferred from the data:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetWriter

Matrix invoices = Matrix.builder('invoices')
    .columns(id: [1, 2], amount: [123.45G, 98765.43G])
    .types(Integer, BigDecimal)
    .build()

MatrixParquetWriter.write(invoices, new File('invoices.parquet'))
```

If you need fixed metadata, provide precision and scale:

```groovy
MatrixParquetWriter.write(invoices, new File('invoices-fixed.parquet'), [
    amount: [10, 2]
])
```

`BigInteger` columns are written as `BINARY` with a `DECIMAL(precision, 0)` annotation, with precision inferred from the actual values. This avoids the `Long` range limit that older versions hit when writing very large integers.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetWriter

Matrix identifiers = Matrix.builder('identifiers')
    .columns(id: [new BigInteger('123456789012345678901234567890')])
    .types(BigInteger)
    .build()

MatrixParquetWriter.write(identifiers, new File('identifiers.parquet'))
```

`java.util.Date` values round-trip as epoch milliseconds when Matrix metadata is present. `LocalDateTime` and `Timestamp` values use microsecond timestamp precision, and `Time` values use millisecond precision.

## Time Zones

`LocalDateTime` values are converted through UTC using the system default time zone unless you specify one:

```groovy
import java.time.ZoneId
import java.time.LocalDateTime
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

Matrix events = Matrix.builder('events')
    .columns(name: ['created'], occurredAt: [LocalDateTime.of(2026, 6, 20, 12, 0)])
    .types(String, LocalDateTime)
    .build()

MatrixParquetWriter.write(events, new File('events.parquet'), ZoneId.of('Europe/Stockholm'))
Matrix stockholm = MatrixParquetReader.read(new File('events.parquet'), ZoneId.of('Europe/Stockholm'))
```

The same option is available through the builder and SPI maps as `zoneId`.

## Index Metadata

Matrix indexes created with `createIndex(...)` are preserved in Parquet metadata:

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
```

In 0.6.0, index column names are stored under `matrix.indexColumns` as a JSON string array so names containing commas round-trip correctly. The reader still accepts the legacy comma-delimited metadata form. Older Matrix versions and external consumers that split `matrix.indexColumns` on commas will not parse newly-written index metadata correctly.

## Reading External Parquet Files

Files not written by Matrix are still readable. Without Matrix metadata, column types are inferred from the Parquet schema and logical annotations. A plain `BINARY` column that Matrix metadata declares as `BigDecimal` is parsed from its UTF-8 text content, which supports external files that encode decimals as strings.

Some type information cannot be recovered from external files. For example, a generic Parquet `DECIMAL` logical type is read as `BigDecimal`; without Matrix metadata, the reader cannot distinguish an original `BigInteger` column from a scale-0 decimal column.

## Memory Model

Matrix is an in-memory data structure. Reading a Parquet file loads the full result into memory. For very large files, filter or prepare the data upstream, increase JVM heap size, or use big-data tooling for the initial reduction before importing into Matrix.

## Third-Party Libraries

The module uses Apache Parquet and Hadoop libraries directly:

- `org.apache.parquet:parquet-column`
- `org.apache.parquet:parquet-hadoop`
- `org.apache.hadoop:hadoop-common`
- `org.apache.hadoop:hadoop-mapreduce-client-core`

## Best Practices

1. Use the generic `Matrix.read(...)` / `matrix.write(...)` API for simple file workflows.
2. Use `MatrixParquetWriter.builder(...)` when combining compression, decimal metadata, and time-zone options.
3. Keep `inferPrecisionAndScale` enabled unless you need a fixed schema contract.
4. Record the `matrix.indexColumns` JSON metadata change if older readers or external tools consume your Parquet metadata.
5. Test representative data before writing very large files, especially nested maps/lists and high-precision numeric columns.

In the next section, we'll explore the matrix-bigquery module, which provides functionality for interacting with Google BigQuery.

Go to [previous section](10-matrix-bom.md) | Go to [next section](12-matrix-bigquery.md) | Back to [outline](outline.md)
