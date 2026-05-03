package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsheetsWriter

/**
 * Tests for GsheetsWriter class.
 *
 * These tests verify input validation and method existence.
 * Actual write operations are tested via external/integration tests.
 */
class GsheetsWriterTest {

  @Test
  void testWriteNullMatrixThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.write(null)
    }, 'Should throw on null matrix')
  }

  @Test
  void testWriteEmptyMatrixThrows() {
    Matrix emptyColumns = Matrix.builder().build()
    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.write(emptyColumns)
    }, 'Should throw on matrix with no columns')
  }

  @Test
  void testWriteMatrixWithNoRowsThrows() {
    Matrix noRows = Matrix.builder()
        .data(id: [], name: [])
        .build()

    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.write(noRows)
    }, 'Should throw on matrix with no rows')
  }

}
