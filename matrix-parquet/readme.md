# Matrix-Parquet

This module enables import and export of [Apache Parquet](https://parquet.apache.org/) files to and from Matrix objects using `MatrixParquetWriter` and `MatrixParquetReader`.

## Installation

Add the following to your Gradle build script:
```groovy
implementation 'org.apache.groovy:groovy:5.0.3'
implementation 'se.alipsa.matrix:matrix-core:3.3.0'
implementation 'se.alipsa.matrix:matrix-parquet:0.4.0'
```

## Basic Usage

### Reading a Parquet file
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader

File file = new File("data.parquet")
Matrix matrix = MatrixParquetReader.read(file)

// Or with a custom matrix name
Matrix matrix = MatrixParquetReader.read(file, "myData")
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

## Supported Data Types

| Java/Groovy Type | Parquet Type | Notes |
|------------------|--------------|-------|
| Integer | INT32 | |
| Long | INT64 | |
| BigInteger | INT64 | |
| Float | FLOAT | |
| Double | DOUBLE | |
| BigDecimal | FIXED_LEN_BYTE_ARRAY (DECIMAL) | See [BigDecimal Precision](#bigdecimal-precision) |
| Boolean | BOOLEAN | |
| LocalDate | INT32 (DATE) | |
| java.sql.Date | INT32 (DATE) | |
| java.sql.Time | INT32 (TIME_MILLIS) | Millisecond precision |
| LocalDateTime | INT64 (TIMESTAMP_MICROS) | Microsecond precision |
| java.sql.Timestamp | INT64 (TIMESTAMP_MICROS) | Microsecond precision |
| java.util.Date | INT64 | Epoch milliseconds |
| List | LIST | See [Nested Structures](#nested-structures) |
| Map | MAP or STRUCT | See [Nested Structures](#nested-structures) |
| Other | BINARY (STRING) | Stored as toString() |

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
