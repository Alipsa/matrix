# Matrix Parquet

Read and write Apache Parquet from Groovy/Java Matrix applications on JDK 21. This development
version targets **0.8.0** and uses parquet-java 1.18.1. Include `matrix-core` and Groovy alongside
`matrix-parquet` in your application. To test this checkout, run `./gradlew :matrix-parquet:test`.

## Optimized Parquet and selective reads

Hugging Face ordinary and optimized downloads are standard Parquet. Content-defined chunking
(CDC) changes writer page boundaries for deduplication; it requires no special reader. Page
indexes are independent and let compatible predicates skip pages. The checked-in PyArrow 21
fixtures exercise all four CDC/index combinations with SNAPPY/ZSTD and V1/V2 pages.

```groovy
import java.time.LocalDate
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter
import se.alipsa.matrix.parquet.Predicates

File file = new File('events.parquet')
Matrix events = Matrix.builder('events').columns(
    id: [1L, 2L, 3L], date: [LocalDate.of(2026, 1, 1)] * 3,
    text: ['first', 'second', 'third']).build()
MatrixParquetWriter.builder(events).pageSize(65536).write(file)

Matrix selected = MatrixParquetReader.builder().columns(['text', 'id'])
    .filter(Predicates.ge('id', 2L)).limit(10).read(file)
assert selected.column('id') == [2L, 3L]
```

Projection accepts top-level fields in output order. Filter-only columns are read internally.
`Predicates.eq/ne/lt/le/gt/ge` have Number, String, Boolean, LocalDate, LocalDateTime, Instant,
LocalTime and byte[] overloads. Use `isNull`, `isNotNull`, `and`, `or` and `not` for composition.
Values must match the file's logical schema. Unrepresentable numeric bounds, fractional integer values, NaN/Infinity, lossy decimal
rescaling, and finer timestamp/time bounds use exact residual evaluation instead of pushdown.
For example, `Predicates.gt('i', 1.5G)` on an integer column correctly selects integers 2 and above.
An `and` can still push its safe child; `or` requires both children to be safe supersets, and `not`
is always residual.
Repeated/nested predicates are rejected. Missing indexes fall back to scans; corrupt data fails.

String ordering uses unsigned UTF-8 bytes, matching Parquet. Unannotated BINARY predicates take
byte[] even when legacy metadata or `binaryAsString` makes output text. Comparisons with null are false; null tests are explicit. `not` is Boolean negation, including
for null rows. NaN comparisons are false except `ne`; numeric infinities retain their ordering.
Safe physical comparisons use row-group, dictionary, bloom and page-index pruning. Binary
DECIMAL comparisons and expressions without safe pushdown use exact residual evaluation.
LocalDateTime predicates on adjusted timestamps use `zoneId`; Instant predicates on naive
local timestamps are rejected. External TIME fields retain the existing `java.sql.Time`
millisecond output contract; filtering evaluates the original time precision.

## Batches and shards

```groovy
import se.alipsa.matrix.parquet.MatrixParquetReader

MatrixParquetReader.builder().limit(1000).maxBatchRows(250)
    .batches(new File('events.parquet')).withCloseable { batches ->
      while (batches.hasNext()) {
        def batch = batches.next()
        assert batch.rowCount() <= 250
      }
    }

// Explicit lists retain caller order; directories and explicit globs sort normalized paths.
def dataset = MatrixParquetReader.builder().parallelism(2).sourceFileColumn('_source')
    .read([new File('train-00001.parquet'), new File('train-00000.parquet')])
def fromDirectory = MatrixParquetReader.read(new File('train'))
def fromGlob = MatrixParquetReader.readGlob('train/train-*.parquet')
```

A batch normally contains one selected row group. `maxBatchRows` splits large groups; individual
cells and compressed row-group pages still need heap. Close iterators on early exit; exhaustion
and errors close them automatically. Limits count accepted rows across batches/files. Footer
IDs remain valid when filters prune their groups; such groups produce no batch. There is no
ordinal row seeking API.

