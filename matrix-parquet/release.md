# Matrix-parquet Release History

## v0.4.1, in progress
- org.apache.hadoop:hadoop-common 3.4.2 -> 3.4.3
- org.apache.hadoop:hadoop-mapreduce-client-core 3.4.2 -> 3.4.3

## v0.4.0, 2026-01-31
- remove parquet-carpet dependency (MatrixCarpetIO) - now using native Parquet implementation
- add support for nested structures: structs (POJOs, maps) and repeated fields (arrays)
- add URL, Path, InputStream, and byte[] input support to MatrixParquetReader (API consistency with matrix-csv and matrix-json)
- add BigDecimal precision and scale control in MatrixParquetWriter write methods
- add in-memory write support via InMemoryOutputFile and InMemoryPositionOutputStream (eliminates temporary files)
- add timezone support for timestamp handling (optional parameter in reader/writer methods)
- MatrixParquetWriter can now write to either a file or directory (using matrix name for filename)
- use matrixName as Parquet schema name if present
- add @CompileStatic to MatrixParquetReader for performance and type safety
- add comprehensive input validation to both reader and writer (null checks, empty matrix, file existence)
- add safeFileName sanitization for directory targets (strips path separators and unsafe characters)
- fix bug: time precision schema/implementation mismatch (now uses MICROS for timestamps, MILLIS for time)
- fix bug: BigDecimal schema inference incorrectly set minimum scale to 2
- extract magic strings to constants for maintainability
- add comprehensive GroovyDoc to MatrixParquetReader and MatrixParquetWriter
- add extensive test coverage including edge cases, validation, and round-trip verification
- cache reflection metadata for struct handling (performance optimization)
- upgrade dependencies
  - org.apache.hadoop:hadoop-common [3.4.1 -> 3.4.2]
  - org.apache.hadoop:hadoop-mapreduce-client-core [3.4.1 -> 3.4.2]
  - org.apache.parquet:parquet-column [1.15.2 -> 1.16.0]
  - org.apache.parquet:parquet-hadoop [1.15.2 -> 1.16.0]

## v0.3.0, 2025-05-28
- Add a "native" parquet implementation in the form of MatrixParquetReader and MatrixParquetWriter.
Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-parquet/0.3.0/matrix-parquet-0.3.0.jar

## v0.2, 2025-03-12
- Require jdk21
Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-parquet/0.2/matrix-parquet-0.2.jar

## v0.1, 2025-02-16
- initial release