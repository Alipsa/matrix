package se.alipsa.matrix.avro.benchmarks

import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Runs Matrix Avro schema inference benchmarks.
 */
class MatrixAvroSchemaBenchmark {

  private static final int RUNS_ARG_INDEX = 2
  private static final int DEFAULT_RUNS = 5
  private static final int BENCHMARK_YEAR = 2024
  private static final int TAG_MODULO = 3
  static void main(String[] args) {
    int rows = args.size() > 0 ? (args[0] as int) : 100_000
    int warmups = args.size() > 1 ? (args[1] as int) : 2
    int runs = args.size() > RUNS_ARG_INDEX ? (args[RUNS_ARG_INDEX] as int) : DEFAULT_RUNS
    Matrix matrix = buildMatrix(rows)
    List<Long> inferFalse = runSchemaBenchmark(matrix, warmups, runs, false)
    List<Long> inferTrue = runSchemaBenchmark(matrix, warmups, runs, true)
    println 'MatrixAvroSchemaBenchmark'
    println "Rows: ${rows}, Warmups: ${warmups}, Runs: ${runs}"
    println "Infer precision/scale = false avg: ${BenchmarkUtils.avgMs(inferFalse)} (min: ${BenchmarkUtils.minMs(inferFalse)}, max: ${BenchmarkUtils.maxMs(inferFalse)})"
    println "Infer precision/scale = true  avg: ${BenchmarkUtils.avgMs(inferTrue)} (min: ${BenchmarkUtils.minMs(inferTrue)}, max: ${BenchmarkUtils.maxMs(inferTrue)})"
  }
  private static List<Long> runSchemaBenchmark(Matrix matrix, int warmups, int runs, boolean infer) {
    for (int i = 0; i < warmups; i++) {
      MatrixAvroWriter.buildSchema(matrix, infer)
    }
    List<Long> times = []
    for (int i = 0; i < runs; i++) {
      times << BenchmarkUtils.timeNs {
        MatrixAvroWriter.buildSchema(matrix, infer)
      }
    }
    return times
  }
  private static Matrix buildMatrix(int rows) {
    Map<String, List<?>> cols = [:]
    cols['id'] = (1..rows).toList()
    cols['amount'] = (1..rows).collect { it / 10 }
    cols['created'] = (1..rows).collect { LocalDate.of(BENCHMARK_YEAR, 1, 1).plusDays(it % 365) }
    cols['event'] = (1..rows).collect { LocalDateTime.of(BENCHMARK_YEAR, 1, 1, 12, 0).plusMinutes(it % 60) }
    cols['tags'] = (1..rows).collect { ["tag${it % TAG_MODULO}", "tag${(it + 1) % TAG_MODULO}"] }
    cols['attrs'] = (1..rows).collect { [a: it, b: "v${it}"] }
    return Matrix.builder('SchemaBenchmark')
        .columns(cols)
        .types(Integer, BigDecimal, LocalDate, LocalDateTime, List, Map)
        .build()
  }

}