Datasets validate every footer before returning rows and require identical logical fields,
order, decimal precision/scale, timestamp units/UTC flags and Matrix type metadata, including
`matrix.nestedTypes`. Duplicate
canonical files and empty selections fail. Directory reads include direct `.parquet` children;
globs are explicit and can select deeper paths. Compatible Matrix indices are restored when every shard has the same index metadata and
projection retains its columns. A source column
contains canonical absolute paths and must not collide with a physical column. Parallel workers
use independent streams and converters, reuse each parsed footer, prefetch at most one batch per
worker, and preserve file/group order. `rowGroups` is for one file; use
`rowGroupsByFile: [(file.canonicalPath): [1, 0]]` for explicit dataset group selections.

Directory results use the directory name; list and glob results use the first selected filename
without `.parquet`. `matrixName` overrides either default, including batches.

Globs match paths relative to the literal root. `train/*.parquet` visits only direct children;
`train/**/*.parquet` permits arbitrary depth. `{a,b/c}` alternatives also permit variable depth.
Fixed-depth patterns bound the filesystem walk. A missing root or an empty match raises a contextual
`IllegalArgumentException`. POSIX backslashes escape glob characters (`train/x\\*.parquet` in a
Groovy string selects the literal `x*.parquet`); Windows backslashes are converted to `/` separators,
and glob escaping is unavailable there. `C:*.parquet` drive-relative patterns are rejected; use
`C:/data/*.parquet`. Ordinary `read(String)` paths never expand globs.

## HTTP and Hub URIs

```groovy
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.Predicates

def remote = MatrixParquetReader.builder().remoteReadMode('AUTO').columns(['id'])
    .filter(Predicates.ge('id', 1000L)).limit(100)
    .readUrl('https://huggingface.co/datasets/owner/repo/resolve/main/train.parquet')
def hub = MatrixParquetReader.builder().remoteReadMode('AUTO').limit(20)
    .readUrl('hf://datasets/owner/repo@refs/convert/parquet/train/0000.parquet')
```

Replace owner/repo/path with a real dataset. DOWNLOAD preserves full-download behavior. AUTO
verifies `206 Content-Range` support then uses ranges, or downloads when the initial probe is
unsupported. RANGE requires range support. Once decoding starts, transport/object-change errors
propagate. Range caching is bounded by `rangeCacheBytes`; individual requests are capped separately at
8 MiB. Bulk reads request the needed span rather than splitting it into 64 KiB requests. Small
reads prefetch at most 64 KiB. Validated redirect targets are reused, and fully consumed range
responses permit HTTP keep-alive reuse. ETag/Last-Modified validators and total length detect
changes when available; same-length changes without validators cannot be detected reliably.
Strong ETags use `If-Match`; weak `W/` tags use `If-Unmodified-Since` when Last-Modified is present.
Response ETags are still compared on every fetch to detect changes.
Redirects are bounded and HTTPS downgrades rejected. Connection failures before response headers
are retried once; malformed responses and body failures propagate. Streams are spooled to
owner-only temporary files on POSIX; byte arrays use a seekable memory input.

Hub dataset URI parsing is pinned to huggingface_hub v2.2.0. Revisions follow the repository:
`hf://datasets/owner/repo@revision/path`. Encode ordinary revision slashes as `%2F`; special
`refs/convert/<name>` and `refs/pr/N` forms may remain unescaped. `@` inside a file path is literal.
Dot and parent path/revision segments are rejected. HF URI paths are literal (already-percent-encoded path text is escaped again). `HF_TOKEN` is
optional and read only for explicit hf:// calls; authorization is attached only to HTTPS
`huggingface.co` on its default/443 port and never to CDN redirect origins. This is HTTP range
access, with no Xet download/deduplication client.

## Read options

Use fluent builder methods, `MatrixParquetReader.builder(typedOptions)`, or SPI maps:
`Matrix.read([columns: ['id'], limit: 10L], file)`. Unknown options are rejected. Null map values preserve defaults, including nullable AUTO options.
`ParquetReadOptions.describe()` lists these options at runtime.

