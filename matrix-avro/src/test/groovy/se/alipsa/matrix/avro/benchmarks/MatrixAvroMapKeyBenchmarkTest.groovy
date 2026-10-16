package se.alipsa.matrix.avro.benchmarks

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class MatrixAvroMapKeyBenchmarkTest {

  @Test
  void rejectsZeroAndNegativeRunsBeforeBuildingWorkloads() {
    [-1, 0].each { int runs ->
      def error = assertThrows(IllegalArgumentException) {
        MatrixAvroMapKeyBenchmark.main(['100', '0', String.valueOf(runs)] as String[])
      }
      assertEquals('runs must be at least 1', error.message)
    }
  }
}
