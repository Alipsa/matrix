# Optimized Parquet measurements

Measured 2026-10-17 on macOS arm64 (Apple M5 Max), G1 GC, OpenJDK 21.0.12.1, 4 GiB test heap, parquet-java 1.18.1,
SNAPPY, default 128 MiB row-group target and 1 MiB pages. Each workload has 1,000,000 rows,
an Integer ID and six String columns. Wide text repeats 1,000 labels per column; categorical
repeats 12. Mixed adds LocalDate and Map(label, exact BigDecimal) columns. Generated inputs
live in build/, outside git. The compared Group loop uses shared corrected value conversion.

| Workload | File bytes | Group median ms | Columnar median ms | Lower median time | Group retained bytes | Columnar retained bytes |
|---|---:|---:|---:|---:|---:|---:|
| wide | 4,898,180 | 7,990 | 767 | 90.4% | 381,674,424 | 45,751,352 |
| categorical | 4,173,171 | 7,940 | 775 | 90.2% | 342,270,976 | 45,364,064 |
| mixed/nested | 7,934,477 | 18,569 | 2,462 | 86.7% | 790,769,704 | 406,370,144 |

Medians exclude run 0; three warmed runs are included. Group warmed elapsed ranges were
7,368–10,150 / 6,845–11,093 / 18,334–21,881 ms. Columnar ranges were 765–782 / 763–783 /
2,332–2,486 ms. One codec-spike build overlapped the wide Group run and contributes to its
variance; these are local development measurements, not universal speed guarantees. The
original pre-change reader baseline was separately captured before production edits at
5,633 / 5,413 / 5,433 / 5,424 ms on the same million-row wide shape (file 4,898,111 bytes).

Current-thread allocated bytes were approximately 13.83 GB versus 1.32 GB (wide), 13.78 GB
versus 1.31 GB (categorical), and 38.53 GB versus 3.52 GB (mixed). Retained deltas are sampled
after explicit full GC with the result live. They measure retained result heap, not peak RSS
or a rigorously sampled peak-live-heap metric. In particular the categorical retained result
uses 86.7% less heap; do not relabel that measurement as peak heap. No OS cache purge was
performed. Run 0 represents first application access, not a guaranteed cold filesystem cache.
Timing thresholds are absent from correctness CI.

## Confirmation with GC live-heap sampling

A second million-row run added GC notifications. The sampler records the maximum sum of heap
pool usage **after GC** during each read, including the final full GC with the result live.
This is sampled peak live heap, including benchmark inputs/buffers, not instantaneous heap
occupancy between GCs or process RSS. Medians below exclude run 0 (three warmed runs).

| Workload | Group ms | Columnar ms | Group sampled live peak bytes | Columnar sampled live peak bytes |
|---|---:|---:|---:|---:|
| wide | 7,280 | 764 | 1,009,640,184 | 584,545,248 |
| categorical | 7,158 | 797 | 963,066,680 | 541,390,488 |
| mixed/nested | 18,936 | 2,238 | 1,998,185,928 | 1,343,803,744 |

Wide elapsed time is 89.5% lower; categorical sampled live heap is 43.8% lower; mixed elapsed
time is 88.2% lower. Thus the initial 20% elapsed / 25% live-heap / at-most-5% regression targets
are met on these workloads with this stated sampling method. Warmed elapsed ranges were
7,230–7,295 / 6,922–7,263 / 18,605–19,304 ms for Group and 752–786 / 776–800 / 2,213–2,306 ms
for columnar. Allocations were approximately 14.20/1.32, 14.15/1.31 and 40.16/3.40 GB respectively.
These local measurements remain evidence, not performance guarantees or CI timing assertions.

## Reproduction

Create an init file outside the repository:

```groovy
// /tmp/parquet-benchmark.init.gradle
gradle.projectsEvaluated {
  gradle.rootProject.project(':matrix-parquet').tasks.named('test') {
    systemProperty 'parquet.baseline', 'true'
    maxHeapSize = '4g'
  }
}
```

```bash
./gradlew :matrix-parquet:test --tests '*ParquetReadBenchmarkTest' \
  -I /tmp/parquet-benchmark.init.gradle --no-configuration-cache --console=plain
```

The BENCH records include workload, implementation, run, rows, file bytes, elapsed milliseconds,
thread allocation and retained heap. Avoid concurrent builds for isolated timing measurements.

## Correctness and I/O evidence

