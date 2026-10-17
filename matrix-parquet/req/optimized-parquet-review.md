# 9. PR #481 review fixes

The numbered tasks correspond to the thirteen findings in the review. The fixes were developed
on `feature/optimized-parquet-review`, based on the original PR commit eee2b16e.

- 9.1 [x] Handle empty dictionaries without a descending Groovy range. Add the exact three-row
  all-null PyArrow string fixture, generator provenance and a read regression.
- 9.2 [x] Decode JSON and ENUM annotations as UTF-8 String, including dictionary conversion;
  retain BSON bytes. Test PyArrow JSON and Java ENUM/BSON with binary overrides and update the
  migration table.
- 9.3 [x] Separate the 8 MiB request cap from the configured cache ceiling. Bulk reads use the
  requested span; reuse validated redirect targets, refresh rejected signed targets through the
  original URL, and leave fully consumed successful range connections eligible for keep-alive.
  Measure actual request counts with a local redirect/range server.
- 9.4 [x] Route static URL/URL-string overloads through the builder transport, including named
  and zoned hf:// overloads. Test bounded timeouts, downgrade rejection and Hub parsing.
- 9.5 [x] Write local files to temporary siblings and atomically replace the destination only
  after successful close. Preserve existing POSIX permissions and clean temporary files on
  failure. Test rounding and precision failures after a valid row and compare original bytes.
- 9.6 [x] Support sourceFileColumn for local single-file reads and batches. Restore common
  compatible indices for both collected and batched dataset reads, including truncated batches;
  omit indices when projection removes their columns or shards disagree on index metadata.
- 9.7 [x] Resolve each top-level column's validated type policy once and carry it in WriteSchema
  through decimal/schema construction and metadata emission. Remove obsolete duplicate inference
  helpers and report full nested logical paths; test nested error context.
- 9.8 [x] Treat null option-map values as absent consistently, preserving Boolean/default/map
  values and nullable AUTO semantics. Reject null bloom entries with contextual validation.
- 9.9 [x] Create dataset worker threads through a daemon ThreadFactory. Assert their daemon
  status while an iterator remains open; explicit close is still required for prompt cleanup.
- 9.10 [x] Route named/zoned File/Path/string overloads through the shared directory-capable
  builder and test directory results, names, time-zone values and index restoration.
- 9.11 [x] Reject literal dot/parent path segments and decoded revision dot/parent segments;
  retain the pinned client URL cases. Test nested paths and encoded revision traversal.
- 9.12 [x] Reject unrecoverable legacy [B metadata only for required output/filter columns.
  Test successful projection around the damaged column and rejection when a filter needs it.
- 9.13 [x] Correct the reader's memory/batch/cache comments and writer option documentation,
  including literal V1 defaults; update source/index/transport behavior in the module guide.

## Verification

The required ordered module gates passed:

```bash
./gradlew :matrix-parquet:codenarcMain :matrix-parquet:codenarcTest --console=plain
./gradlew :matrix-parquet:spotlessCheck --console=plain
PARQUET_PYTHON=/tmp/matrix-parquet-fixtures/bin/python \
  ./gradlew :matrix-parquet:test :matrix-parquet:compileConsumerApi -PrunSlowTests=true --console=plain
./gradlew test -Pheadless=true --console=plain
```

The Parquet suite reported **133 tests, 128 passed, zero failed, five intentional opt-in
benchmark/capture skips**. Default-size CDC generation/read and the downstream consumer compile
probe passed. The full repository task passed; unaffected tasks may be up-to-date.

Additional successful checks:

```bash
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/generate_optimized.py
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/verify_matrix_output.py
```

Fixture regeneration is deterministic. Checked-in Parquet binaries total **738,113 bytes**,
below the 4 MiB aggregate and 1 MiB individual limits. Arrow verifies SNAPPY/ZSTD strings,
binary payloads, exact decimals and metadata-preserving CDC rewrites.

HTTP request-count assertions show one 1 MiB data request after probing, one redirect resolution
per input, and no extra request when the default cache serves the repeated block. A 1 KiB cache
still permits the same single 1 MiB bulk request. The test checks byte-array and ByteBuffer
readFully paths. Successful bodies remain eligible for JDK keep-alive pooling; socket-level
connection reuse and live Hub latency were not measured. Existing selective-transfer, page-index
and bloom-filter byte-reduction tests also pass.

## 10. Follow-up output and range-buffer review

- 10.1 [x] Create temporary siblings with ordinary file creation permissions, so new output
  follows the process umask. Compare new Parquet permissions with an ordinary sibling file on
  POSIX, while retaining the existing-destination permission regression.
- 10.2 [x] Resolve existing output paths before creating the sibling and replacing the target.
  Test that relative symbolic links survive both failed and successful writes, and that the
  resolved file retains its contents on failure and receives the new rows on success.
- 10.3 [x] Document the same-directory atomic replacement requirement and
  `AtomicMoveNotSupportedException` in the release notes and module guide. Keep safe failure
  rather than introduce a replacement fallback with weaker failure guarantees.
- 10.4 [x] Read directly into array-backed ByteBuffers using their array offset and current
  position. Retain the bounded scratch buffer for direct buffers. Exercise sliced buffers with
  nonzero positions, sentinel bytes outside the slice, direct buffers, EOF and empty reads in
  the existing HTTP request-count regression.

Follow-up verification passed, in the required order:

```bash
./gradlew :matrix-parquet:codenarcMain :matrix-parquet:codenarcTest :matrix-parquet:spotlessCheck --console=plain
PARQUET_PYTHON=/tmp/matrix-parquet-fixtures/bin/python \
  ./gradlew :matrix-parquet:test :matrix-parquet:compileConsumerApi -PrunSlowTests=true --console=plain
./gradlew test -Pheadless=true --console=plain
```

The Parquet suite reported **135 tests, 130 passed, zero failed, five intentional skips**,
including successful POSIX permission/symlink tests, Python interoperability and consumer API
compilation. The full repository task passed; unaffected tasks may be up-to-date.

## 11. Dangling symlinks and interrupted-write documentation

- 11.1 [x] Resolve symbolic links even when their destination does not exist, including relative
  and absolute targets and link chains. Reject cycles before creating temporary output. Test
  successful target creation and failed writes preserving dangling links and absent targets.
- 11.2 [x] Explain that abrupt JVM termination may leave `.matrix-parquet-<uuid>.tmp` siblings
  beside the resolved output and that unused leftovers can be removed.

Verification passed in order:

```bash
./gradlew :matrix-parquet:codenarcMain :matrix-parquet:codenarcTest --console=plain
./gradlew :matrix-parquet:spotlessCheck --console=plain
PARQUET_PYTHON=/tmp/matrix-parquet-fixtures/bin/python \
  ./gradlew :matrix-parquet:test :matrix-parquet:compileConsumerApi -PrunSlowTests=true --console=plain
./gradlew test -Pheadless=true --console=plain
```

The Parquet suite reported **137 tests, 132 passed, zero failed, five intentional skips**.
The POSIX dangling-link and cycle tests ran successfully, as did Python interoperability,
consumer API compilation and the full repository suite; unaffected tasks may be up-to-date.
