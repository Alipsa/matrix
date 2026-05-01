package se.alipsa.matrix.avro.benchmarks

import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix
import java.nio.file.Files

/**
 * Runs Matrix Avro read/write throughput benchmarks.
 */
class MatrixAvroBenchmark {

  private static final int RUNS_ARG_INDEX = 2
  private static final int DEFAULT_RUNS = 5
  private static final int VALUE_MULTIPLIER = 2
  static void main(String[] args) {
    int rows = args.size() > 0 ? (args[0] as int) : 100_000
    int warmups = args.size() > 1 ? (args[1] as int) : 2
    int runs = args.size() > RUNS_ARG_INDEX ? (args[RUNS_ARG_INDEX] as int) : DEFAULT_RUNS
    Matrix matrix = buildMatrix(rows)
    File tmp = Files.createTempFile('matrix-avro-benchmark-', '.avro').toFile()
    try {
      for (int i = 0; i < warmups; i++) {
        runOnce(matrix, tmp)
      }
      List<Long> writeTimes = []
      List<Long> readTimes = []
      for (int i = 0; i < runs; i++) {
        def results = runOnce(matrix, tmp)
        writeTimes << results[0]
        readTimes << results[1]
      }
      println 'MatrixAvroBenchmark'
      println "Rows: ${rows}, Warmups: ${warmups}, Runs: ${runs}"
      println "Write ms avg: ${BenchmarkUtils.avgMs(writeTimes)} (min: ${BenchmarkUtils.minMs(writeTimes)}, max: ${BenchmarkUtils.maxMs(writeTimes)})"
      println "Read  ms avg: ${BenchmarkUtils.avgMs(readTimes)} (min: ${BenchmarkUtils.minMs(readTimes)}, max: ${BenchmarkUtils.maxMs(readTimes)})"
      println "File size: ${tmp.length()} bytes"
    } finally {
      tmp.delete()
    }
  }
  private static List<Long> runOnce(Matrix matrix, File tmp) {
    long writeNs = BenchmarkUtils.timeNs {
      MatrixAvroWriter.write(matrix, tmp)
    }
    long readNs = BenchmarkUtils.timeNs {
      MatrixAvroReader.read(tmp)
    }
    return [writeNs, readNs]
  }
  private static Matrix buildMatrix(int rows) {
    Map<String, List<?>> cols = [:]
    cols['id'] = (1..rows).toList()
    cols['value'] = (1..rows).collect { it * VALUE_MULTIPLIER }
    cols['name'] = (1..rows).collect { 'row' + it }
    return Matrix.builder('Benchmark')
        .columns(cols)
        .types(Integer, Integer, String)
        .build()
  }

}
