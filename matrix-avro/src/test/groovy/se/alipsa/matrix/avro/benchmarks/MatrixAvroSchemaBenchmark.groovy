package se.alipsa.matrix.avro.benchmarks

import groovy.transform.CompileStatic
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

import java.time.LocalDate
import java.time.LocalDateTime

@CompileStatic
class MatrixAvroSchemaBenchmark {

  static void main(String[] args) {
    int rows = args.size() > 0 ? (args[0] as int) : 100_000
    int warmups = args.size() > 1 ? (args[1] as int) : 2
    int runs = args.size() > 2 ? (args[2] as int) : 5

    Matrix matrix = buildMatrix(rows)

    List<Long> inferFalse = runSchemaBenchmark(matrix, warmups, runs, false)
    List<Long> inferTrue = runSchemaBenchmark(matrix, warmups, runs, true)

    println "MatrixAvroSchemaBenchmark"
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
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = (1..rows).toList()
    cols["amount"] = (1..rows).collect { it / 10 }
    cols["created"] = (1..rows).collect { LocalDate.of(2024, 1, 1).plusDays(it % 365) }
    cols["event"] = (1..rows).collect { LocalDateTime.of(2024, 1, 1, 12, 0).plusMinutes(it % 60) }
    cols["tags"] = (1..rows).collect { ["tag${it % 3}", "tag${(it + 1) % 3}"] }
    cols["attrs"] = (1..rows).collect { [a: it, b: "v${it}"] }

    return Matrix.builder("SchemaBenchmark")
        .columns(cols)
        .types(Integer, BigDecimal, LocalDate, LocalDateTime, List, Map)
        .build()
  }
}
