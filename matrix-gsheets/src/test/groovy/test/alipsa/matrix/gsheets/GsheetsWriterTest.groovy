package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.UpdateValuesResponse
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor

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

  @Test
  void testUpdateWithServicePreflightsFormattedOpenEndedRangeBeforeWriting() {
    Sheets sheetsService = mock(Sheets)
    Matrix matrix = Matrix.builder().data(amount: [1.50]).build()

    assertThrows(IllegalArgumentException,
        () -> GsheetsWriter.updateWithService('some-id', 'Sheet1!A:D', matrix, sheetsService))
    verifyNoInteractions(sheetsService)
  }

  @Test
  void testUpdateWithServiceWritesPlainMatrixWithoutFormatting() {
    Sheets sheetsService = mock(Sheets)
    Sheets.Spreadsheets spreadsheets = mock(Sheets.Spreadsheets)
    Sheets.Spreadsheets.Values values = mock(Sheets.Spreadsheets.Values)
    Sheets.Spreadsheets.Values.Update update = mock(Sheets.Spreadsheets.Values.Update)
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.values()).thenReturn(values)
    when(values.update(eq('some-id'), eq('Sheet1!B2'), any())).thenReturn(update)
    when(update.setValueInputOption('RAW')).thenReturn(update)
    when(update.execute()).thenReturn(new UpdateValuesResponse())
    Matrix matrix = Matrix.builder().data(id: [1]).build()

    assertEquals('some-id', GsheetsWriter.updateWithService('some-id', 'Sheet1!B2', matrix, sheetsService))
    verify(values).update(eq('some-id'), eq('Sheet1!B2'), any())
    verify(spreadsheets, never()).batchUpdate(anyString(), any())
  }

  @Test
  void testUpdateWithServiceAcceptsRowOnlyRange() {
    Sheets sheetsService = mock(Sheets)
    Sheets.Spreadsheets spreadsheets = mock(Sheets.Spreadsheets)
    Sheets.Spreadsheets.Values values = mock(Sheets.Spreadsheets.Values)
    Sheets.Spreadsheets.Values.Update update = mock(Sheets.Spreadsheets.Values.Update)
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.values()).thenReturn(values)
    when(values.update(eq('some-id'), eq('Sheet1!2:3'), any())).thenReturn(update)
    when(update.setValueInputOption('RAW')).thenReturn(update)
    when(update.execute()).thenReturn(new UpdateValuesResponse())
    Matrix matrix = Matrix.builder().data(id: [1]).build()

    assertEquals('some-id', GsheetsWriter.updateWithService('some-id', 'Sheet1!2:3', matrix, sheetsService))
    verify(values).update(eq('some-id'), eq('Sheet1!2:3'), any())
  }

  @Test
  void testUpdateWithServiceRejectsRowOnlyRangeForFormattedDecimalWrite() {
    Sheets sheetsService = mock(Sheets)
    Matrix matrix = Matrix.builder().data(amount: [1.50]).build()

    assertThrows(IllegalArgumentException,
        () -> GsheetsWriter.updateWithService('some-id', 'Sheet1!2:3', matrix, sheetsService))
    verifyNoInteractions(sheetsService)
  }

  @Test
  void testUpdateWithServiceFormatsLowercaseStartingCell() {
    Sheets sheetsService = mock(Sheets)
    Sheets.Spreadsheets spreadsheets = mock(Sheets.Spreadsheets)
    Sheets.Spreadsheets.Values values = mock(Sheets.Spreadsheets.Values)
    Sheets.Spreadsheets.Values.Update update = mock(Sheets.Spreadsheets.Values.Update)
    Sheets.Spreadsheets.Get get = mock(Sheets.Spreadsheets.Get)
    Sheets.Spreadsheets.BatchUpdate batchUpdate = mock(Sheets.Spreadsheets.BatchUpdate)
    ArgumentCaptor<BatchUpdateSpreadsheetRequest> requestCaptor = ArgumentCaptor.forClass(BatchUpdateSpreadsheetRequest)
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.values()).thenReturn(values)
    when(values.update(eq('some-id'), eq('Sheet1!b2'), any())).thenReturn(update)
    when(update.setValueInputOption('RAW')).thenReturn(update)
    when(update.execute()).thenReturn(new UpdateValuesResponse())
    when(spreadsheets.get('some-id')).thenReturn(get)
    when(get.setFields('sheets.properties')).thenReturn(get)
    when(get.execute()).thenReturn(new Spreadsheet().setSheets([
        new Sheet().setProperties(new SheetProperties().setTitle('Sheet1').setSheetId(7))
    ]))
    when(spreadsheets.batchUpdate(eq('some-id'), requestCaptor.capture())).thenReturn(batchUpdate)
    when(batchUpdate.execute()).thenReturn(new BatchUpdateSpreadsheetResponse())
    Matrix matrix = Matrix.builder().data(amount: [1.50]).build()

    GsheetsWriter.updateWithService('some-id', 'Sheet1!b2', matrix, sheetsService)

    def range = requestCaptor.value.requests[0].repeatCell.range
    assertEquals(7, range.sheetId)
    assertEquals(2, range.startRowIndex)
    assertEquals(3, range.endRowIndex)
    assertEquals(1, range.startColumnIndex)
    assertEquals(2, range.endColumnIndex)
    assertEquals('NUMBER', requestCaptor.value.requests[0].repeatCell.cell.userEnteredFormat.numberFormat.type)
    assertEquals('0.00', requestCaptor.value.requests[0].repeatCell.cell.userEnteredFormat.numberFormat.pattern)
    verify(update).execute()
    verify(get).execute()
    verify(batchUpdate).execute()
  }

  @Test
  void testExtractSheetNameUnescapesQuotedBangCharacters() {
    assertEquals("It's!here", GsheetsWriter.extractSheetName("'It''s!here'!A1"))
  }

}
