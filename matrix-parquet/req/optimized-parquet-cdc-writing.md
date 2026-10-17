# Content-defined chunking (CDC) writing: requirements and plan

Status date: 2026-10-17. Scope: native CDC output from `MatrixParquetWriter`, so Matrix can write
Hugging Face "optimized" Parquet (CDC + page indexes) without a Python rewrite step. This follows
up on item 5.2 of [optimized-parquet-plan.md](optimized-parquet-plan.md), which deferred native
CDC until a supported parquet-java release exposes it.

## 1. Background

CDC chooses data-page boundaries from a rolling (gear) hash of each column's values instead of from
fixed byte counts. When rows are inserted, deleted or appended, pages away from the edit keep the
same content and bytes. Content-addressable storage such as Hugging Face Xet can then deduplicate
those pages between revisions
([Hugging Face CDC explanation](https://huggingface.co/blog/parquet-cdc)). Output is ordinary
Parquet; readers need no changes. matrix-parquet 0.8.0 already reads CDC files and writes page
indexes by default.

### 1.1 Upstream state (verified 2026-10-17)

| Item | State | Relevance |
|---|---|---|
| parquet-java release | 1.18.1 is the latest on Maven Central; 1.19.0 is at rc9 (2026-09-16) and not published | Matrix pins `v_parquet = "1.18.1"` in `gradle/libs.versions.toml`; no CDC API exists in it |
| [apache/parquet-java#3817](https://github.com/apache/parquet-java/issues/3817) | Open feature request | Tracking issue |
| [apache/parquet-java#3818](https://github.com/apache/parquet-java/pull/3818) | Open, no reviews yet, last updated 2026-10-17; targets the main branch, not 1.19.0 | Ports Arrow C++/arrow-rs chunker; adds `CdcOptions` and `ParquetWriter.Builder.withContentDefinedChunking(CdcOptions)`, marked EXPERIMENTAL and off by default |
| Arrow C++ / PyArrow | CDC since 21.0.0 ([apache/arrow#45750](https://github.com/apache/arrow/issues/45750)) | Reference implementation and interop oracle |
| [apache/arrow#51685](https://github.com/apache/arrow/pull/51685) | Merged 2026-10-12, after Arrow 26.0.0 (2026-10-09) | Arrow ≤ 26 restarts the chunker for every row group; later Arrow, arrow-rs and #3818 keep the state across row groups within a file |

Facts from #3818 that this plan relies on. Recheck them against the merged and released code
(task 1.3):

- Defaults: `minChunkSize` 256 KiB, `maxChunkSize` 1 MiB, `normLevel` 0, as in Arrow.
- Validation: `minChunkSize >= 0` and `maxChunkSize > minChunkSize`, and the derived mask must be
  1–63 bits. Out-of-range combinations throw `IllegalArgumentException`.
- One chunker per leaf column. Definition and repetition levels are hashed, and cuts happen only
  at `repetitionLevel == 0`, so a page never splits a nested record.
- The page-size, value-count and row-count limits still apply, counted from the start of the page.
- With CDC on, dictionary fallback is decided by dictionary size rather than by parquet-java's
  first-page compression check. All-null column chunks get an empty dictionary page.
- Boundaries match Arrow C++ for the same physical values. Arrow types hashed at their own width
  (narrow integers, decimals, dictionary arrays) chunk differently.
- Upstream reports 12–30% extra write time.
- `ParquetWriter` → `InternalParquetRecordWriter` keeps one chunker set per file automatically.
  `ExampleParquetWriter`, which Matrix uses, inherits this.

## 2. Requirements

### 2.1 Functional

- R1. CDC is opt-in, and it is off by default. Output with CDC off must stay byte-identical to the
  0.8.0 writer for the same inputs and options.
- R2. Expose CDC through Matrix-owned types on `ParquetWriteOptions`,
  `MatrixParquetWriter.WriterBuilder` and the SPI option map. Do not expose parquet-java types (`CdcOptions`) in public signatures; the
  `compileConsumerApi` probe must keep compiling without parquet-java/Hadoop.
- R3. Parameters: `minChunkSize` (long, default 262144), `maxChunkSize` (long, default 1048576),
  `normLevel` (int, default 0). The defaults must equal the upstream defaults of the release
  Matrix adopts.
- R4. Apply CDC on every write path: file (atomic temporary-file replacement), `writeBytes` and SPI
  `write(Matrix, File, Map)`. Tuning, bloom, size-statistics and page-index settings stay
  independent of CDC.
- R5. Validate before creating any output. Report Matrix-contextual errors for negative or
  inverted sizes. Wrap upstream mask-range errors with the option values. Reject CDC options that
  are given while CDC is disabled, so typos are not silently ignored.
- R6. Write page indexes in CDC output, as they are today (the Hugging Face "optimized" definition is
  CDC + page indexes). Do not add an option that turns page indexes off as part of this work.
- R7. Output is deterministic: identical Matrix, options and zone give byte-identical files on every
  path and across runs.

### 2.2 Interoperability and effectiveness

- R8. Files read back losslessly with `MatrixParquetReader` and pinned PyArrow, with all Matrix
  metadata kept.
- R9. Page boundaries must match the pinned PyArrow for physical-width types. Compare per-page row
  boundaries from the OffsetIndex, not compressed bytes, because the encoders differ. The scope
  depends on the PyArrow version:
  - PyArrow ≤ 26 restarts chunking every row group, so only the first row group can match.
  - Arrow ≥ the first release that contains #51685 should match across all row groups.

  Narrow integers (Byte/Short as INT32 with INT(8/16)), decimals and dictionary-typed Arrow
  inputs are documented exceptions, not failures.
- R10. Reuse must be measurably better than non-CDC output. After single-row inserts, deletes and
  updates in the middle of a column, and after appends, the share of data-page bytes that are
  byte-identical between two revisions must exceed the non-CDC baseline. The pass thresholds are
  set from measurement in 5.2.
- R11. Write overhead is measured and documented for the benchmark inputs. It is a regression
  guard, not a correctness gate. Initial guard: CDC median write time ≤ 1.5× non-CDC at default
  settings.

### 2.3 Matrix-specific stability

These Matrix behaviours can change the bytes of a whole column after a small edit, which defeats
dedupe even when CDC itself works:

- R12. Decimal and BigInteger precision/scale inference: one wider value changes the column's
  `FIXED_LEN_BYTE_ARRAY` length and DECIMAL annotation, so every page changes. Document that CDC
  workloads should use explicit `decimalMeta`/`precision`+`scale`. Log a debug-level hint when CDC
  is on and inference was used.
- R13. Zone-dependent encodings: LocalDateTime is stored as an adjusted instant through `zoneId`
  (the system default when unset). Document that reproducible CDC output needs an explicit
  `zoneId`.
- R14. Map and struct ordering: MAP entry order follows the map's iteration order, and struct field
  order follows first-seen key order. Document that maps should have a stable iteration order
  (for example, `LinkedHashMap`) and that a new struct key changes the schema.
- R15. Row-group boundaries are byte-size based and move after edits. Measure the reuse loss near
  boundaries and document it. Do not add row-count-based row groups in this work; revisit only if
  the measurements justify it.

### 2.4 Non-goals

- No chunker implemented in Matrix, no vendored or forked parquet-java, no private upstream patch.
- No mandatory Python/PyArrow bridge. The documented PyArrow rewrite stays as the fallback.
- No Hub upload API, no Xet client, no reader changes.
- No "optimized" preset that silently turns on CDC before CDC exists (see 3.3 for a preset after
  CDC ships).

## 3. Design

### 3.1 Options

| Option (builder / SPI key) | Type | Default | Meaning |
|---|---|---|---|
| `contentDefinedChunking` | boolean | false | Enable CDC data-page boundaries |
| `cdcMinChunkSize` | long | 262144 | Minimum chunk bytes before a content cut is allowed |
| `cdcMaxChunkSize` | long | 1048576 | Forced cut size; must be greater than the minimum |
| `cdcNormLevel` | int | 0 | Mask normalisation; higher values tighten chunk-size spread |

```groovy
MatrixParquetWriter.builder(prices)
    .contentDefinedChunking(true)                 // upstream/Arrow defaults
    .zoneId('UTC')                                // reproducible timestamp bytes (R13)
    .decimalMeta([price: [12, 4] as int[]])       // stable decimal schema (R12)
    .write(new File('prices.parquet'))

MatrixParquetWriter.builder(prices)
    .contentDefinedChunking(65536L, 262144L, 0)   // explicit min, max, normLevel; enables CDC
    .write(new File('prices-small-chunks.parquet'))
```

SPI: `[contentDefinedChunking: true, cdcMinChunkSize: 65536L, cdcMaxChunkSize: 262144L]`. Keys use
the existing case-insensitive normalisation, and null values keep the defaults, as in 0.8.0.

### 3.2 Writer integration

`MatrixParquetWriter.configureWriter` maps the options to
`withContentDefinedChunking(CdcOptions.builder()...build())` when they are enabled and passes
`null` otherwise. This one shared method already serves the file and bytes paths. Atomic
replacement, permission handling and symlink resolution are unchanged.

### 3.3 Optional preset (decision 6.3)

`huggingFaceOptimized()` sets `contentDefinedChunking(true)` and leaves page indexes on. It is
documented as exactly that; codec and row-group settings stay the caller's choice.

### 3.4 Alternatives rejected

| Alternative | Reason |
|---|---|
| Port the chunker into Matrix | Needs per-column page-boundary control inside `ColumnWriteStore`/`ColumnWriterBase`, which is not public API in 1.18.1; duplicates upstream maintenance |
| Depend on a fork or snapshot of #3818 | Unreleased, EXPERIMENTAL, may change; unsuitable for a published library |
| Arrow Java / JNI writer | Native dependency and a different value model; not verified to expose CDC options |
| PyArrow subprocess | Contradicts the no-mandatory-Python rule; stays as documented manual rewrite |

## 4. Plan

### 1. Upstream gate and tracking (P0, blocks 3–6)

- 1.1 [ ] Recheck #3817/#3818 status, review comments, target branch and the release that contains
  it. Record the date, links and the supported parquet-java version in this document. Repeat at
  each Matrix release planning point until merged.
- 1.2 [ ] Draft upstream feedback from the spike (2.x): Matrix-shaped cases such as
  `ExampleParquetWriter`/`Group` records, nested LIST/MAP/struct, V1/V2 pages and
  `PlainParquetConfiguration`. Ask for authorization before posting externally; a reviewable draft
  in this document is enough until then.
- 1.3 [ ] Gate: CDC is in a parquet-java release published on Maven Central. Before upgrading,
  confirm in that release:
  - the `CdcOptions` and builder API names;
  - the defaults and validation rules from 1.1;
  - the cross-row-group behaviour;
  - the dictionary-fallback behaviour;
  - compatibility with JDK 21 and Hadoop 3.5.0.

  Update section 1.1 wherever the release differs from #3818.

### 2. Pre-release spike (not merged to main, not published)

- 2.1 [ ] On a scratch branch, build #3818 into `mavenLocal`. Point matrix-parquet at it locally
  and wire the options from 3.1 through `configureWriter`.
- 2.2 [ ] Run the existing matrix-parquet suite with CDC off to confirm R1 (byte-identical output
  against 0.8.0 for the fixture matrices).
- 2.3 [ ] Prototype the measurements in 5.2 and 5.6 to get baseline numbers and candidate
  thresholds. Record the numbers and environment here.
- 2.4 [ ] Confirm or revise the Matrix-specific risks R12–R15 with measurements. Delete the branch
  after recording results; do not ship any part of it.

### 3. Dependency upgrade (after 1.3)

- 3.1 [ ] Bump `v_parquet` in `gradle/libs.versions.toml` to the gating release. Check transitive
  Hadoop/codec changes, the `codecWithoutHadoop` probe and `compileConsumerApi`. Update the BOM if
  the module version changes.
- 3.2 [ ] Run the full matrix-parquet suite, including `-PrunSlowTests=true`, with CDC off. Fix only
  regressions caused by the upgrade, with targeted tests.

### 4. Matrix API and writer integration

- 4.1 [ ] Add the four options from 3.1 to `ParquetWriteOptions`: fields, setters, `toMap`,
  `fromMap`, `descriptors` and `validate`. Use Matrix-contextual messages for R5. Add tests for
  defaults, null-map entries, unknown keys, case-insensitive keys and CDC options given without
  CDC.
- 4.2 [ ] Add `WriterBuilder.contentDefinedChunking(boolean)` and
  `contentDefinedChunking(long min, long max, int normLevel)`. Document that the three-argument
  form enables CDC. Javadoc must list each parameter and its default.
- 4.3 [ ] Map the options in `configureWriter` (3.2). Wrap upstream `IllegalArgumentException`
  with the offending option values. Verify no partial or temporary file remains after a rejected
  configuration.
- 4.4 [ ] Extend `ParquetFormatProvider` SPI documentation and tests for the new keys.
- 4.5 [ ] Add the R12 debug hint when CDC is on and decimal or BigInteger precision was inferred.
  It must not change output.

### 5. Verification

- 5.1 [ ] PyArrow boundary parity (R9). Extend `generate_optimized.py` to write single-row-group
  CDC files with the pinned PyArrow 21.0.0 for required/optional INT64, optional STRING and
  optional `list<int64>`, at the small fixture sizes from the existing CDC fixtures. Write the same
  values through Matrix with matching CDC, page-size and dictionary settings. Assert equal
  OffsetIndex `firstRowIndex` sequences per column. Document the Byte/Short, decimal and
  dictionary-array exceptions. When an Arrow release with #51685 is available, add a
  multi-row-group parity case and pin that version separately.
- 5.2 [ ] Reuse effectiveness (R10). For a deterministic multi-column Matrix (long strings,
  integers, doubles, a nested list), write revision A, then edit it and write revision B. Edits:
  - insert one row in the middle;
  - delete one row in the middle;
  - update one value;
  - prepend 100 rows;
  - append 100 rows.

  Hash each data page's bytes using OffsetIndex locations, and compute the reused byte share for
  CDC and non-CDC. Assert CDC > non-CDC for every edit, and assert the CDC share is at least the
  thresholds fixed from 2.3. Run at sizes that span several pages and, in the slow lane, several
  row groups.
- 5.3 [ ] Determinism (R7). Writing the same Matrix twice must give byte-identical files. The file
  and `writeBytes` paths must give identical bytes, and the SPI path must match the builder path.
  CDC off must still match the 0.8.0 output for the existing fixtures (R1).
- 5.4 [ ] Round-trip (R8). Read CDC output with `MatrixParquetReader` (values, types, index
  metadata) and with PyArrow (`verify_matrix_output.py`: `equals` against the source table, page
  index present). Cover V1 and V2 pages, SNAPPY and ZSTD, all-null columns and every supported
  scalar policy type.
- 5.5 [ ] Nested and edge shapes: LIST, MAP and struct columns. Check that no page splits a record
  (each page's first row starts at `repetitionLevel == 0`). Also cover empty matrices, single-row
  matrices, and `pageSize < cdcMaxChunkSize` (document which limit cuts first).
- 5.6 [ ] Performance (R11). Extend the write benchmark with CDC on and off on the existing
  benchmark inputs. Record median time, output size and peak heap in
  `matrix-parquet/benchmarks/optimized-parquet.md`, and add the 1.5× guard as an opt-in benchmark
  assertion.

### 6. Documentation, decisions and release

- 6.1 [ ] Replace the "Native CDC writing is deferred" text in `readme.md`,
  `docs/cookbook/matrix-parquet.md`, `docs/tutorial/11-matrix-parquet.md` and `release.md`:
  - option table with defaults and validation;
  - the two examples from 3.1;
  - the stability guidance from R12–R15;
  - the PyArrow parity scope from R9;
  - the measured reuse and overhead numbers.

  Keep the PyArrow rewrite as an alternative for users on older Matrix versions.
- 6.2 [ ] Mark item 5.2 of `optimized-parquet-plan.md` as superseded by this document. Link the
  adopted parquet-java release.
- 6.3 [ ] Decide whether to ship the `huggingFaceOptimized()` preset (3.3). Record the decision
  and rationale here.
- 6.4 [ ] Decide the matrix-parquet version (the first release after the gating parquet-java
  release). Do not create git tags.
- 6.5 [ ] Final gates, in order. Record the commands and results here or in the PR description
  before checking any box above:

```bash
./gradlew :matrix-parquet:codenarcMain :matrix-parquet:codenarcTest --console=plain
./gradlew :matrix-parquet:spotlessCheck --console=plain
PARQUET_PYTHON=/tmp/matrix-parquet-fixtures/bin/python \
  ./gradlew :matrix-parquet:test :matrix-parquet:compileConsumerApi -PrunSlowTests=true --console=plain
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/verify_matrix_output.py
./gradlew test -Pheadless=true --console=plain
```

## 5. Open questions

- Q1. Should `contentDefinedChunking(true)` also refuse to use system-default `zoneId` when the
  Matrix has LocalDateTime columns (strict R13), or only document it? This plan proposes to
  document it only.
- Q2. Should Matrix keep the upstream EXPERIMENTAL label on its own options until parquet-java
  drops it? This plan proposes yes: say "experimental" in the Javadoc and release notes, while
  keeping the Matrix option names stable.
- Q3. Is row-count-based row-group sizing worth a separate follow-up if 2.4/5.2 show significant
  reuse loss at row-group boundaries (R15)?
