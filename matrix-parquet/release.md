# Matrix-parquet Release History

## v0.8.0 (unreleased)

Add CDC/page-index interoperability fixtures; a column-oriented row-group reader; typed predicates,
projection, limits and closeable batches; strict sharded datasets with bounded parallel decoding;
HTTP range reads; Hub dataset URI resolution; and shared writer tuning. Local files and byte arrays
use InputFile directly. New files carry `matrix.parquet.version=2`.

Complete defaults, supported types and end-to-end examples are in [the module guide](readme.md).

| Behavior | 0.8.0 result | Compatibility/opt-out |
|---|---|---|
| New Matrix strings | STRING-annotated BINARY recursively | No opt-out; standard annotation fixes interoperability |
| Legacy Matrix plain BINARY text | Remains String, including nested text/map keys | Automatic; metadata carrying the new format marker overrides legacy inference |
| 0.7.x byte[] columns | Fail with a contextual unrecoverable-legacy-binary error | The pinned 0.7.x writer fails with InvalidSchemaException for byte[]; legacy `[B` text payloads cannot recover bytes; regenerate from source |
| JSON / ENUM BINARY | UTF-8 String, including dictionary/null-only cases | Preserves pre-0.8 text behavior; BSON remains byte[] |
| External raw BINARY | byte[] | `binaryAsString(true)` restores prior UTF-8 behavior |
| Unadjusted timestamps | Local wall-clock time preserved | No bug-preserving flag; applications needing the former shift can explicitly convert the local value through UTC into the desired zone |
| Integer-backed DECIMAL | BigDecimal with declared scale | No bug-preserving flag; explicit `unscaledValue()` obtains prior raw numeric value |
| UUID annotation | UUID | No text-decoding compatibility flag; explicit UUID/string/byte conversion at application boundary |
| FLOAT16 | Contextual unsupported-type error | No UTF-8 fallback; convert explicitly before reading |
| External INT96 | Read-only UTC timestamp support, returned in reader zone or as compatible metadata type | Residual filters; writer never emits INT96 |
| Unsigned integers | UINT8/16 → Integer, UINT32 → Long, UINT64 → BigInteger | Correct unsigned values; residual-only predicates |
| Timestamp precision | LocalDateTime/Timestamp/Instant default to TIMESTAMP(MICROS), consistently truncating sub-microsecond digits; Instant.now() works on Linux/Windows | timestampUnit('NANOS') preserves precision within int64 range; intentional exception to lossless-decimal default, bounded below 1 µs |
| Dataset names | Directory name for directory reads; first filename without extension for lists/globs | matrixName overrides collected/batched names |
| String-declared GString | CharSequence values accepted and converted to text | Other declared-type checks remain strict |
| Nested type metadata | matrix.nestedTypes preserves canonical nested leaf classes | Older readers ignore key; missing/unknown/incompatible metadata retains inference |
| Byte / Short fallback | Signed INT32 with INT(8/16), restored as Byte/Short | Legacy unannotated text stays String; explicitly convert to String before writing to retain text output |
| LocalTime fallback | TIME(NANOS), restored as LocalTime using new-format metadata | Legacy text stays String; explicit String conversion retains text output |
| Instant fallback | TIMESTAMP(MICROS, adjusted=true), restored as Instant using new-format metadata | Legacy text stays String; truncate sub-microsecond digits under MICROS, reject int64 overflow; NANOS preserves precision |
| UUID writer fallback | FIXED[16] + UUID, restored as UUID | Legacy text stays String; explicit String conversion retains text output |
| Character fallback | STRING, returned as String | Normalize emitted metadata to String; Character identity is not preserved |
| Enum fallback | STRING using enum name(), returned as String | Normalize metadata to String; convert explicitly for former overridden toString() output |
| ZonedDateTime / OffsetDateTime fallback | Explicit unsupported-type error | Convert to Instant to retain instant, or String to retain zone/offset text; no implicit zone loss |
| Object column, single supported policy type | That type's encoding and canonical metadata | Reads back typed as the inferred canonical class instead of Object; no explicit types required |
| Abstract/interface declared supertype (Number, CharSequence, Comparable, Serializable, Temporal) | Validate assignability, infer one supported policy type and emit its canonical name | Reads back as the resolved class instead of the declared supertype; normalize heterogeneous values explicitly |
| Concrete declared type with incompatible runtime values | Validation error before writing | Box primitives then require every non-null value to be an instance; no silent schema/metadata mismatch |
| BigDecimal, declared or inferred through a supertype | Exact FIXED_LEN_BYTE_ARRAY DECIMAL with resolved precision/scale | Remove DOUBLE fallback; infer by default, or require explicit decimal settings when inference is disabled; no lossy compatibility mode |
| All-null Number/Temporal declaration | Validation error before writing | New write failure: retype explicitly to a supported concrete type; no nominal String metadata |
| Concrete Date column mixing Date/Timestamp or sql.Date/Timestamp | Validation error before writing | Runtime encoding policies differ; normalize every value to one supported temporal class before writing |
| All-null Object column | STRING/BINARY with canonical String metadata | Retain current text schema behavior; reads back as String-typed column |
| Mixed/unsupported Object column or other custom types | Contextual validation error before writing | Name the first offending row/class; explicitly normalize values or map to String |