| Option | Default | Meaning |
|---|---|---|
| matrixName | null | Explicit name; otherwise filename for local files, schema name for memory/streams/URLs |
| zoneId | system zone (null) | Adjusted timestamp conversion; ZoneId or zone name |
| columns | null/all | Nonempty unique ordered top-level output names |
| filter | null | Immutable Matrix-owned ParquetPredicate |
| binaryAsString | null/AUTO | Legacy Matrix unannotated binary is text; external/new binary is bytes; true reads external unannotated binary as UTF-8 recursively, false reads raw bytes; annotations and explicit new binary metadata take precedence |
| useColumnIndexFilter | true | Permit page pruning; false is useful for result/I/O comparisons |
| limit | null/unlimited | Nonnegative matching-row bound; zero retains schema |
| rowGroups | null/all | Unique, zero-based original footer IDs, caller order |
| rowGroupsByFile | empty/all | Dataset-only canonical file path to ordered group ID list |
| maxBatchRows | null | Positive accepted-row batch cap; default one row group |
| sourceFileColumn | null | Optional canonical local file/shard path column, added after projection; unsupported for memory/stream/URL sources |
| parallelism | 1 | Positive maximum dataset workers across files and row groups |
| remoteReadMode | DOWNLOAD | DOWNLOAD, AUTO or RANGE |
| connectTimeoutMillis | 10000 | Positive HTTP connection timeout |
| readTimeoutMillis | 60000 | Positive HTTP read timeout |
| rangeCacheBytes | 8388608 | Positive per-stream cache ceiling; independent request cap is 8 MiB |

## Writer options and exact types

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetWriter

def values = Matrix.builder('prices').columns(id: [1L, 2L], price: [1.25G, 2.50G]).build()
MatrixParquetWriter.builder(values).writerVersion('V2').compressionCodec('ZSTD')
    .rowGroupSize(16L * 1024 * 1024).pageSize(65536)
    .bloomFilterEnabled([id: true]).bloomFilterNDV([id: 1000L])
    .decimalMeta([price: [8, 2] as int[]]).write(new File('prices.parquet'))
