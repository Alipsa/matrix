# Matrix-Parquet

This module enables import and export of [Apache Parquet](https://parquet.apache.org/) files to and from Matrix objects using `MatrixParquetWriter` and `MatrixParquetReader`.

## At a Glance

| Goal | Entry point |
|------|-------------|
| Read a file | `MatrixParquetReader.builder().read(file)` |
| Write a file | `MatrixParquetWriter.builder(matrix).write(file)` |
| Read via SPI | `Matrix.read(file)` |
| Write via SPI | `matrix.write(file)` |
| Discover read options | `Matrix.listReadOptions('parquet')` / `ParquetReadOptions.describe()` |
| Discover write options | `Matrix.listWriteOptions('parquet')` / `ParquetWriteOptions.describe()` |

## API Surface

**Direct API (recommended)**
- `MatrixParquetReader.builder()` — fluent reader with `.matrixName()`, `.zoneId()`, `.read(File/Path/URL/InputStream/byte[])` 
- `MatrixParquetWriter.builder(matrix)` — fluent writer with `.precision()`, `.scale()`, `.decimalMeta()`, `.compressionCodec()`, `.zoneId()`, `.inferPrecisionAndScale()`, `.write(File/Path/OutputStream)`, `.writeBytes()`

**SPI (generic Matrix API)**
- `Matrix.read(options, file)` — options map with keys `matrixName`, `zoneId`
- `matrix.write(options, file)` — options map with keys `inferPrecisionAndScale`, `precision`, `scale`, `decimalMeta`, `compressionCodec`, `zoneId`

**Typed options classes**
- `ParquetReadOptions` — typed options for reading; use `ParquetReadOptions.describe()` for runtime discovery
- `ParquetWriteOptions` — typed options for writing; use `ParquetWriteOptions.describe()` for runtime discovery

## Default Behavior

**Reading**
- Matrix name for file reads: `matrixName` option wins; otherwise the file basename without extension (e.g. `data.parquet` → matrix named `data`)
- Matrix name for stream/byte-array/URL reads: `matrixName` option wins; otherwise the Parquet schema/message name when available
- Timezone: system default; override with `zoneId` option

**Writing**
- Parquet message type name: `matrix.matrixName` when present, otherwise `MatrixSchema`
- BigDecimal: precision and scale are inferred from the data by default (`inferPrecisionAndScale = true`)
- Compression: `SNAPPY` by default; override with the `compressionCodec` option (e.g. `GZIP`, `ZSTD`, `UNCOMPRESSED`)
- Timezone: system default; override with `zoneId` option
- Nested Map columns: stored as MAP when value types are homogeneous, as STRUCT when heterogeneous
- Index columns: written to `matrix.indexColumns` metadata as a JSON string array so column names containing commas round-trip; readers also accept the legacy comma-delimited metadata form

## Installation

Add the following to your Gradle build script:
```groovy
implementation 'org.apache.groovy:groovy:5.0.5'
implementation 'se.alipsa.matrix:matrix-core:3.7.1'
implementation 'se.alipsa.matrix:matrix-parquet:0.5.0'
```

## Basic Usage

### Using Matrix.read() / matrix.write()

If `matrix-parquet` is on the classpath, `.parquet` files are also available through the generic Matrix SPI API:

```groovy
import se.alipsa.matrix.core.Matrix

Matrix data = Matrix.read(new File('data.parquet'))
Matrix stockholm = Matrix.read([matrixName: 'events', zoneId: 'Europe/Stockholm'], new File('events.parquet'))

data.write([inferPrecisionAndScale: true], new File('copy.parquet'))
data.write([precision: 38, scale: 18, zoneId: 'Europe/Stockholm'], new File('money.parquet'))
data.write([decimalMeta: [amount: [8, 2]]], new File('money-meta.parquet'))

println Matrix.listReadOptions('parquet')
println Matrix.listWriteOptions('parquet')
```

### Reading a Parquet file
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader

File file = new File("data.parquet")
Matrix matrix = MatrixParquetReader.read(file)

// Or with a custom matrix name
Matrix matrix = MatrixParquetReader.read(file, "myData")

// Read from a Path
Matrix matrix = MatrixParquetReader.read(file.toPath())

// Read from an InputStream
new FileInputStream(file).withCloseable { is ->
  Matrix matrix = MatrixParquetReader.read(is)
}

// Read from a URL (e.g. file:// or https://)
URL url = file.toURI().toURL()
Matrix matrix = MatrixParquetReader.read(url)

// Read from a file path string
Matrix matrix = MatrixParquetReader.readFile("/path/to/data.parquet")
```

### Writing a Matrix to a Parquet file
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetWriter
import se.alipsa.matrix.datasets.Dataset

Matrix data = Dataset.cars().withMatrixName('cars')
File file = new File("cars.parquet")
MatrixParquetWriter.write(data, file)

// Or write to a directory (filename derived from matrix name)
File dir = new File("output/")
MatrixParquetWriter.write(data, dir)  // Creates output/cars.parquet
```

### Builder API (recommended)

The builder API provides a fluent, discoverable interface for reading and writing:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

// Writing
MatrixParquetWriter.builder(matrix)
    .precision(38)
    .scale(18)
    .zoneId('Europe/Stockholm')
    .write(new File('output.parquet'))

// Writing to byte array
byte[] bytes = MatrixParquetWriter.builder(matrix)
    .inferPrecisionAndScale(true)
    .writeBytes()

// Writing to OutputStream
outputStream.withCloseable { os ->
  MatrixParquetWriter.builder(matrix).write(os)
}

// Reading
Matrix data = MatrixParquetReader.builder()
    .matrixName('myData')
    .zoneId('Europe/Stockholm')
    .read(new File('data.parquet'))

// Reading from byte array, InputStream, URL, or Path also supported
Matrix data = MatrixParquetReader.builder()
    .read(parquetBytes)
```

## Supported Data Types

| Java/Groovy Type   | Parquet Type                   | Notes                                             |
|--------------------|--------------------------------|---------------------------------------------------|
| Integer            | INT32                          |                                                   |
| Long               | INT64                          |                                                   |
| BigInteger         | BINARY (DECIMAL, scale=0)      | Precision auto-inferred from data; no `Long` range limit |
| Float              | FLOAT                          |                                                   |
| Double             | DOUBLE                         |                                                   |
| BigDecimal         | FIXED_LEN_BYTE_ARRAY (DECIMAL) | See [BigDecimal Precision](#bigdecimal-precision) |
| Boolean            | BOOLEAN                        |                                                   |
| LocalDate          | INT32 (DATE)                   |                                                   |
| java.sql.Date      | INT32 (DATE)                   |                                                   |
| java.sql.Time      | INT32 (TIME_MILLIS)            | Millisecond precision                             |
| LocalDateTime      | INT64 (TIMESTAMP_MICROS)       | Microsecond precision                             |
| java.sql.Timestamp | INT64 (TIMESTAMP_MICROS)       | Microsecond precision                             |
| java.util.Date     | INT64                          | Epoch milliseconds                                |
| List               | LIST                           | See [Nested Structures](#nested-structures)       |
| Map                | MAP or STRUCT                  | See [Nested Structures](#nested-structures)       |
| Other              | BINARY (STRING)                | Stored as toString()                              |

## BigDecimal Precision

BigDecimal values require special handling to preserve precision. There are three approaches:

### 1. Automatic Precision Inference (Default)
```groovy
// Automatically infers precision and scale from the data
MatrixParquetWriter.write(data, file)
// Or explicitly:
MatrixParquetWriter.write(data, file, true)
```

### 2. Uniform Precision for All BigDecimal Columns
```groovy
// Use precision=38, scale=18 for all BigDecimal columns
MatrixParquetWriter.write(data, file, 38, 18)
```

### 3. Per-Column Precision Specification
```groovy
// Specify precision and scale for specific columns
MatrixParquetWriter.write(data, file, [
    salary: [10, 2],   // precision=10, scale=2
    rate: [8, 4]       // precision=8, scale=4
])
```

**Note:** If the specified precision is too small for a value, an `IllegalArgumentException` will be thrown with a descriptive message indicating the required vs. allowed precision.

## Nested Structures

The module supports nested Groovy collections:

```groovy
def data = Matrix.builder('products').data(
    id: [1, 2],
    tags: [['electronics', 'sale'], ['furniture']],           // List
    attributes: [[color: 'red', size: 'L'], [color: 'blue']], // Map as STRUCT
    reviews: [[[rating: 5, user: 'A']], [[rating: 4, user: 'B']]] // List of Maps
).types([Integer, List, Map, List]).build()

MatrixParquetWriter.write(data, file)
Matrix restored = MatrixParquetReader.read(file)
```

### Nested Structure Limitations
- Maps with heterogeneous value types are stored as STRUCT (preserves key names as field names)
- Maps with homogeneous value types are stored as MAP (standard Parquet map)
- Deeply nested structures are supported but may impact performance
- Bean/POJO objects are converted to Maps using reflection

## Timezone Handling

By default, `LocalDateTime` values are converted to/from UTC using the system default timezone. To use a specific timezone:

```groovy
import java.time.ZoneId

// Writing with a specific timezone
MatrixParquetWriter.write(data, file, ZoneId.of("America/New_York"))

// Reading with a specific timezone
Matrix matrix = MatrixParquetReader.read(file, ZoneId.of("America/New_York"))

// Reading with name and timezone
Matrix matrix = MatrixParquetReader.read(file, "myData", ZoneId.of("Europe/London"))
```

**Important:** For consistent round-trip behavior, use the same timezone for writing and reading. If files are shared across systems with different default timezones, explicitly specify the timezone.

## Compression

Parquet files are compressed with `SNAPPY` by default. Override per write:

```groovy
import org.apache.parquet.hadoop.metadata.CompressionCodecName

// Builder API
MatrixParquetWriter.builder(data)
    .compressionCodec(CompressionCodecName.GZIP)
    .write(file)

// String form (also accepted)
MatrixParquetWriter.builder(data)
    .compressionCodec('ZSTD')
    .write(file)

// SPI options map
data.write([compressionCodec: 'UNCOMPRESSED'], file)
```

Supported codec names: `UNCOMPRESSED`, `SNAPPY`, `GZIP`, `ZSTD`, `LZ4_RAW` (any value of `org.apache.parquet.hadoop.metadata.CompressionCodecName`). Compression is transparent on read — `MatrixParquetReader` auto-detects the codec from the file footer, no reader-side configuration is needed.

## Known Limitations

### Memory Usage
Matrix is an **in-memory data structure**. The entire Parquet file is loaded into memory when reading. Ensure you have sufficient RAM available for:
- The raw data size
- Java object overhead (typically 2-3x the raw data size)
- Working memory for processing

For very large datasets that don't fit in memory, consider:
- Processing files in chunks externally before loading
- Using dedicated big data tools (Spark, Hadoop) for initial filtering
- Increasing JVM heap size (`-Xmx`)

### JDK Compatibility
This module requires **JDK 21** and is limited to JDK 21 maximum due to Hadoop 3.4.x compatibility constraints. JDK 22+ is not supported.

### Timestamp Precision
- `LocalDateTime` and `Timestamp` are stored with **microsecond** precision
- `Time` is stored with **millisecond** precision
- Nanosecond precision is not preserved

### Type Metadata
Column type information is stored in Parquet file metadata. When reading files not created by `MatrixParquetWriter`:
- Types are inferred from Parquet schema logical type annotations
- Some type information may be lost (e.g., `java.sql.Timestamp` vs `LocalDateTime`)
- `BigInteger` columns read without Matrix metadata (e.g. external files) are inferred as `BigDecimal` with scale 0, since both the Parquet schema and the inference logic only see a generic `DECIMAL` logical annotation
- `BigInteger` values nested inside `List`/`Map` columns always get a declared precision of 38 regardless of their actual digit count (top-level column precision inference does not extend into nested structures), and round-trip back as numerically-equal `BigDecimal` values rather than `BigInteger` (nested elements carry no expected-type metadata, unlike top-level columns). This does not cause data loss or truncation, but external Parquet readers that validate `DECIMAL` precision against declared metadata rather than actual byte length may reject or misinterpret values with more than 38 digits

## Technical Notes

### Thread Safety
Both `MatrixParquetWriter` and `MatrixParquetReader` use thread-safe caching for:
- PropertyDescriptor lookups (for struct handling)
- Class.forName() results (for type parsing)

The timezone parameter uses thread-local storage, making concurrent writes/reads with different timezones safe.

### Performance Tips
1. Use `inferPrecisionAndScale=true` (default) for automatic BigDecimal handling
2. For large matrices with struct columns, the first write may be slower due to reflection caching
3. Subsequent operations on the same struct types benefit from cached metadata

## 3rd Party Libraries

| Library | Purpose | License |
|---------|---------|---------|
| org.apache.parquet:parquet-column | Parquet columnar format | Apache 2.0 |
| org.apache.parquet:parquet-hadoop | Hadoop integration | Apache 2.0 |
| org.apache.hadoop:hadoop-common | Hadoop utilities | Apache 2.0 |
| org.apache.hadoop:hadoop-mapreduce-client-core | MapReduce client | Apache 2.0 |
