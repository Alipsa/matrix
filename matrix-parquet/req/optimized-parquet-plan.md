# Hugging Face optimized Parquet: analysis and enhancement plan

Analysis date: 2026-10-17. Scope: compatibility and baseline measurements (P0), columnar and bounded local reads,
typed predicates and writer tuning (P1), sharded datasets and HTTP ranges (P2),
optional Hub resolution and upstream CDC tracking (P3). Implementation and verification evidence is recorded at the end of this document.
The findings below describe the pre-implementation baseline.

## Findings

### What “optimized Parquet” means

Hugging Face describes optimized Parquet as ordinary Parquet written with content-defined
chunking (CDC) and page indexes. These are writer/layout features, not a separate container
format, codec, file extension, or reader mode. PyArrow exposes
`use_content_defined_chunking=True` and `write_page_index=True`; its CDC option requires
PyArrow 21 or later. [Hugging Face documentation](https://huggingface.co/docs/hub/en/datasets-libraries),
[PyArrow integration](https://huggingface.co/docs/hub/en/datasets-pyarrow).

CDC chooses boundaries from logical column content before encoding/compression. Readers
decode the resulting ordinary pages without reproducing the chunking algorithm. Xet can
reuse matching stored chunks across revisions; reading a downloaded local file does not
itself invoke Xet or give matrix-parquet incremental download capability.
[Hugging Face CDC explanation](https://huggingface.co/blog/parquet-cdc).

Page indexes consist of `ColumnIndex` (page statistics) and `OffsetIndex` (page locations
and first-row positions), associated with column chunks. They support skipping candidate
pages and aligning reads across columns. Indexes are optional, and page boundaries need
not align across columns. Filtering must still reject nonmatching records within candidate
pages. [Parquet page-index specification](https://parquet.apache.org/docs/file-format/pageindex/).

PyArrow can move page statistics from page headers to the index when index writing is
enabled. Thus tests must include real PyArrow output rather than assuming that Java-written
round trips cover this layout. CDC and page indexes are independent switches; test all four
combinations. Do not assume fixed page sizes or infer CDC from a page-size distribution.
[PyArrow writer reference](https://arrow.apache.org/docs/python/generated/pyarrow.parquet.write_table.html).

### Pre-implementation repository behavior

The authoritative dependencies are **parquet-java 1.18.1**, Hadoop **3.5.0** in
`gradle/libs.versions.toml`; the module is **0.7.1-SNAPSHOT**. The older v0.6.0 plan is
historical and should not determine dependency decisions here. Keep JDK 21.

| Area | Evidence in current code | Assessment |
|---|---|---|
| Full-file decoding | `MatrixParquetReader.read(File, ...)` reads the footer and uses `ParquetReader<Group>` / `GroupReadSupport` | CDC/indexed files should decode through the existing dependency; unproven until external fixtures run |
| Selective reads | `ParquetReadOptions` and `ReaderBuilder` only expose name/time zone; core loop materializes every row and field | No caller-supplied predicate, projection, or bounded read; no useful page pruning for the current full scan |
| Remote reads | URL overloads open a stream; `readFromInputStream` copies it entirely to a temporary file | Entire file transfers before reading; page pruning cannot reduce network traffic |
| Metadata | Reader uses Matrix-specific type/index keys when present, otherwise schema inference | External files need tests without Matrix metadata; Matrix indexes are unrelated to Parquet page indexes |
| Writes | File and memory writers configure schema, codec and Matrix metadata | Index production is delegated to upstream defaults; no CDC configuration |
| Coverage | Six Groovy test classes and `mtcars.parquet` fixture; no CDC/index-specific suite | Cross-implementation and page-pruning coverage missing |

parquet-java 1.18.1 already exposes `withFilter`, `useColumnIndexFilter(boolean)`, and
record filtering. Column-index filtering is enabled by default by its read-options builder.
The gap is supplying a predicate and preserving correct materialization, rather than merely
turning an upstream flag on.
[Reader API](https://github.com/apache/parquet-java/blob/apache-parquet-1.18.1/parquet-hadoop/src/main/java/org/apache/parquet/hadoop/ParquetReader.java),
[read defaults](https://github.com/apache/parquet-java/blob/apache-parquet-1.18.1/parquet-hadoop/src/main/java/org/apache/parquet/ParquetReadOptions.java).

The upstream writer enables statistics by default and serializes column/offset indexes.
Consequently, “add page-index writing” is not yet an established missing feature: first
inspect actual matrix-parquet output for supported columns. There is no CDC option in the
examined 1.18.1 `ParquetWriter.Builder` or `ParquetProperties.Builder`. Do not introduce a
nonfunctional `optimizedParquet` or `contentDefinedChunking` option.
[Writer API](https://github.com/apache/parquet-java/blob/apache-parquet-1.18.1/parquet-hadoop/src/main/java/org/apache/parquet/hadoop/ParquetWriter.java),
[writer properties](https://github.com/apache/parquet-java/blob/apache-parquet-1.18.1/parquet-column/src/main/java/org/apache/parquet/column/ParquetProperties.java),
[index serialization](https://github.com/apache/parquet-java/blob/apache-parquet-1.18.1/parquet-hadoop/src/main/java/org/apache/parquet/hadoop/ParquetFileWriter.java).

### Public API and dependency boundary

`parquet-column` and `parquet-hadoop` are `implementation` dependencies. Exposing
`FilterPredicate` would force downstream consumers to add parquet-java to their compile
classpath or require changing publication scope to `api`. Use Matrix-owned predicate and
writer-version types instead; translate them internally. Keep parquet-java implementation
private for the new API and add downstream Gradle/Maven compile smoke tests. Existing public
codec signatures are a separate legacy exposure to characterize, not a reason to introduce
more upstream types.

### Adjacent schema risks

These affect Hugging Face datasets independently of optimization and must be tracked
separately from CDC compatibility:

- Unannotated `BINARY`/`FIXED_LEN_BYTE_ARRAY` currently defaults to `String`, and
  `readPrimitive` decodes it as UTF-8. Arbitrary image/audio bytes therefore lose fidelity.
- DECIMAL annotations on `INT32`/`INT64` are inferred as `BigDecimal`, but the corresponding
  primitive read branches return raw integers without applying decimal scale.
- TIMESTAMP decoding ignores `isAdjustedToUTC=false` and shifts local wall-clock values
  through the configured/system zone. Test naive PyArrow timestamps explicitly before declaring
  their annotation confirmed by fixtures.
- UUID and FLOAT16 annotations on FIXED_LEN_BYTE_ARRAY reach the UTF-8 fallback. INT96
  reaches `group.getString`; the exact exception remains to be characterized by a fixture. Neither
  path constitutes valid decoding.
- `readList` expects the standard three-level `list` structure. Include PyArrow compliant
  lists, nested structs and null/empty values; do not claim support for all historical LIST
  representations without additional work.

Prefer a small representative text dataset as the initial compatibility target. Include
binary/media and integer-backed decimal fixtures to make broader limitations explicit.

## Implementation sequence

### 0. Set the release and migration contract (P0 prerequisite)

- 0.1 [x] Before production changes, set `matrix-parquet/build.gradle` to `0.8.0-SNAPSHOT`; target
  release **0.8.0**. Keep P2/P3 follow-ups separately releasable rather than promising their
  delivery in 0.8.0. Add a 0.8.0 migration section to release notes with the behavior table below,
  complete option defaults and runnable examples.

- 0.2 [x] Capture representative 0.7.x-produced fixtures before changing the writer, including
  top-level/nested strings, string map keys and Matrix temporal/decimal metadata. Include a 0.7.x
  byte[] failure fixture; do not assume every old file is recoverable. Include them in the 4 MiB
  fixture budget. Add before/after characterization tests plus external fixtures for behavior
  0.7.x did not write, asserting values and runtime types. The Group reader retained for
  differential tests must receive phase-2 fixes; separately preserve original expected 0.7.x
  results for migration characterization.

  | Behavior | 0.8.0 result | Compatibility/opt-out |
  |---|---|---|
  | New Matrix strings | STRING-annotated BINARY recursively | No opt-out; standard annotation fixes interoperability |
  | Legacy Matrix plain BINARY text | Remains String, including nested text/map keys | Automatic; metadata carrying the new format marker overrides legacy inference |
  | 0.7.x byte[] columns | Fail with a contextual unrecoverable-legacy-binary error | The pinned original writer fails with InvalidSchemaException for byte[]; legacy `[B` text cannot recover bytes; regenerate from source |
  | External raw BINARY | byte[] | `binaryAsString(true)` restores prior UTF-8 behavior |
  | Unadjusted timestamps | Local wall-clock time preserved | No bug-preserving flag; applications needing the former shift can explicitly convert the local value through UTC into the desired zone |
  | Integer-backed DECIMAL | BigDecimal with declared scale | No bug-preserving flag; explicit `unscaledValue()` obtains prior raw numeric value |
  | UUID annotation | UUID | No text-decoding compatibility flag; explicit UUID/string/byte conversion at application boundary |
  | FLOAT16 / INT96 unsupported cases | Contextual unsupported-type error | No UTF-8 fallback; supported-type contract fixed before release |
  | Byte / Short fallback | Signed INT32 with INT(8/16), restored as Byte/Short | Legacy unannotated text stays String; explicitly convert to String before writing to retain text output |
  | LocalTime fallback | TIME(NANOS), restored as LocalTime using new-format metadata | Legacy text stays String; explicit String conversion retains text output |
  | Instant fallback | TIMESTAMP(MICROS, adjusted=true), restored as Instant using new-format metadata | Legacy text stays String; reject sub-microsecond precision loss and int64-micros overflow; explicitly convert to String for text output |
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

### 1. Establish compatibility and writer-index facts (P0, first deliverable)

- 1.1 [x] Add `OptimizedParquetTest.groovy` and checked-in deterministic fixtures
  under `src/test/resources/optimized/`. Add a generator script plus a manifest containing
  generator/PyArrow version, options, schema, row counts, expected values and SHA-256 checksums.
  Python is a fixture-generation tool, not a runtime or normal Gradle-test dependency.

- 1.2 [x] Generate paired ordinary/CDC-only/index-only/CDC+index files from identical values.
  Include SNAPPY and ZSTD, dictionary enabled/disabled, data pages v1/v2, multiple pages per
  column and multiple row groups. Use deterministic varied long strings so the CDC fixtures
  actually cross chunk boundaries; verify physical multi-page structure. Set a **4 MiB aggregate
  checked-in binary-fixture budget**, with a 1 MiB per-file cap. Use CDC `{'min_chunk_size': 4096,
  'max_chunk_size': 16384, 'norm_level': 0}` for small fixtures, and verify the resulting
  boundaries/pages. PyArrow 21 accepts this dictionary with both size keys required, confirmed in
  its [versioned implementation](https://github.com/apache/arrow/blob/apache-arrow-21.0.0/python/pyarrow/_parquet.pyx).
  Add an opt-in default-CDC generation/interoperability test under `./gradlew :matrix-parquet:test
  -PrunSlowTests=true`, using a build-directory fixture. Skip with an explicit reason if Python or
  pinned PyArrow is absent; do not silently install packages. Add the Python/PyArrow lane to
  `.github/workflows/release-all-tools.yml` (including its pull_request runs), invoking the opt-in
  command so this case actually runs. Fail generation errors when dependencies are available and
  enforce the checked-in budget. Pairwise coverage replaces a Cartesian product. Include
  ordered/unordered columns, null-only pages, null/empty nested lists and structs, Unicode,
  decimals and timestamps.

- 1.3 [x] Assert exact values, types, order and null semantics through file, path, bytes, stream,
  builder and SPI entry points. Use a local HTTP server for URL paths; regular tests must not
  depend on Hugging Face availability or credentials. Cover empty files and external metadata
  without Matrix-specific keys.

- 1.4 [x] Inspect index references and deserialize `ColumnIndex`/`OffsetIndex` using
  `ParquetFileReader`. Check page counts, monotonic offsets, row positions and valid bounds.
  Inspect both Java-produced file and byte-array output. Record which types produce indexes and
  any upstream omissions. Do not infer index presence from a flag.

- 1.5 [x] Repair any demonstrated CDC/index decoding failure with a targeted regression test. If
  all supported-schema fixtures pass, record that no CDC decoder change is necessary. Do not
  upgrade parquet-java solely because the files use CDC.

- 1.6 [x] Before reader changes, establish a repeatable full-scan baseline using generated
  approximately 1M-row wide-text, low-cardinality categorical and mixed/nested datasets. Keep
  large inputs outside Git. Record generation commands/seeds, JVM/heap/GC, CPU, warmup and
  repeated-run medians, rows/s, elapsed time, allocation/GC, peak heap and bytes read. Separate
  cold and warm I/O and compare ordinary/indexed/CDC+indexed layouts. Reuse these exact inputs
  throughout P1/P2; include narrow projection and selective predicates as soon as those APIs
  exist.

### 2. Fix demonstrated external-schema fidelity gaps (P0 for broad HF support)

Order within 2.0: **2.0 → 2.0c (shared descriptor) → 2.0a/2.0b/2.0d
(policies implemented on that descriptor) → 2.0e (integration verification)**.
Task identifiers remain stable for review references; their numeric order does not override
these dependencies. Do not implement policies on the old first-value inference as an interim step.

- 2.0 [x] Before 2.1, write String as BINARY + `LogicalTypeAnnotation.stringType()` through the
  shared primitive schema builder, including list elements, struct fields, map values and required
  map keys. Verify the annotations with PyArrow. Preserve existing files: when Matrix metadata
  identifies a legacy file, unannotated BINARY remains String recursively, including nested values
  whose expected type is null. New binary-capable output must include a versioned Matrix
  schema/encoding marker to distinguish raw binary from legacy nested text; do not apply legacy
  rules blindly to newly written media bytes. Add nullable Boolean `binaryAsString` (default
  null/AUTO): null uses legacy-Matrix text compatibility and raw bytes for external/new-format
  files; true forces unannotated BINARY to UTF-8 strings recursively; false forces raw bytes
  recursively. Annotations take precedence; explicitly declared Matrix String fields remain
  strings. Define FIXED binary separately: never override DECIMAL/UUID/FLOAT16 annotations.
  Propagate through options, builder and SPI, and test old nested strings/map keys plus new mixed
  binary/text fields.

- 2.0a [x] Require raw byte[] writing as BINARY without STRING annotation, with the new format
  marker, at top level and inside nested structures. Add an explicit byte[] branch using Parquet
  Binary bytes; reject unsupported schema/value combinations with contextual errors instead of the
  `value.toString()` fallback only after implementing the explicit type policies in 2.0b–2.0d. Land
  2.0a–2.0e together so fallback removal has no implicit scope. Resolve metadata array class names
  through a fixed allowlist mapping of names to supported writer-produced types, including `[B` to
  byte[]. Do not load arbitrary classes named by untrusted file metadata. If a resolver still
  needs `Class.forName(name, false, loader)`, call it only for allowlisted names. Unknown names
  fall back to per-field schema inference, without failing the whole read; malformed metadata must
  not shift type/column alignment. Test unknown/custom class names and arrays. The legacy `[B`
  corruption rule below takes precedence over both allowlist resolution and schema fallback. Add
  file/bytes/stream/SPI round-trip tests for null, empty and arbitrary non-UTF-8 bytes, plus
  PyArrow verification of exact payloads. Characterize original byte[] write failure and the `[B`
  class-resolution failure. Use explicitly synthetic legacy text metadata if the original writer
  cannot produce a valid file; fixing class lookup cannot recover a textual payload. Fail explicitly on legacy Matrix byte[] metadata rather than returning the
  textual payload as if it were original binary. State this in migration notes and test it;
  marker-aware new byte[] files must read normally.

- 2.0b [x] Implement per-type encodings and explicit rejections. Replace remaining implicit
  `toString()` cases with the policies in 0.2. Implement Byte/Short as signed INT32 +
  `intType(8/16, true)`, with range validation and annotated nested-width inference. Encode LocalTime as
  TIME(NANOS) to preserve nanoseconds, and Instant as adjusted TIMESTAMP(MICROS). Reject Instant
  values with sub-microsecond precision rather than silently truncating, and validate int64
  epoch-micros overflow using checked arithmetic, including negative/pre-epoch instants. Test
  historical dates beyond the int64-nanos range, exact microseconds, fractional-microsecond
  rejection and extreme Instant bounds. MICROS has a wider range than NANOS but still cannot
  represent Instant.MIN/MAX; do not promise those. No NANOS write option is required in this
  release. New-format metadata restores these exact Java types; document external schema-inference
  types separately and preserve existing java.sql.Time/LocalDateTime contracts. Encode UUID as 16
  bytes with `uuidType()` in the specification's byte order. Character becomes STRING; enums become
  STRING using `name()`, with canonical String metadata at every supported nesting level. Reject
  ZonedDateTime, OffsetDateTime and remaining custom classes before writing rows; messages describe
  explicit Instant/String conversion or column retyping.

- 2.0c [x] Unify column type resolution and validation. Resolve every column through one validated
  schema/type descriptor used for physical schema, value encoding, decimal settings and emitted
  metadata. For Object and abstract/interface supertypes (Number, CharSequence, Comparable,
  Serializable, Temporal), scan all non-null values, require assignability to the boxed declaration
  and one supported policy type, then emit that encoding's canonical name. Never decide from only
  the first sample. For concrete declarations, including non-final concrete classes, require each
  non-null value to be an instance after primitive boxing normalization; additionally validate the
  single encoding policy across subclasses. A non-final modifier alone does not turn a concrete
  declaration into an inference-only supertype. Report the first offending zero-based row, declared
  type, runtime class and expected policy before output mutation. Resolve supported concrete
  all-null columns from their declaration; Object or abstract/interface all-null columns use
  canonical String only if compatible with the declaration, otherwise require an explicit supported
  type. Thus all-null Number/Temporal columns need explicit retyping rather than false String
  metadata. For supported concrete declarations, the single encoding policy comes from the runtime
  values, while metadata retains the declared supported type unless an explicit canonicalizing
  policy (Character/Enum) applies. Empty columns use the declared policy. Mixed Date/Timestamp
  values fail even though Timestamp is assignable to Date, because MILLIS/MICROS policies differ;
  never dispatch incompatible units per value. Add a legacy characterization test for Date first,
  Timestamp later, recording the current 1000× timestamp corruption, and sql.Date first, Timestamp
  later, recording the current write failure. New tests require rejection before output mutation in
  either order, plus homogeneous temporal-subclass round trips with declared Date metadata. Test
  Object/supertype inference, concrete mismatch, boxed normalization and all-null rules separately.
  Test Number/CharSequence/Comparable/Serializable/Temporal declarations, and reject a concrete
  BigDecimal column containing Integer values before writing.

- 2.0d [x] Route every resolved BigDecimal column, declared or resolved from Object/Number/other
  supertypes, through shared `inferDecimalPrecisionAndScale` and column-name `decimalMeta` handling
  before schema construction. Use explicit per-column settings first, explicit global
  precision/scale next, then inference if enabled. With `inferPrecisionAndScale(false)`, require
  complete explicit settings for each resolved BigDecimal field; reject missing settings rather than
  falling back to DOUBLE. Apply existing rounding/precision validation to all these paths. Always
  emit FIXED_LEN_BYTE_ARRAY + DECIMAL and canonical BigDecimal metadata together; remove the DOUBLE
  fallback for declared BigDecimal too. Handle nested decimals with the same resolved-field policy;
  document nested metadata addressing and reject unresolved settings. Use a documented minimum
  precision/scale (1, 0) for all-null declared BigDecimal when inferring. Test untyped Groovy
  literals `[1.5, 2.25]`, high-precision values that DOUBLE cannot represent, negative scales,
  Number-declared decimals, per-column overrides and inference disabled with and without explicit
  settings. Assert exact BigDecimal values, runtime classes, declared Matrix types, physical DECIMAL
  metadata and all-null behavior across entry points. General declared-type validation tests belong
  to 2.0c. Canonical metadata must never claim BigDecimal over a DOUBLE or INT32 encoding.

- 2.0e [x] Verify metadata coverage, migration contracts and cross-entry-point behavior. Write the
  canonical type name instead of Object or the abstract/interface declaration. Canonicalized
  Character/Enum values follow the String policy; do not silently widen heterogeneous numeric types.
  Reject mixed or unsupported policy types with the first offending row index (document zero-based),
  actual class and expected policy. All-null Object columns retain STRING/BINARY with canonical
  String metadata. Validate nested inferred schemas consistently before writing. Do not discover
  unsupported types halfway through overwriting an output file. Include null/all-null columns and
  boxed primitive normalization in schema validation. Add ordinary/optimized and file/bytes/SPI
  round-trip tests for each supported policy, nested variants where supported, and contextual
  rejection tests for each rejected class. Characterize legacy text separately: it remains String,
  not reconstructed Java objects. Assert writer-emitted matrix.columnTypes names are a subset of the
  resolver allowlist across all supported types and canonicalized columns, including Byte/Short and
  containers. Never emit java.lang.Object or arbitrary enum/custom class names in new files; old
  unknown names continue to use the schema fallback. Test the untyped builder example with
  Integer/String data, homogeneous nullable Object columns, all-null Object columns and mixed
  `[1, 'x']` rejection before output mutation. Assert inferred canonical metadata names and resulting
  Matrix types, including Object columns of each supported policy type. Extend migration notes with
  each 0.2 row. For each of 2.0a–2.0e, run the ordered module checks in 6.3 and record the
  successful commands/results before checking off that task; do not use a single completion checkbox
  for all five areas.

- 2.1 [x] Add byte-preserving inference/conversion for raw binary fields, including binary inside
  nested media structs. Retain string decoding for STRING annotations and DECIMAL handling and the
  2.0 legacy/override rules. Document the compatibility impact of changing external raw-binary
  columns from String to byte[]. Binary writer/reader round-trip support in 2.0a is mandatory, not
  conditional.

- 2.2 [x] Apply DECIMAL scale to INT32/INT64 physical values, including nested values; test
  negative, null, scale-zero and fractional values against PyArrow expectations.

- 2.3 [x] Respect TIMESTAMP `isAdjustedToUTC`: adjusted timestamps use the configured zone;
  unadjusted timestamps retain local wall-clock fields via UTC epoch arithmetic
  (`LocalDateTime.ofEpochSecond(instant.epochSecond, instant.nano, ZoneOffset.UTC)`). Cover
  MILLIS/MICROS/NANOS, pre-epoch values, nested timestamps, DST boundaries and
  UTC/Europe-Stockholm/America-New_York. Generate naive and timezone-aware PyArrow fields and
  assert their footer annotations; preserve adjusted Matrix round trips. Specify Date/Timestamp
  metadata overrides explicitly rather than silently assigning an instant to local timestamps.

- 2.4 [x] Add UUID, FLOAT16 and INT96 fixtures. Decode UUID to `UUID`; define FLOAT16 widening to
  `Float` with special-value tests, or fail explicitly if unsupported in the first delivery.
  Characterize INT96 failure and implement documented legacy timestamp semantics or a contextual
  unsupported-type error. Never silently decode these as text. Cover compliant, two-level and
  legacy LIST layouts with explicit support/error contracts.

- 2.5 [x] Keep these tests paired across ordinary and optimized layouts so failures are attributed
  to schema conversion rather than CDC. Report unsupported logical types explicitly; broaden
  beyond these cases only when a fixture demonstrates need.

### 3. Expose selective local reads using upstream page filtering (P1)

- 3.1 [x] Build a shared `InputFile` core using `LocalInputFile` and `PlainParquetConfiguration`;
  use `LocalOutputFile`/`OutputFile` and plain configuration on file writes too. Preserve naming,
  time-zone and ownership behavior across legacy, builder and SPI paths. Add an in-memory seekable
  `InputFile` for byte arrays, replacing their temp-file copy. Test seeks, EOF and independent
  streams without full-buffer copies. Use `ParquetFileReader.open(InputFile, ParquetReadOptions)`
  and an explicit per-row-group loop, with `getRowGroups()`,
  `readFilteredRowGroup(int)`/`readRowGroup(int)` and `MessageColumnIO.getRecordReader(pages,
  materializer, filter)`. Set requested schema before filtering. Keep original footer group IDs
  stable even if pruning removes groups from an internal list; spike and test this mapping before
  exposing selectors. Close each `PageReadStore` on advancement/failure. Do not build the new core
  around `ParquetReader`. Its 1.18.1 API does expose current-row-group index, but does not provide
  direct indexed group selection; explicit groups fit batching and independent workers better.
  Follow the upstream [internal reader loop](https://github.com/apache/parquet-java/blob/apache-parquet-1.18.1/parquet-hadoop/src/main/java/org/apache/parquet/hadoop/InternalParquetRecordReader.java),
  without depending on that package-private class. Keep accept/rollback bookkeeping in the Matrix
  materializer using the hooks in 3.2; the loop respects skipped/null records without duplicating
  column-list truncation. Spike this integration before removing Hadoop paths. Test
  SNAPPY/ZSTD/GZIP/uncompressed read/write with hadoop-common/mapreduce excluded, and inspect
  linkage/dependency reports. Keep required codec classes until this proves removal safe;
  distinguish avoiding filesystem initialization from removing dependencies. Do not raise the JDK
  target as part of this change.

- 3.2 [x] Replace `GroupReadSupport` with a custom `ReadSupport`/`RecordMaterializer` whose
  converters append to typed column lists. Precompute schema/converter mappings and avoid per-cell
  name lookup, per-row Group/list/Object[] creation and row-to-column transposes. Spike Matrix
  construction from columns to verify ownership/copy behavior. Preserve nested list/map/struct
  nulls, empty collections and parent-row alignment. Any active row filter, including ordinary
  pushed-down record filtering, requires transactional output: converters may append values before
  acceptance is known. Snapshot each output/filter-column list size at row start. Accept in Matrix
  `RecordMaterializer.getCurrentRecord()`, which the filtering wrapper calls only after its
  pushed-down predicate passes: evaluate any residual predicate there before accepting and return
  null on rejection. Return a non-null lightweight record token for acceptance, including all-null
  rows. Roll back to the snapshots in `skipCurrentRecord()`, reached through parquet-java's
  skipped/null-record handling, and reset nested assembly state. Keep rollback idempotent. Mid-row
  exceptions must invoke the same materializer-owned abort/rollback operation before propagating;
  snapshots also protect this path. Test the actual hook ordering with pushed-down and residual
  rejection. Test rejected first/middle/last rows and entire rejected groups, ensuring every
  column has the same accepted-row count. Use staging where nested assembly requires it, without
  allocating a generic row buffer for every unfiltered flat row. Differential-test against the old
  reader before adding new projection/filter options; preserve schema fixes from phase 2 in shared
  conversions. A ReadSupport adapter may initialize the converter tree, but the explicit
  ParquetFileReader loop owns group selection and requested schema.

- 3.3 [x] Implement dictionary-aware string reuse in converters: decode each dictionary ID once
  per active dictionary and reuse the String reference, supporting dictionary changes and
  plain-page fallback. No global `String.intern()` or unbounded cross-file cache. Verify identity
  reuse on categorical data and exact values after dictionary fallback.

- 3.4 [x] Add the proposed options below, including `toMap`, `fromMap`, descriptors, validation,
  builder methods and public GroovyDoc. Use Matrix-owned immutable predicate types, translated
  internally; no upstream predicate classes or closures in the new public contract. Reject unknown
  columns, duplicates and incompatible predicate types before reading. Start projection with
  top-level column names.

  | Proposed option | Type | Default | Meaning |
  |---|---|---|---|
  | `columns` | `List<String>` | `null` | All columns; supplied nonempty list selects output columns in requested order |
  | `filter` | Matrix-owned `ParquetPredicate` | `null` | Logical-schema predicate with exact record filtering |
  | `useColumnIndexFilter` | `boolean` | `true` | Allow upstream page pruning when available; false is a comparison/diagnostic path |

- 3.5 [x] Pass translated upstream predicates via `FilterCompat.get(predicate)`. The core loop
  sets the union of output columns and all pushdown/residual filter columns via
  `ParquetFileReader.setRequestedSchema(MessageType)` before filtering/group reads. ReadSupport
  only initializes the matching converter/materializer tree; it does not own a second projection
  implementation. Keep exact record filtering enabled. Read filter-only columns internally and
  exclude them from the resulting Matrix. Add `Predicates.eq/ne/lt/le/gt/ge`, `isNull/isNotNull`,
  `and/or/not` with typed value overloads for Number, String, Boolean, LocalDate, LocalDateTime,
  Instant, LocalTime and byte[]. Convert dates, timestamp/time units, UTC adjustment, decimals
  (including integer and fixed binary physical forms) using the actual file schema. Reject
  overflow, lossy rescaling, incompatible local/instant comparisons and unsupported repeated-field
  predicates. Implement exact logical residual evaluation when safe pushdown is unavailable; do
  not incorrectly compare signed decimal bytes as generic lexicographic binary. Partition
  pushdown/residual expressions safely, especially under OR/NOT. Document null/NaN semantics and
  test them against full evaluation.

- 3.6 [x] Remap `matrix.columnTypes` by original field name for projected/reordered columns.
  Restore Matrix index metadata only when all indexed columns survive; otherwise omit that Matrix
  index. Empty matches must still retain the selected schema and types.

- 3.7 [x] Test result equivalence with index filtering on/off and indexes absent. Cover AND/OR,
  null predicates, unordered data, truncated string bounds, boundary values and filter-only
  columns. Treat unavailable indexes as a normal scan fallback; propagate genuine corruption
  rather than silently returning partial results.

- 3.8 [x] Prove page skipping using a counting seekable input and controlled multi-page fixture.
  Disable row-group/dictionary/bloom pruning in this focused test so an entire row-group skip
  cannot masquerade as page-index use. Require identical results and fewer data-page bytes read;
  avoid timing assertions and exact read-call counts.

- 3.9 [x] Add `limit(long)` (default unlimited/null, nonnegative; zero returns typed empty
  output), `rowGroups(List<Integer>)` (default all, zero-based, unique IDs in caller-specified
  order), and `batches(...)` returning a Matrix-owned closeable iterator of Matrix values,
  normally one batch per selected row group. Limit counts matching rows after filtering and
  applies across batches. Footer IDs in range remain valid when statistics, dictionary or bloom
  pruning excludes them: contribute no rows and skip the empty batch. Only out-of-range or
  duplicate IDs are rejected. Preserve caller-selected order and test valid pruned IDs. Close
  resources on exhaustion, explicit close and failure; document try-with-resources/withCloseable.
  Iterator state stays on one input and does not prefetch unbounded groups. One row group can
  exceed heap, so also support `maxBatchRows` (positive, default null) to split it; document that
  byte-array cells/one huge row still constrain memory. Skip empty filtered batches and retain
  schema for empty materialized results. Test early stop and cleanup.

- 3.10 [x] Measure 3.2/3.3 against 1.6 before removing the old path. Initial acceptance target: at
  least 20% lower median full-scan elapsed time on wide text and 25% lower peak live heap on
  categorical data, with no greater than 5% median regression on mixed/nested data. Record
  repeated-run variance; revise targets explicitly if evidence shows they are unrealistic. Timing
  thresholds belong in benchmark reports, not flaky CI assertions.

### 4. Add selective HTTP access (P2, depends on 3)

- 4.1 [x] Implement a seekable HTTP `InputFile` / `SeekableInputStream` with bounded range
  caching. Discover stable length and verify actual `206`/`Content-Range` behavior; fetch
  footer/index/data ranges on demand. Reuse the same remote-object identity for metadata and rows,
  detecting changes via validators where available.

- 4.2 [x] Preserve ordinary URL reads through fallback to the existing temporary-file path when a
  server lacks range support. Proposed `remoteReadMode` enum/string default `DOWNLOAD` preserves
  current behavior; `AUTO` attempts ranges then downloads safely, and `RANGE` requires verified
  range support. Add positive `connectTimeoutMillis` and `readTimeoutMillis`, proposed defaults
  10000/60000, plus positive `rangeCacheBytes`, proposed default 8388608. Propagate these through
  builder/options/SPI.

- 4.3 [x] Test redirects typical of Hub resolve URLs, servers ignoring Range, bad range responses,
  EOF, retries with bounded attempts, resource cleanup and object changes. Do not forward
  authorization across unrelated redirect origins. Keep credentials out of logs. No new Hub
  authentication or Xet-client integration is required for public URLs.

- 4.4 [x] Use a local range-capable HTTP server and selective projection/predicate fixture to
  prove fewer transferred bytes than a full download. Count footer/index overhead too. Document
  that this is HTTP range access, not Xet deduplication or a streaming Matrix.

### 5. Confirm writing behavior; defer native CDC to a separate decision (P1/P3)

- 5.1 [x] Based on 1.4, document existing page-index output. If an eligible-column omission is
  reproducible, fix shared writer configuration across file/memory/stream paths; extract common
  configuration rather than copying more builder chains. Add page-size tuning through the
  explicitly planned options in 5.4; benchmarks inform guidance, not whether the options are
  implemented.

- 5.2 [x] Keep native CDC writing deferred unless a supported parquet-java release exposes it or a
  separately scoped upstream implementation is accepted. No private upstream patch, mandatory
  Python bridge, or misleading “optimized” switch in this enhancement. Page-size/row-group tuning
  alone is not CDC.

- 5.3 [x] For users needing CDC output now, document the external PyArrow rewrite below, verify
  values and custom metadata survive, and state the extra I/O and Python requirement. A future
  native CDC proposal must cover per-column logical hashing, nested levels, dictionary effects,
  deterministic output and measured reuse after inserts/deletes.

- 5.4 [x] Expose the safe tuning subset below through typed options, builder, SPI and shared
  writer configuration. Use a Matrix-owned writer-version enum/string. Validate positive
  sizes/NDVs, primitive column paths and names against the resolved schema; reject unknown map
  keys/columns and invalid versions. Keep current defaults explicitly.

  | Option | Type | Proposed default | Meaning |
  |---|---|---|---|
  | `rowGroupSize` | long | 134217728 | Target row-group bytes |
  | `pageSize` | int | 1048576 | Target encoded data-page bytes |
  | `dictionaryEncoding` | boolean | true | Enable dictionary encoding with upstream fallback |
  | `writerVersion` | Matrix enum/string | V1 | Select V1/V2 writer encodings and page layout |
  | `columnIndexTruncateLength` | int | 64 | Binary bounds truncation for page indexes |
  | `statisticsTruncateLength` | int | 2147483647 | Binary bounds truncation for row-group statistics |
  | `bloomFilterEnabled` | Map<String,Boolean> | empty | Per-column overrides, globally disabled |
  | `bloomFilterNDV` | Map<String,Long> | empty | Per-column expected distinct values; omission retains upstream sizing |
  | `sizeStatisticsEnabled` | boolean | true | Collect upstream size statistics |

  V2 selects a different encoder factory as well as page layout; do not describe this as just a
  page-header toggle. Keep V1 default and document that older consumers may not support all V2
  encodings; name specific consumers only after compatibility testing. Describe target-size versus
  hard-limit semantics, NDV memory tradeoffs and disabled-column interactions. Test physical
  metadata/page versions and logical equivalence across every output path. Add indexed equality
  tests proving bloom-filter pruning independently from page/dictionary/row-group-statistics
  pruning. Match any changed defaults to versioned upstream constants before implementation.

- 5.5 [x] Track native CDC in P3: investigate [parquet-java
  #3818](https://github.com/apache/parquet-java/pull/3818), referenced by
  [Arrow #51684](https://github.com/apache/arrow/issues/51684) as a Java CDC implementation. The
  Java PR remains open and unmerged as verified on 2026-10-17; 1.18.1 exposes no supported
  CDC builder API. Recheck before choosing a future dependency. Link the applicable issue/PR and supported release here,
  or draft a concrete upstream issue covering hooks/page boundaries, nested hashing and
  interoperability. Ask for authorization before posting externally; a reviewable issue draft is
  sufficient until then. Do not silently leave deferral untracked.

### 6. Documentation, measurements and completion gates

- 6.1 [x] Update module README, `docs/cookbook/matrix-parquet.md`,
  `docs/tutorial/11-matrix-parquet.md` and release notes. Distinguish tested full-file
  compatibility, selective local reading, selective HTTP reading and CDC writing. Include complete
  examples, every new parameter/default, supported types and fallback behavior. Do not advertise
  arbitrary ordinal random access from a filter API; `withFileRange` is a byte range, not a row
  interval. Ordinal seeking is a separate feature.

- 6.2 [x] Record a reproducible benchmark for full scan, narrow projection and selective
  predicates on ordinary/indexed/CDC+indexed files: rows, file/page/row-group sizes, compression,
  returned rows, bytes read/transferred, wall time and peak memory. Keep timing benchmarks outside
  normal correctness tests; compare full-scan results to 1.6 and the measured targets in 3.10.

- 6.3 [x] For each implementation phase run, in order: `./gradlew :matrix-parquet:codenarcMain`,
  `./gradlew :matrix-parquet:spotlessCheck`, `./gradlew :matrix-parquet:test`. Run `./gradlew
  test` before completing implementation. Record successful commands and outcomes here or in the
  PR before checking tasks off. Include `codenarcTest` for changed tests.

### 7. Sharded datasets and bounded parallel decoding (P2)

- 7.1 [x] Add `read(List<File>)`, directory input and explicit `readGlob(String)` using caller
  order for lists and sorted normalized-path order for directories/globs; reject duplicate
  normalized paths. No implicit glob interpretation in ordinary file strings. Directory reads
  select direct `.parquet` children by default, not recursive trees. Reject empty selections with
  a clear error. Check all schemas before collecting results: strict logical types, field
  names/order, decimal precision/scale and timestamp units/UTC flags by default. Do not silently
  coerce incompatible shards. Test Matrix metadata policy separately from physical schema
  compatibility.

- 7.2 [x] Add `sourceFileColumn` (String, default null; reject collisions) containing each shard's
  normalized absolute local path. Apply projection/predicates independently per file schema and a
  single matching-row limit across the dataset. Define dataset row-group selectors as explicit
  per-file selections rather than ambiguous global group numbers. Extend closeable batches to
  shards while preserving deterministic file/group order.

- 7.3 [x] Add `parallelism` (positive int, default 1) and bounded prefetch (default one work item
  per worker). Decode independent files/groups on separate readers; never share a seekable stream,
  converter or mutable dictionary. Reuse each file's already-parsed footer through
  `ParquetFileReader.open(InputFile, ParquetMetadata, ParquetReadOptions, SeekableInputStream)`
  with a separate stream per worker. Verify the source identity/length matches the footer snapshot
  and close streams on constructor/open failures. Test one footer parse per file rather than per
  worker; do not share mutable pruning state between workers. Preserve order, cap queued results,
  cancel remaining work on limit/close/failure and propagate errors with source context. Measure
  throughput/heap against sequential decoding and test resource cleanup and deterministic results;
  avoid decoding whole datasets before returning the first batch.

### 8. Optional Hub URL resolution (P3)

- 8.1 [x] Specify and test `hf://datasets/<owner>/<repo>@<rev>/<path>` resolution to an HTTPS
  `/datasets/<owner>/<repo>/resolve/<rev>/<path>` URL; revision defaults to `main`. Match
  huggingface_hub parsing/quoting, including percent-encoded revision slashes
  (`refs%2Fconvert%2Fparquet`) and supported special unescaped refs. Pin compatibility to
  huggingface_hub **v2.2.0** `parse_hf_uri` and URL quoting, after comparing v1.0.0 `resolve_path`,
  `unquote`, special-ref regex and `safe_revision`. The new convert-ref regex allows dots/dashes;
  deep-path @ is literal. Record differences and regenerate offline differential reference cases
  with the adopted tagged client. Preserve unescaped refs/convert/parquet and refs/pr/N; percent-encode
  other slash-containing revisions according to the selected client. Add differential URL tests
  for @, nested paths, encoded and special refs, Unicode and avoiding double decoding/encoding.
  [Client implementation](https://github.com/huggingface/huggingface_hub/blob/v2.2.0/src/huggingface_hub/hf_file_system.py).
  Reuse phase 4 transport without adding Xet claims. Optional HF_TOKEN is sent only to approved
  `https://huggingface.co` origins, never signed CDN redirect origins or logs; test redirect
  behavior with mocked origins. Verify current Hub URL/auth documentation before implementation.
  Existing public HTTPS reads need no token.

Execution order: **P0: 0–2; P1: 3 and 5.1/5.4; P2: 7 and 4; P3: 8 and 5.5**.
Documentation and completion checks in 6 apply to every delivery. Phase 5.2/5.3 explains
CDC deferral and the external rewrite. Native CDC writing is not a prerequisite for reading
optimized downloads correctly. The execution record below describes the delivered implementation.

## Usage examples and fixture reproduction

API for a downloaded optimized file (compatibility verified by phase 1):

```groovy
import se.alipsa.matrix.parquet.MatrixParquetReader

def data = MatrixParquetReader.read(new File('downloaded-optimized.parquet'))
```

Phase 3 API; values convert through the file schema:

```groovy
import se.alipsa.matrix.parquet.Predicates
import se.alipsa.matrix.parquet.MatrixParquetReader

def subset = MatrixParquetReader.builder()
    .columns(['id', 'text'])
    .filter(Predicates.ge('id', 1000L))
    .limit(100)
    .useColumnIndexFilter(true)
    .read(new File('downloaded-optimized.parquet'))
```

Additional P1/P2 usage:

```groovy
import java.time.LocalDate
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.Predicates
import se.alipsa.matrix.parquet.MatrixParquetWriter

// Logical DATE comparisons require no epoch-day conversion in application code.
def preview = MatrixParquetReader.builder()
    .filter(Predicates.ge('date', LocalDate.of(2026, 1, 1)))
    .limit(20)
    .read(new File('events.parquet'))

// Close explicitly even if consumption stops early; limit counts matches globally.
MatrixParquetReader.builder().limit(1000).maxBatchRows(250)
    .batches(new File('events.parquet')).withCloseable { batches ->
      while (batches.hasNext()) {
        def batch = batches.next()
        // Process this Matrix before requesting the next bounded batch.
      }
    }

MatrixParquetWriter.builder(preview)
    .rowGroupSize(16L * 1024 * 1024)
    .pageSize(64 * 1024)
    .dictionaryEncoding(true)
    .writerVersion('V2')
    .write(new File('preview.parquet'))

// Shard order follows this supplied list; directory/glob APIs sort paths.
def shards = MatrixParquetReader.builder().sourceFileColumn('_source').parallelism(2)
    .read([new File('train-00000.parquet'), new File('train-00001.parquet')])
```

For explicit lists, preserve caller order (and reject duplicate normalized paths); for
folder/glob selections use sorted normalized paths. Carry this distinction into 7.1.

Minimal end-to-end external rewrite for CDC output. Use a dedicated environment and pin
the generator version in the fixture manifest; 21.0.0 is a suitable minimum baseline:

```bash
python3 -m venv /tmp/matrix-parquet-fixtures
/tmp/matrix-parquet-fixtures/bin/python -m pip install pyarrow==21.0.0
/tmp/matrix-parquet-fixtures/bin/python - <<'PY'
import pyarrow.parquet as pq

# input.parquet is an existing matrix-parquet output file.
table = pq.read_table('input.parquet')
pq.write_table(
    table, 'optimized.parquet', compression='snappy',
    use_content_defined_chunking=True, write_page_index=True,
)
assert pq.read_table('optimized.parquet').equals(table, check_metadata=True)
PY
```

Here `table` is the full Arrow table, the second argument is the output path,
`compression` explicitly chooses SNAPPY (also PyArrow's default), CDC defaults to false
unless enabled, and page-index writing defaults to false unless enabled. Other settings
use the pinned PyArrow version's defaults. This example loads the table into memory;
the eventual fixture generator must explicitly set row-group/page/encoding options and
emit all four CDC/index combinations plus its manifest. It must not require Hub uploads.


## Implementation and verification record — 2026-10-17

All numbered deliverables above are implemented and verified in this branch. Native CDC writing
is intentionally deferred as specified by 5.2; its upstream tracking and external rewrite are
complete. The module remains on parquet-java 1.18.1, Hadoop 3.5.0 and JDK 21.

### Final ordered gates

These successful commands verify the delivered tree, including each separate 2.0a–2.0e task:

```bash
./gradlew :matrix-parquet:codenarcMain :matrix-parquet:codenarcTest --console=plain
./gradlew :matrix-parquet:spotlessCheck --console=plain
PARQUET_PYTHON=/tmp/matrix-parquet-fixtures/bin/python \
  ./gradlew :matrix-parquet:test :matrix-parquet:compileConsumerApi -PrunSlowTests=true --console=plain
./gradlew test -Pheadless=true --console=plain
```

CodeNarc and Spotless passed. The Parquet suite reported **122 tests, 117 passed, zero failures,
five skipped**. The skips are opt-in benchmarks and original-writer fixture capture; default-size
CDC was generated and tested. The Java consumer compiles without parquet-java/Hadoop on its
compile classpath. The full repository regression task passed; unaffected tests may be up-to-date.

| Task | Implementation and independently asserted coverage |
|---|---|
| 0.1–1.5 | 0.8.0 migration table; original 0.7.x strings/temporal/decimal fixtures; eight Arrow layout cases and manifest; physical indexes/pages; all primary entry points |
| 2.0 | Recursive STRING annotations, version marker, automatic legacy nested text compatibility and binaryAsString options |
| 2.0a | Raw binary writes and exact reads through bytes/files/streams/SPI; fixed metadata allowlist; unknown/incompatible metadata fallback; explicit legacy `[B` rejection |
| 2.0b | Byte/Short/LocalTime/Instant/UUID/Character/Enum policies; precision/overflow checks; contextual unsupported-type rejection |
| 2.0c | Shared ColumnTypePolicy descriptor; complete scans, abstract/interface inference, concrete assignability, all-null rules and incompatible Date subclass rejection in either order |
| 2.0d | Exact inferred/declared/nested BigDecimal DECIMAL routing; per-field/global precedence, disabled-inference errors, high precision, negative scale and all-null cases |
| 2.0e | Allowlist coverage for all emitted canonical types; every supported policy through bytes, streams and SPI files; validation before output mutation and migration notes |
| 2.1–2.5 | External raw media bytes, integer decimals, naive/adjusted timestamps at every unit, UUID and explicit FLOAT16/INT96 errors, legacy LIST variants |
| 3.1–3.9 | InputFile/ParquetFileReader core, column converters, accept/skip rollback, dictionary reuse, logical predicates, projection/index remapping, physical pruning, limits and closeable bounded batches |
| 4.1–4.4 | Range input, fallback, cache/timeouts, bounded redirects/retries, origin-scoped tokens, malformed/truncated ranges, object changes and fewer transferred bytes |
| 5.1–5.5 | Physical output indexes across file/memory/stream paths, shared tuning, bloom-only pruning, metadata-preserving Arrow CDC rewrite and upstream CDC tracking |
| 6.1, 6.3 | Complete README/defaults/examples, tutorial/cookbook/release updates; ordered gates and full repository suite |
| 1.6, 3.10, 6.2, 7.3 | Original wide baseline, million-row Group/columnar comparisons, sampled live heap, three Arrow layouts with full/projection/filter modes and sequential/parallel dataset measurements |
| 7.1–7.3 | Ordered list/directory/glob shards, strict schemas/metadata, source column, per-file groups, reused footers, bounded independent workers, limit and early-close cleanup |
| 8.1 | Tagged huggingface_hub 2.2.0 offline differential cases after v1.0.0 comparison; revision/path quoting and redirect credential tests |

### Reproduction and additional successful checks

```bash
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/generate_optimized.py
/tmp/matrix-hf-fixtures/bin/python matrix-parquet/src/test/python/generate_hub_urls.py
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/verify_matrix_output.py
./gradlew :matrix-parquet:test --tests ParquetInteropTest --console=plain
./gradlew :matrix-parquet:codecWithoutHadoop --console=plain
groovy scripts/release-all/test-workflow.groovy .
```

Fixture regeneration is byte-for-byte deterministic on the pinned environments. Checked-in
Parquet binaries total **737,019 bytes**, within the 4 MiB budget and 1 MiB individual cap.
Python environments use pyarrow==21.0.0 and huggingface_hub==2.2.0 respectively. Python verification
confirmed SNAPPY/ZSTD annotated strings, binary payloads and exact decimals; CDC rewrites retain
values and Matrix metadata and are reread by Java. CI runs these checks in
.github/workflows/release-all-tools.yml. Fixture provenance and client-version differences are
recorded in src/test/resources/optimized/README.md.

Original-writer capture was run successfully with:

```bash
./gradlew :matrix-parquet:test --tests ParquetLegacyCaptureTest \
  -I /tmp/parquet-legacy.init.gradle --no-configuration-cache --console=plain
```

That init script sets test system property parquet.captureLegacy=true. The capture compiles the
writer/value helper at b2095159664b34ce426685c387adf0bccdb33e01 together in an isolated loader.
The byte[] hypothesis was corrected by this end-to-end experiment: this pinned writer throws
InvalidSchemaException and cannot produce a valid byte[] fixture. The checked-in `[B` text footer
is explicitly synthetic; no original bytes are claimed recoverable. Date/Timestamp corruption is
preserved in an actual original-writer fixture and new mixed policies are rejected before output.

The codec-exclusion spike showed that even UNCOMPRESSED writer initialization loads Hadoop's
ZlibCompressor.CompressionLevel. Local filesystem/configuration setup is removed from the new
read/write core, but runtime Hadoop dependencies remain until upstream codec linkage permits
removal. No JDK target increase or dependency upgrade is claimed.

### Measurements

See [the benchmark report](../benchmarks/optimized-parquet.md) for hardware/JVM/heap/GC, input
generation, warmup/variance, allocations, sampled live-heap definitions, exact byte counts and
reproduction commands. The original wide baseline was captured before production edits; the
three-workload differential comparison retains the original Group loop with shared corrected
schema conversions. Inputs are generated outside Git, and no claim of an OS-cold cache is made.

The million-row comparison measured 89.5% lower wide median time, 43.8% lower categorical sampled
live heap and 88.2% lower mixed/nested time, meeting the initial targets with the documented
sampling method. Identical Arrow layouts exercise full scans, ID projection and 100-row selection;
page indexes reduced selective reads from 1,279,020 to 552,869 bytes (542,383 with CDC). An isolated
page-index test reads 2,914 versus 82,700 bytes with all other pruning disabled. Independent bloom
pruning reads 16,737 versus 40,373 bytes. HTTP ranges transfer 88,476 bytes of an approximately 906,000-byte object,
including footer overhead. Four-worker shard decoding measured a 242 ms warm median versus 341 ms
sequentially. These are local observations, not timing assertions or general speed guarantees.

Benchmark commands (init-file setup is fully documented in the report):

```bash
./gradlew :matrix-parquet:test --tests '*ParquetReadBenchmarkTest' \
  -I /tmp/parquet-baseline.init.gradle --no-configuration-cache --console=plain
./gradlew :matrix-parquet:test --tests '*ParquetDatasetBenchmarkTest' \
  -I /tmp/parquet-dataset.init.gradle --no-configuration-cache --console=plain
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/generate_benchmarks.py
./gradlew :matrix-parquet:test --tests '*ParquetLayoutBenchmarkTest' \
  -I /tmp/parquet-layout.init.gradle --no-configuration-cache --console=plain
```
