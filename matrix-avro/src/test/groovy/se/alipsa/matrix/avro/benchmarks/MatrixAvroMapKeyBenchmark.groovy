package se.alipsa.matrix.avro.benchmarks

import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

/**
 * Compares ordinary maps, sparse records, scalar arrays, and arrays of records.
 * Run avroMapKeyBenchmark with -Prows (default 30000), -Pwarmups (default 5),
 * and -Pruns (default 7). Timings have no pass/fail thresholds.
 */
// Two is used independently for argument indexing, median selection, and alternating map shapes.
@SuppressWarnings('DuplicateNumberLiteral')
class MatrixAvroMapKeyBenchmark {

  private static final String MAP_WORKLOAD = 'map'
  private static final String SPARSE_WORKLOAD = 'sparse'
  private static final String SCALAR_LIST_WORKLOAD = 'scalarList'
  private static final String NESTED_LIST_WORKLOAD = 'nestedList'
  private static final int DEFAULT_ROWS = 30_000
  private static final int DEFAULT_WARMUPS = 5
  private static final int DEFAULT_RUNS = 7
  private static final int RUNS_ARG_INDEX = 2
  private static final int OPTIONAL_FIELDS = 30
  private static final int LIST_ELEMENTS = 16

  /**
   * Runs in-memory write benchmarks with the selected row, warmup, and run counts.
   * @param args optional rows, warmups, and runs; defaults are 30000, 5, and 7
   * @throws IllegalArgumentException if runs is less than 1
   */
  static void main(String[] args) {
    int rows = args.size() > 0 ? args[0] as int : DEFAULT_ROWS
    int warmups = args.size() > 1 ? args[1] as int : DEFAULT_WARMUPS
    int runs = args.size() > RUNS_ARG_INDEX ? args[RUNS_ARG_INDEX] as int : DEFAULT_RUNS
    if (runs < 1) {
      throw new IllegalArgumentException('runs must be at least 1')
    }
    ['record', MAP_WORKLOAD, SPARSE_WORKLOAD, SCALAR_LIST_WORKLOAD, NESTED_LIST_WORKLOAD].each { String kind ->
      Matrix matrix = buildMatrix(kind, rows)
      AvroWriteOptions options = optionsFor(kind)
      warmups.times { MatrixAvroWriter.writeBytes(matrix, options) }
      List<Long> times = (0..<runs).collect {
        BenchmarkUtils.timeNs { MatrixAvroWriter.writeBytes(matrix, options) }
      }
      long median = times.sort()[runs.intdiv(2)]
      println "$kind rows=$rows warmups=$warmups runs=$runs median=${BenchmarkUtils.avgMs([median])} ms " +
          "avg=${BenchmarkUtils.avgMs(times)} min=${BenchmarkUtils.minMs(times)} max=${BenchmarkUtils.maxMs(times)}"
    }
  }

  private static Matrix buildMatrix(String kind, int rows) {
    List values = (0..<rows).collect { int i ->
      switch (kind) {
        case MAP_WORKLOAD -> i % 2 == 0 ? [a: i, b: i] : [a: i, c: i]
        case SCALAR_LIST_WORKLOAD -> (0..<LIST_ELEMENTS).collect { i + it }
        case NESTED_LIST_WORKLOAD -> [[a: i, b: i], [a: i + 1, b: i + 1]]
        default -> [a: i, b: i]
      }
    }
    Matrix.builder('Keys').columns([value: values])
        .types(kind in [SCALAR_LIST_WORKLOAD, NESTED_LIST_WORKLOAD] ? List : Map).build()
  }

  private static AvroWriteOptions optionsFor(String kind) {
    Map<String, AvroSchemaDecl> fields = [a: AvroSchemaDecl.type(Integer), b: AvroSchemaDecl.type(Integer)]
    if (kind == SPARSE_WORKLOAD) {
      OPTIONAL_FIELDS.times { fields['optional' + it] = AvroSchemaDecl.type(Integer) }
      return new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.record(fields)])
    }
    if (kind == NESTED_LIST_WORKLOAD) {
      return new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.array(AvroSchemaDecl.record(fields))])
    }
    AvroWriteOptions.defaults()
  }
}
