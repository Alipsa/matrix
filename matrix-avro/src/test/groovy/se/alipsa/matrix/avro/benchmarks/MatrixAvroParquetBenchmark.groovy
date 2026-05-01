package se.alipsa.matrix.avro.benchmarks

import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Compares Matrix Avro and Parquet read/write benchmarks.
 */
class MatrixAvroParquetBenchmark {

  private static final int RUNS_ARG_INDEX = 2
  private static final int DEFAULT_RUNS = 5
  private static final int BENCHMARK_YEAR = 2024
  private static final String TEMP_FILE_PREFIX = 'matrix-avro-parquet-benchmark-'
  static void main(String[] args) {
    int rows = args.size() > 0 ? (args[0] as int) : 100_000
    int warmups = args.size() > 1 ? (args[1] as int) : 2
    int runs = args.size() > RUNS_ARG_INDEX ? (args[RUNS_ARG_INDEX] as int) : DEFAULT_RUNS
    Matrix matrix = buildMatrix(rows)
    File avroFile = Files.createTempFile(TEMP_FILE_PREFIX, '.avro').toFile()
    File parquetFile = Files.createTempFile(TEMP_FILE_PREFIX, '.parquet').toFile()
    try {
      for (int i = 0; i < warmups; i++) {
        MatrixAvroWriter.write(matrix, avroFile, true)
        MatrixAvroReader.read(avroFile)
        MatrixParquetWriter.write(matrix, parquetFile, true)
        MatrixParquetReader.read(parquetFile)
      }
      List<Long> avroWriteTimes = []
      List<Long> avroReadTimes = []
      List<Long> parquetWriteTimes = []
      List<Long> parquetReadTimes = []
      for (int i = 0; i < runs; i++) {
        avroWriteTimes << BenchmarkUtils.timeNs {
          MatrixAvroWriter.write(matrix, avroFile, true)
        }
        avroReadTimes << BenchmarkUtils.timeNs {
          MatrixAvroReader.read(avroFile)
        }
        parquetWriteTimes << BenchmarkUtils.timeNs {
          MatrixParquetWriter.write(matrix, parquetFile, true)
        }
        parquetReadTimes << BenchmarkUtils.timeNs {
          MatrixParquetReader.read(parquetFile)
        }
      }
      println 'MatrixAvroParquetBenchmark'
      println "Rows: ${rows}, Warmups: ${warmups}, Runs: ${runs}"
      println "Avro write avg: ${BenchmarkUtils.avgMs(avroWriteTimes)} (min: ${BenchmarkUtils.minMs(avroWriteTimes)}, max: ${BenchmarkUtils.maxMs(avroWriteTimes)})"
      println "Avro read  avg: ${BenchmarkUtils.avgMs(avroReadTimes)} (min: ${BenchmarkUtils.minMs(avroReadTimes)}, max: ${BenchmarkUtils.maxMs(avroReadTimes)})"
      println "Parquet write avg: ${BenchmarkUtils.avgMs(parquetWriteTimes)} (min: ${BenchmarkUtils.minMs(parquetWriteTimes)}, max: ${BenchmarkUtils.maxMs(parquetWriteTimes)})"
      println "Parquet read  avg: ${BenchmarkUtils.avgMs(parquetReadTimes)} (min: ${BenchmarkUtils.minMs(parquetReadTimes)}, max: ${BenchmarkUtils.maxMs(parquetReadTimes)})"
      println "Avro file size: ${avroFile.length()} bytes"
      println "Parquet file size: ${parquetFile.length()} bytes"
    } finally {
      avroFile.delete()
      parquetFile.delete()
    }
  }
  private static Matrix buildMatrix(int rows) {
    Map<String, List<?>> cols = [:]
    cols['id'] = (1..rows).toList()
    cols['amount'] = (1..rows).collect { it / 10 }
    cols['name'] = (1..rows).collect { "row${it}" }
    cols['created'] = (1..rows).collect { LocalDate.of(BENCHMARK_YEAR, 1, 1).plusDays(it % 365) }
    cols['event'] = (1..rows).collect { LocalDateTime.of(BENCHMARK_YEAR, 1, 1, 12, 0).plusMinutes(it % 60) }
    return Matrix.builder('AvroParquetBenchmark')
        .columns(cols)
        .types(Integer, BigDecimal, String, LocalDate, LocalDateTime)
        .build()
  }

}
