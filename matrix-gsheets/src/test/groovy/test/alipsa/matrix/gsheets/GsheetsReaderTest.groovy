package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.gsheets.GsheetsReader

/**
 * Tests for GsheetsReader class.
 *
 * These tests verify input validation and method existence.
 * Actual read operations are tested via external/integration tests.
 */
class GsheetsReaderTest {

  @Test
  void testReadInvalidSpreadsheetIdThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.read(null, 'Sheet1!A1:D10', true)
    }, 'Should throw on null spreadsheet ID')

    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.read('', 'Sheet1!A1:D10', true)
    }, 'Should throw on empty spreadsheet ID')
  }

  @Test
  void testReadInvalidRangeThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.read('some-id', null, true)
    }, 'Should throw on null range')

    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.read('some-id', '', true)
    }, 'Should throw on empty range')
  }

  @Test
  void testReadAsObjectInvalidSpreadsheetIdThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.readAsObject(null, 'Sheet1!A1:D10', true)
    }, 'Should throw on null spreadsheet ID')
  }

  @Test
  void testReadAsObjectInvalidRangeThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.readAsObject('some-id', null, true)
    }, 'Should throw on null range')
  }

  @Test
  void testReadAsStringsInvalidSpreadsheetIdThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.readAsStrings(null, 'Sheet1!A1:D10', true)
    }, 'Should throw on null spreadsheet ID')
  }

  @Test
  void testReadAsStringsInvalidRangeThrows() {
    assertThrows(IllegalArgumentException, () -> {
      GsheetsReader.readAsStrings('some-id', null, true)
    }, 'Should throw on null range')
  }

}
