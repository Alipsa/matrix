package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsheetsReader

/**
 * Tests for GsheetsReader class.
 *
 * These tests verify input validation and mocked read behavior.
 * External integration tests cover reads against the Google Sheets API.
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

  @Test
  void testReadAsObjectHandlesEmptyRange() {
    Matrix matrix = GsheetsReader.readAsObjectWithService(
        'some-id',
        'Sheet1!A1:D10',
        true,
        sheetsServiceReturning(new ValueRange(), 'UNFORMATTED_VALUE')
    )

    assertEquals(0, matrix.rowCount())
    assertEquals(4, matrix.columnCount())
    assertEquals(['c1', 'c2', 'c3', 'c4'], matrix.columnNames())
  }

  @Test
  void testReadAsStringsHandlesEmptySingleCellRange() {
    Matrix matrix = GsheetsReader.readAsStringsWithService(
        'some-id',
        'Sheet1!A1',
        false,
        sheetsServiceReturning(new ValueRange(), 'FORMATTED_VALUE')
    )

    assertEquals(0, matrix.rowCount())
    assertEquals(1, matrix.columnCount())
    assertEquals(['c1'], matrix.columnNames())
  }

  private static Sheets sheetsServiceReturning(ValueRange response, String renderOption) {
    def sheetsService = mock(Sheets)
    def spreadsheets = mock(Sheets.Spreadsheets)
    def values = mock(Sheets.Spreadsheets.Values)
    def getRequest = mock(Sheets.Spreadsheets.Values.Get)

    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.values()).thenReturn(values)
    when(values.get(anyString(), anyString())).thenReturn(getRequest)
    when(getRequest.setValueRenderOption(renderOption)).thenReturn(getRequest)
    when(getRequest.execute()).thenReturn(response)

    sheetsService
  }

}