Unknown metadata class names are never loaded and fall back to physical/logical schema inference.
Incompatible metadata declarations also fall back rather than creating falsely typed columns.
Legacy Matrix text compatibility applies recursively; new raw binary metadata remains authoritative.
The checked-in original 0.7.x string fixture predates these writer changes. Legacy byte[] recovery
claims were narrowed after an end-to-end check found InvalidSchemaException in this checkout.

External INT96 decodes as adjusted UTC timestamps; FLOAT16 raises contextual unsupported-type errors. External TIME still returns
java.sql.Time at millisecond precision; exact predicate filtering preserves original precision.
Mixed Date/Timestamp policies are now rejected before writing, preventing the prior 1000× unit
corruption. New Object/supertype values are resolved consistently for schema and metadata.
Unknown SPI options are now rejected. Dataset reads restore common compatible Matrix indices, require matching type
metadata, and reject empty selections and duplicate paths.

Review fixes also cover empty Arrow dictionaries; consistent static URL/Hub and directory overloads;
local source columns; daemon dataset workers; null option-map defaults; path-segment validation;
projection around legacy `[B` columns; one validated type-policy scan per column; and atomic output
replacement on success. New files use normal process/umask permissions; existing POSIX permissions
are retained, and writes through existing symbolic links update the resolved target while preserving
the link. Local file writes require same-directory atomic replacement support; unsupported network
or FUSE filesystems raise `AtomicMoveNotSupportedException` and leave existing output unchanged.
HTTP bulk requests reuse resolved targets and a separately bounded cache.

October review fixes also add exact residual fallback for unrepresentable predicate bounds,
partial AND pruning, canonical container inference, full nested error paths, strict bloom/codec
validation, secure read spooling, bounded cross-platform glob parsing, weak-ETag preconditions,
legacy MAP layouts, and indexed column access in the write loop; dead reader/POJO paths were removed.

Native CDC writing remains deferred to upstream parquet-java #3818; the Python rewrite preserves
custom metadata. Hadoop stays as a runtime dependency because codec initialization still loads it.

## v0.7.0, 2026-09-22
- SPI `inferPrecisionAndScale` parsing is now strict: Boolean values and trimmed, case-insensitive `true`/`false` strings are accepted; former truthy inputs are rejected.
- Breaking change: explicit DECIMAL scale conversion now defaults to `RoundingMode.UNNECESSARY`, rejecting lossy conversion with a contextual `IllegalArgumentException`. Set `roundingMode: 'HALF_UP'` (or another named `RoundingMode`) when rounding is intentional.
- Add typed `roundingMode(RoundingMode)` and `roundingMode(String)` options to `ParquetWriteOptions` and `MatrixParquetWriter.WriterBuilder`, plus the `write(matrix, file, precision, scale, roundingMode)` overload.
- Fix explicit DECIMAL validation to reject values whose rescaled decimal precision exceeds the schema precision, even if their two's-complement representation fits the fixed-length field.
- Fix directory-target writes to normalize Matrix names to a single Windows-safe `.parquet` filename and confine it to the supplied existing directory. Explicit file targets remain unchanged. Note: a Matrix already named `foo.parquet` previously produced `foo.parquet.parquet` and now produces `foo.parquet`, so an existing output filename can change on upgrade.
- Fix `java.sql.Time` round trips to retain milliseconds and infer external `TIME(MILLIS)`, `TIME(MICROS)`, and `TIME(NANOS)` fields as `Time`; microsecond and nanosecond inputs are downgraded to milliseconds.
- Upgrade dependencies
  - org.apache.parquet:parquet-column 1.17.1 -> 1.18.1
  - org.apache.parquet:parquet-hadoop 1.17.1 -> 1.18.1

## v0.6.0, 2026-06-20
- Upgrade dependencies
  - org.apache.parquet:parquet-column 1.17.0 -> 1.17.1
  - org.apache.parquet:parquet-hadoop 1.17.0 -> 1.17.1
- Add compression codec support: `ParquetWriteOptions.compressionCodec` / `WriterBuilder.compressionCodec(...)` / SPI `compressionCodec` option; default changed from `UNCOMPRESSED` to `SNAPPY`
- Fix bug: `BigInteger` columns silently truncated when their value exceeded `Long` range (mapped to `INT64` via `.longValue()`); now mapped to `BINARY` with a `DECIMAL(precision, 0)` logical annotation, with precision auto-inferred from the data
- Fix bug: reading a `BigDecimal`-typed column stored as plain `BINARY` with no `DECIMAL` annotation (e.g. an externally-written file) incorrectly called `getDouble` on binary data; now parses the UTF-8 string as a `BigDecimal`
- Fix bug: Parquet writers are now closed if row writing fails, preventing leaked file handles on exceptions
- Fix bug: documented `java.util.Date` support now stores epoch milliseconds as `INT64` and restores `Date` values when Matrix metadata is present
- Improve usability: `decimalMeta` options now accept natural Groovy list syntax (`[precision, scale]`) and all write paths validate precision/scale consistently
- Improve usability: stream, byte-array, and URL reads now derive the matrix name from the Parquet schema/message name when no explicit `matrixName` is supplied
- Fix bug: index column metadata is now stored as JSON, so index column names containing commas round-trip correctly while legacy comma-delimited metadata remains readable
- Breaking metadata-format change: indexed column names are now written to `matrix.indexColumns` as a JSON string array instead of comma-delimited text; older readers and external tools that split the metadata value on commas will not parse new files correctly
- Remove dead/redundant code: unreachable commented-out lines in `MatrixParquetWriter.buildPrimitiveType`; redundant empty-collection special case in `writeList`

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