`ParquetPruningTest` inspects actual offset/column indexes and multiple pages, compares indexed
and nonindexed byte counts with row-group/statistics/dictionary/bloom pruning disabled, and
checks dictionary String identity. `ParquetDatasetAndHttpTest` counts transferred range bytes
for narrow projection and selective filtering, checks fallback and ETag changes, and compares
ordered sequential/parallel shard results. These assertions use bytes/results, not wall clocks.

The no-Hadoop spike (`./gradlew :matrix-parquet:codecWithoutHadoop`) found that SNAPPY, ZSTD,
GZIP and UNCOMPRESSED writer initialization all require
`org/apache/hadoop/io/compress/zlib/ZlibCompressor$CompressionLevel`. Therefore Hadoop remains
on the runtime classpath even though local InputFile/OutputFile use PlainParquetConfiguration.

The public Java consumer probe (`./gradlew :matrix-parquet:compileConsumerApi`) removes
parquet-java and Hadoop from its compile classpath and successfully compiles projection,
Predicates, limits and writer tuning. This verifies that new public predicates do not require
consumers to add parquet-column explicitly.

## Bounded dataset parallelism

Four shards of 250,000 rows each (Integer ID and 1,000 categorical text labels), SNAPPY,
maxBatchRows=65,536, all 1,000,000 rows materialized in deterministic order:

| Workers | Warm ms (runs 1–3) | Median ms | Median sampled live peak bytes |
|---|---|---:|---:|
| 1 | 370 / 341 / 308 | 341 | 66,905,816 |
| 4 | 247 / 242 / 239 | 242 | 57,235,736 |

This local workload gains about 29% median throughput time with four workers. Heap samples vary
with collection timing; the measured queue stays bounded to one batch per worker, not a claim
that parallel reads always use less memory. The benchmark verifies first/last IDs and row count.
Run `*ParquetDatasetBenchmarkTest` with the same init setup but system property
`parquet.datasetBenchmark=true` instead of `parquet.baseline`.

Observed deterministic I/O checks on the controlled fixtures: page-index reads 2,914 bytes
versus 82,700 without page pruning; bloom-filter reads 16,737 versus 40,373 with statistics
unable to exclude the interior key and dictionaries/pages disabled; selective HTTP transferred
88,476 versus a 906,195-byte full object. HTTP data uses generated UUID strings, so exact byte
counts vary; assertions require reduced transfer and exact returned rows, not these constants.

## Identical Arrow layouts: full scan, projection and predicate

PyArrow 21.0.0 generated 1,000,000 sorted int64 IDs and six text columns (1,000 repeated labels,
about 140 characters/label), four 250,000-row groups, SNAPPY, 64 KiB target pages, default
dictionary encoding and default CDC sizes. Only CDC/index settings differ. No OS cache purge;
run 0 is warmup, medians include runs 1–3. Heap is the same GC live sample defined above.

| Layout | Mode | Returned rows | File bytes | Read bytes (footer included) | Median ms | Median live peak bytes |
|---|---|---:|---:|---:|---:|---:|
| ordinary | full | 1,000,000 | 6,066,382 | 6,066,378 | 727 | 110,855,320 |
| ordinary | id projection | 1,000,000 | 6,066,382 | 5,085,954 | 214 | 55,266,648 |
| ordinary | id >= 999900 | 100 | 6,066,382 | 1,279,020 | 99 | 32,019,888 |
| indexed | full | 1,000,000 | 6,069,245 | 6,021,834 | 741 | 117,800,072 |
| indexed | id projection | 1,000,000 | 6,069,245 | 5,082,882 | 226 | 56,010,848 |
| indexed | id >= 999900 | 100 | 6,069,245 | 552,869 | 7 | 27,740,176 |
| CDC + indexed | full | 1,000,000 | 7,250,428 | 6,968,991 | 693 | 111,151,624 |
| CDC + indexed | id projection | 1,000,000 | 7,250,428 | 5,105,311 | 209 | 57,589,672 |
| CDC + indexed | id >= 999900 | 100 | 7,250,428 | 542,383 | 3 | 27,766,232 |

The selective read benefits from row-group pruning in every layout and page pruning where
indexes exist. These short timings demonstrate this workload, not a guaranteed CDC speedup;
CDC has a larger file here. The independent page-index test isolates page pruning alone.

Reproduce with pinned PyArrow and the same init script as above, replacing its system property
with `parquet.layoutBenchmark=true`:

```bash
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/generate_benchmarks.py
./gradlew :matrix-parquet:test --tests '*ParquetLayoutBenchmarkTest' \
  -I /tmp/parquet-layout.init.gradle --no-configuration-cache --console=plain
```

The generator writes only build/layout-benchmarks. The optional test checks returned counts and
last IDs and emits per-run time, exact input bytes and sampled live heap for every combination.
