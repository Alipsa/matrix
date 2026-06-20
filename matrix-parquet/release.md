# Matrix-parquet Release History

## v0.6.0, 2026-06-19
- Upgrade dependencies
  - org.apache.parquet:parquet-column 1.17.0 -> 1.17.1
  - org.apache.parquet:parquet-hadoop 1.17.0 -> 1.17.1 
- Add compression codec support: `ParquetWriteOptions.compressionCodec` / `WriterBuilder.compressionCodec(...)` / SPI `compressionCodec` option; default changed from `UNCOMPRESSED` to `SNAPPY`
- Fix bug: `BigInteger` columns silently truncated when their value exceeded `Long` range (mapped to `INT64` via `.longValue()`); now mapped to `BINARY` with a `DECIMAL(precision, 0)` logical annotation, with precision auto-inferred from the data
- Breaking metadata-format change: indexed column names are now written to `matrix.indexColumns` as a JSON string array instead of comma-delimited text so names containing commas round-trip; the new reader accepts both JSON and legacy comma-delimited metadata, but older readers and external tools that split the metadata value on commas will not parse new files correctly

## v0.5.0, 2026-04-29
- Add SPI integration: `MatrixParquetFormatProvider` registers `.parquet` extension with Matrix SPI so `Matrix.read(file)` and `matrix.write(file)` work without explicit imports
- Add `ParquetReadOptions` and `ParquetWriteOptions` typed options classes with `describe()` and `fromMap()` for runtime discovery and SPI use
- Add builder API: `MatrixParquetReader.builder()` and `MatrixParquetWriter.builder(matrix)` as the recommended fluent interface
- Add `write(OutputStream)` and `write(Path)` overloads to writer; add `read(byte[])`, `read(URL)`, `read(Path)`, `read(InputStream)` to builder
- Add `writeBytes()` method to write to a byte array without a file
- Strengthen `ParquetWriteOptions.validate()`: enforce `precision > 0`, `scale >= 0`, `scale ≤ precision` for both uniform and per-column decimal meta
- Add `decimalMeta` per-column precision/scale validation in `validateDecimalMeta` (checks shape, range, and consistency)
- Fix bug: resource leak in `MatrixParquetReader` — reader now wrapped in `withCloseable` to ensure streams are always closed
- Fix bug: negative `BigDecimal` values padded with `0x00` instead of `0xFF` causing sign bit corruption on read
- Fix bug: timestamp type mapped to `MILLIS` instead of `MICROS` causing precision loss
- Fix bug: deprecated `BigDecimal.ROUND_HALF_UP` replaced with `RoundingMode.HALF_UP`
- Fix `hasUniformPrecisionAndScale()` semantics: now returns true only when both `precision` and `scale` are non-null
- Enable `@CompileStatic` by default for all production sources
- Enable CodeNarc with `ignoreFailures=false`; fix all pre-existing violations
- Remove ivy dependency (was unused)
- Make internal APIs private; simplify `readFromInputStream`
- Migrate tests to `@TempDir`; remove manual temp-file cleanup boilerplate
- Correct POM publication URLs: `url`, `license.url`, and `scm.url` now use `tree/main` paths (Maven Central convention)
- Add README sections: "At a Glance" goal/entry-point table, "API Surface" reference, "Default Behavior" for naming and decimal defaults
- Upgrade dependencies
  - org.apache.hadoop:hadoop-common [3.4.2 -> 3.4.3]
  - org.apache.hadoop:hadoop-mapreduce-client-core [3.4.2 -> 3.4.3]

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