```

File, Path, byte-array and OutputStream writes share writer tuning. Size settings are targets,
not hard memory limits. V2 changes value encoders as well as page layout; V1 remains the default
for compatibility with older consumers. Bloom NDVs affect memory/false-positive tradeoffs and do
not enable a filter by themselves. Column settings use physical primitive paths (for example
`tags.list.element`); unknown/nonprimitive paths fail. Disabled bloom columns remain disabled.

| Option | Default | Meaning |
|---|---|---|
| compressionCodec | SNAPPY | Codec name/CompressionCodecName; tested SNAPPY, ZSTD, GZIP, UNCOMPRESSED |
| timestampUnit | MICROS | MICROS truncates sub-microsecond timestamp digits; NANOS preserves them within int64 range |
| inferPrecisionAndScale | true | Infer exact decimal settings when no explicit settings apply |
| precision / scale | null / null | Uniform positive precision and nonnegative scale, provided together |
| decimalMeta | empty | Logical field path to `[precision, scale]`; overrides uniform settings |
| roundingMode | UNNECESSARY | Reject lossy decimal rescaling; opt in to e.g. HALF_UP deliberately |
| zoneId | system zone (null) | LocalDateTime to adjusted timestamp conversion |
| rowGroupSize | 134217728 | Positive target row-group bytes |
| pageSize | 1048576 | Positive target data-page bytes |
| dictionaryEncoding | true | Dictionary encoding with upstream fallback |
| writerVersion | V1 | V1 or V2 writer encodings and page layout |
| columnIndexTruncateLength | 64 | Positive binary page-bound truncation bytes |
| statisticsTruncateLength | 2147483647 | Positive row-group binary statistics truncation bytes |
| bloomFilterEnabled | empty, disabled | Primitive path to Boolean override; SPI also accepts trimmed true/false strings |
| bloomFilterNDV | empty | Primitive path to positive integral long count; SPI accepts integral numbers and numeric strings, rejecting fractions/overflow |
| sizeStatisticsEnabled | true | Collect size statistics |

Files are written to a temporary sibling and atomically replace the destination after successful
close. Failed writes preserve an existing destination; filesystems without atomic replacement
support fail safely with `AtomicMoveNotSupportedException`. New files use normal process/umask
permissions; existing POSIX permissions are retained. Existing symbolic links are resolved before
writing, including dangling links, so the link is preserved and its target is created or replaced.
Abrupt JVM termination can leave `.matrix-parquet-<uuid>.tmp` siblings beside the resolved output;
these are unpublished temporary output and can be removed once no writer is using them.

JSON and ENUM annotations decode as UTF-8 String; BSON remains binary.
Strings are STRING-annotated recursively; byte[] stays raw BINARY. Byte/Short use signed width
annotations, UUID uses FIXED[16], LocalTime uses TIME(NANOS), and Instant uses adjusted
TIMESTAMP(MICROS) by default. Sub-microsecond digits are truncated consistently for Instant,
LocalDateTime and Timestamp; int64 overflow fails with column/row context. Instant.MIN/MAX
are unsupported. Character and enums become String (`name()` for enums).
ZonedDateTime/OffsetDateTime/custom objects require explicit conversion to Instant/String/Map.
Date/Timestamp subclasses must share a single encoding policy; mixed units fail before output.

Object and abstract/interface declarations scan every non-null value, check assignability and
infer one encoding; canonical metadata replaces the supertype. Concrete declarations require
instance-compatible values and a single runtime encoding policy. Mixed numeric classes need
explicit normalization. All-null Object becomes String; all-null Number/Temporal requires an
explicit supported concrete type. Containers use canonical List/Map metadata. All supported
metadata names are allowlisted; unknown external class names fall back to schema inference.

Every resolved BigDecimal uses exact DECIMAL FIXED storage. Settings precedence is per-field,
uniform, then inference. Nested logical paths use `.element`, `.key`, `.value`, and struct field
names (for example `nested.element.amount`). With inference disabled every decimal needs settings.
All-null BigDecimal infers `(1, 0)`. There is no DOUBLE fallback. Explicit decimal conversion obeys
roundingMode. See [migration notes](release.md) for changed 0.7.x behavior.

Nested collections with different concrete implementations share one LIST/MAP policy. Map values
with different logical policies become STRUCT fields and therefore require String keys. Maps in
lists become STRUCTs when every key is a String; Integer or other supported primitive keys retain
MAP encoding. Readers derive key/value roles from the schema, including nonstandard field names,
legacy outer MAP_KEY_VALUE groups and key-only maps (whose values become null). String-declared columns accept any CharSequence, including GString, and write text.

`matrix.nestedTypes` is a JSON footer object mapping logical leaf paths to canonical Matrix class
names. It preserves nested Instant, Date, Timestamp, sql.Date, Byte, Short and BigInteger values
in lists, map keys/values and struct fields. LocalTime/Time are distinguished by TIME annotations.
Older readers ignore the new key; files without it retain schema inference. Unknown classes and
incompatible entries are ignored, with no external class loading. Segments containing `.` or `\`
are escaped (`\.`, `\\`) so dotted column or struct-key names cannot collide with the path
separator; `decimalMeta` keys remain the raw, unescaped dotted paths.

| Parquet / Matrix type | Read or write behavior |
|---|---|
| UINT8 / UINT16 | Integer; residual predicates |
| UINT32 | Long with unsigned widening; residual predicates |
| UINT64 | BigInteger with unsigned widening; residual predicates |
| Instant / LocalDateTime / Timestamp | TIMESTAMP(MICROS, adjusted=true) by default; truncate sub-microsecond digits; NANOS opt-in |
| External INT96 | Read as UTC instant, returned as LocalDateTime in reader zone; compatible metadata restores Instant/Timestamp/Date; residual predicates; writer never emits INT96 |
| FLOAT16 | Contextual unsupported-type error |

### Timestamp precision

The default `timestampUnit: 'MICROS'` discards sub-microsecond digits downward in time for
LocalDateTime, Timestamp and Instant, including before 1970. LocalDateTime is converted through
the writer zone first. This chiefly affects Linux JDK 15+ nanosecond clocks and Windows 100 ns
clocks; measured macOS clocks provide microseconds. Ordinary `now()` writes therefore succeed.

`timestampUnit('NANOS')` preserves full precision, but Spark-family readers may not support
TIMESTAMP(NANOS). The int64 NANOS range is 1677-09-21T00:12:43.145224192Z through
2262-04-11T23:47:16.854775807Z; values outside it fail. MICROS covers about ±292,000 years
and is suitable for wider dates. Both units reject overflow with column/row context.

Under MICROS, filtering with the original sub-microsecond value finds no equal row because
predicates compare exactly against the truncated stored value. Truncate the filter value too,
or write NANOS:

```groovy
import java.time.Instant
import java.time.temporal.ChronoUnit
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.*

