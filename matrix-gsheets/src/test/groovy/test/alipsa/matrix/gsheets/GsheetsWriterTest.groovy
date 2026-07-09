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

  @Test
  void testWriteInvalidBigDecimalThrowsBeforeCreatingSpreadsheet() {
    Matrix data = Matrix.builder('Invalid Precision')
        .data(id: [1], amount: [new BigDecimal('999999999999999E10')])
        .build()

    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.write(data)
    }, 'Should validate cell values before creating a spreadsheet')
  }

  @Test
  void testUpdateNullSpreadsheetIdThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.update(null, 'Sheet1!A1', Matrix.builder().data(id: [1]).build())
    }, 'Should throw on null spreadsheetId')
  }

  @Test
  void testUpdateNullRangeThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.update('some-id', null, Matrix.builder().data(id: [1]).build())
    }, 'Should throw on null range')
  }

  @Test
  void testUpdateNullMatrixThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.update('some-id', 'Sheet1!A1', null)
    }, 'Should throw on null matrix')
  }

  @Test
  void testUpdateEmptyMatrixThrows() {
    Matrix emptyColumns = Matrix.builder().build()
    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.update('some-id', 'Sheet1!A1', emptyColumns)
    }, 'Should throw on matrix with no columns')
  }

  @Test
  void testUpdateMatrixWithNoRowsThrows() {
    Matrix noRows = Matrix.builder()
        .data(id: [], name: [])
        .build()

    assertThrows(IllegalArgumentException, () -> {
      GsheetsWriter.update('some-id', 'Sheet1!A1', noRows)
    }, 'Should throw on matrix with no rows')
  }

}
