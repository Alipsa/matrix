package se.alipsa.matrix.avro.benchmarks

import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Runs Matrix Avro in-memory conversion benchmarks.
 */
class MatrixAvroConversionBenchmark {

  private static final int RUNS_ARG_INDEX = 2
  private static final int DEFAULT_RUNS = 5
  private static final int BENCHMARK_YEAR = 2024
  private static final int SECONDS_PER_MINUTE = 60
  private static final int TAG_MODULO = 5
  static void main(String[] args) {
    int rows = args.size() > 0 ? (args[0] as int) : 50_000
    int warmups = args.size() > 1 ? (args[1] as int) : 2
    int runs = args.size() > RUNS_ARG_INDEX ? (args[RUNS_ARG_INDEX] as int) : DEFAULT_RUNS
    Matrix matrix = buildMatrix(rows)
    for (int i = 0; i < warmups; i++) {
      byte[] bytes = MatrixAvroWriter.writeBytes(matrix, true)
      MatrixAvroReader.read(bytes, 'Warmup')
    }
    List<Long> writeTimes = []
    List<Long> readTimes = []
    for (int i = 0; i < runs; i++) {
      byte[] bytes = null
      long writeNs = BenchmarkUtils.timeNs {
        bytes = MatrixAvroWriter.writeBytes(matrix, true)
      }
      long readNs = BenchmarkUtils.timeNs {
        MatrixAvroReader.read(bytes, 'Conversion')
      }
      writeTimes << writeNs
      readTimes << readNs
    }
    println 'MatrixAvroConversionBenchmark'
    println "Rows: ${rows}, Warmups: ${warmups}, Runs: ${runs}"
    println "Write bytes avg: ${BenchmarkUtils.avgMs(writeTimes)} (min: ${BenchmarkUtils.minMs(writeTimes)}, max: ${BenchmarkUtils.maxMs(writeTimes)})"
    println "Read bytes  avg: ${BenchmarkUtils.avgMs(readTimes)} (min: ${BenchmarkUtils.minMs(readTimes)}, max: ${BenchmarkUtils.maxMs(readTimes)})"
  }
  private static Matrix buildMatrix(int rows) {
    Map<String, List<?>> cols = [:]
    cols['id'] = (1..rows).toList()
    cols['amount'] = (1..rows).collect { it / 7 }
    cols['date'] = (1..rows).collect { LocalDate.of(BENCHMARK_YEAR, 1, 1).plusDays(it % 365) }
    cols['time'] = (1..rows).collect { LocalTime.of(10, 30).plusSeconds(it % SECONDS_PER_MINUTE) }
    cols['timestamp'] = (1..rows).collect { Instant.ofEpochMilli(1_700_000_000_000L + it) }
    cols['localTs'] = (1..rows).collect { LocalDateTime.of(BENCHMARK_YEAR, 1, 1, 12, 0).plusMinutes(it % SECONDS_PER_MINUTE) }
    cols['uuid'] = (1..rows).collect { UUID.nameUUIDFromBytes(("id-${it}").bytes) }
    cols['tags'] = (1..rows).collect { ["t${it % TAG_MODULO}", "t${(it + 1) % TAG_MODULO}"] }
    cols['props'] = (1..rows).collect { [a: it, b: "v${it}"] }
    cols['blob'] = (1..rows).collect { ("row-${it}" as String).bytes }
    return Matrix.builder('ConversionBenchmark')
        .columns(cols)
        .types(Integer, BigDecimal, LocalDate, LocalTime, Instant, LocalDateTime, UUID, List, Map, byte[])
        .build()
  }

}