Instant original = Instant.EPOCH.plusNanos(123456789) // also occurs with Instant.now()
def data = Matrix.builder().columns(ts: [original]).build()
byte[] micros = MatrixParquetWriter.builder(data).writeBytes()
assert MatrixParquetReader.builder().filter(Predicates.eq('ts', original)).read(micros).rowCount() == 0
assert MatrixParquetReader.builder().filter(Predicates.eq('ts', original.truncatedTo(ChronoUnit.MICROS)))
    .read(micros).rowCount() == 1
byte[] nanos = MatrixParquetWriter.builder(data).timestampUnit('NANOS').writeBytes()
assert MatrixParquetReader.builder().filter(Predicates.eq('ts', original)).read(nanos).rowCount() == 1
// SPI equivalent, with the same default MICROS if omitted:
data.write([timestampUnit: 'NANOS'], new File('timestamps.parquet'))
```

## CDC output, tests and benchmarks

Native CDC writing is deferred: [parquet-java #3818](https://github.com/apache/parquet-java/pull/3818)
was open/unmerged when checked on 2026-10-17. Tuning pages/row groups is not CDC. For CDC output,
rewrite with PyArrow and preserve metadata:

```bash
python3 -m venv /tmp/matrix-parquet-fixtures
/tmp/matrix-parquet-fixtures/bin/python -m pip install pyarrow==21.0.0
/tmp/matrix-parquet-fixtures/bin/python - <<'PY'
import pyarrow.parquet as pq
table = pq.read_table('prices.parquet')
pq.write_table(table, 'prices-cdc.parquet', compression='zstd',
               use_content_defined_chunking=True, write_page_index=True)
assert pq.read_table('prices-cdc.parquet').equals(table, check_metadata=True)
PY
```

This loads the whole Arrow table and adds a rewrite pass. Python is not a runtime dependency.
Hadoop filesystem configuration is avoided for local reads/writes; upstream codec initialization
still requires Hadoop classes, so the dependency and JDK 21 limit remain.

```bash
./gradlew :matrix-parquet:codenarcMain :matrix-parquet:codenarcTest
./gradlew :matrix-parquet:spotlessCheck
./gradlew :matrix-parquet:test :matrix-parquet:compileConsumerApi
PARQUET_PYTHON=/tmp/matrix-parquet-fixtures/bin/python ./gradlew :matrix-parquet:test -PrunSlowTests=true
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/verify_matrix_output.py
./gradlew :matrix-parquet:test --tests ParquetInteropTest --rerun-tasks
./gradlew test
```

Fixture reproduction: run `matrix-parquet/src/test/python/generate_optimized.py` from the repository root with
pinned PyArrow. Checked-in Parquet fixtures total less than 4 MiB (each less than 1 MiB).
Default-size CDC is generated in `build/default-cdc` only in the slow lane; missing Python/Arrow
produces an explicit skip. CI provisions pinned Arrow and checks fixture reproducibility.
See [measurements and reproduction](benchmarks/optimized-parquet.md) for the opt-in benchmark.
