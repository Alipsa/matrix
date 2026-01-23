package se.alipsa.matrix.avro.benchmarks

import groovy.transform.CompileStatic
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

import java.nio.file.Files

@CompileStatic
class MatrixAvroBenchmark {

  static void main(String[] args) {
    int rows = args.size() > 0 ? (args[0] as int) : 100_000
    int warmups = args.size() > 1 ? (args[1] as int) : 2
    int runs = args.size() > 2 ? (args[2] as int) : 5

    Matrix matrix = buildMatrix(rows)
    File tmp = Files.createTempFile("matrix-avro-benchmark-", ".avro").toFile()

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

      println "MatrixAvroBenchmark"
      println "Rows: ${rows}, Warmups: ${warmups}, Runs: ${runs}"
      println "Write ms avg: ${avgMs(writeTimes)} (min: ${minMs(writeTimes)}, max: ${maxMs(writeTimes)})"
      println "Read  ms avg: ${avgMs(readTimes)} (min: ${minMs(readTimes)}, max: ${maxMs(readTimes)})"
      println "File size: ${tmp.length()} bytes"
    } finally {
      tmp.delete()
    }
  }

  private static List<Long> runOnce(Matrix matrix, File tmp) {
    long writeNs = timeNs {
      MatrixAvroWriter.write(matrix, tmp)
    }
    long readNs = timeNs {
      MatrixAvroReader.read(tmp)
    }
    return [writeNs, readNs]
  }

  private static long timeNs(Closure action) {
    long start = System.nanoTime()
    action.call()
    return System.nanoTime() - start
  }

  private static Matrix buildMatrix(int rows) {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = (1..rows).toList()
    cols["value"] = (1..rows).collect { it * 2 }
    cols["name"] = (1..rows).collect { "row" + it }

    return Matrix.builder("Benchmark")
        .columns(cols)
        .types(Integer, Integer, String)
        .build()
  }

  private static String avgMs(List<Long> values) {
    if (values.isEmpty()) return "0"
    long sum = values.sum() as long
    return String.format(Locale.US, "%.2f", sum / 1_000_000.0d / values.size())
  }

  private static String minMs(List<Long> values) {
    if (values.isEmpty()) return "0"
    return String.format(Locale.US, "%.2f", (values.min() as long) / 1_000_000.0d)
  }

  private static String maxMs(List<Long> values) {
    if (values.isEmpty()) return "0"
    return String.format(Locale.US, "%.2f", (values.max() as long) / 1_000_000.0d)
  }
}
