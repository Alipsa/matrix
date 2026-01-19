package test.alipsa.matrix.gsheets

import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import org.junit.jupiter.api.Test
import org.mockito.Mockito

import static org.junit.jupiter.api.Assertions.*
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*
import static se.alipsa.matrix.gsheets.GsUtil.*

/**
 * Mocked tests for GsUtil that don't require actual Google Sheets API access.
 * These tests use Mockito to simulate Google Sheets API responses.
 */
class GsUtilMockedTest {

  @Test
  void testGetSheetNamesWithMockedService() {
    // Create mock Sheets service and related objects
    def sheetsService = mock(Sheets)
    def spreadsheets = mock(Sheets.Spreadsheets)
    def getRequest = mock(Sheets.Spreadsheets.Get)
    def spreadsheet = mock(Spreadsheet)

    // Create mock sheets with properties
    def sheet1 = new Sheet().setProperties(new SheetProperties().setTitle("Sheet1"))
    def sheet2 = new Sheet().setProperties(new SheetProperties().setTitle("Sheet2"))
    def sheet3 = new Sheet().setProperties(new SheetProperties().setTitle("Data"))

    // Setup mock behavior
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.get(anyString())).thenReturn(getRequest)
    when(getRequest.execute()).thenReturn(spreadsheet)
    when(spreadsheet.getSheets()).thenReturn([sheet1, sheet2, sheet3])

    // Test the method
    List<String> sheetNames = getSheetNames("test-spreadsheet-id", sheetsService)

    // Verify results
    assertNotNull(sheetNames)
    assertEquals(3, sheetNames.size())
    assertEquals("Sheet1", sheetNames[0])
    assertEquals("Sheet2", sheetNames[1])
    assertEquals("Data", sheetNames[2])

    // Verify interactions
    verify(sheetsService).spreadsheets()
    verify(spreadsheets).get("test-spreadsheet-id")
    verify(getRequest).execute()
  }

  @Test
  void testGetSheetNamesWithSingleSheet() {
    // Create mock objects
    def sheetsService = mock(Sheets)
    def spreadsheets = mock(Sheets.Spreadsheets)
    def getRequest = mock(Sheets.Spreadsheets.Get)
    def spreadsheet = mock(Spreadsheet)

    // Single sheet
    def sheet = new Sheet().setProperties(new SheetProperties().setTitle("Only Sheet"))

    // Setup mock behavior
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.get(anyString())).thenReturn(getRequest)
    when(getRequest.execute()).thenReturn(spreadsheet)
    when(spreadsheet.getSheets()).thenReturn([sheet])

    // Test the method
    List<String> sheetNames = getSheetNames("single-sheet-id", sheetsService)

    // Verify results
    assertNotNull(sheetNames)
    assertEquals(1, sheetNames.size())
    assertEquals("Only Sheet", sheetNames[0])
  }

  @Test
  void testGetSheetNamesWithSpecialCharacters() {
    // Test that sheet names with special characters are handled correctly
    def sheetsService = mock(Sheets)
    def spreadsheets = mock(Sheets.Spreadsheets)
    def getRequest = mock(Sheets.Spreadsheets.Get)
    def spreadsheet = mock(Spreadsheet)

    // Sheets with special characters in names
    def sheet1 = new Sheet().setProperties(new SheetProperties().setTitle("Data 2024"))
    def sheet2 = new Sheet().setProperties(new SheetProperties().setTitle("Q1-Results"))
    def sheet3 = new Sheet().setProperties(new SheetProperties().setTitle("Employee's Data"))

    // Setup mock behavior
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.get(anyString())).thenReturn(getRequest)
    when(getRequest.execute()).thenReturn(spreadsheet)
    when(spreadsheet.getSheets()).thenReturn([sheet1, sheet2, sheet3])

    // Test the method
    List<String> sheetNames = getSheetNames("test-id", sheetsService)

    // Verify results
    assertEquals(3, sheetNames.size())
    assertEquals("Data 2024", sheetNames[0])
    assertEquals("Q1-Results", sheetNames[1])
    assertEquals("Employee's Data", sheetNames[2])
  }

  @Test
  void testGetSheetNamesEmptySpreadsheet() {
    // Test handling of spreadsheet with no sheets (edge case)
    def sheetsService = mock(Sheets)
    def spreadsheets = mock(Sheets.Spreadsheets)
    def getRequest = mock(Sheets.Spreadsheets.Get)
    def spreadsheet = mock(Spreadsheet)

    // Empty list of sheets
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.get(anyString())).thenReturn(getRequest)
    when(getRequest.execute()).thenReturn(spreadsheet)
    when(spreadsheet.getSheets()).thenReturn([])

    // Test the method
    List<String> sheetNames = getSheetNames("empty-spreadsheet-id", sheetsService)

    // Verify results
    assertNotNull(sheetNames)
    assertEquals(0, sheetNames.size())
    assertTrue(sheetNames.isEmpty())
  }

  @Test
  void testGetSheetNamesPreservesOrder() {
    // Test that the order of sheets is preserved
    def sheetsService = mock(Sheets)
    def spreadsheets = mock(Sheets.Spreadsheets)
    def getRequest = mock(Sheets.Spreadsheets.Get)
    def spreadsheet = mock(Spreadsheet)

    // Create sheets in a specific order
    def sheets = []
    for (int i = 1; i <= 5; i++) {
      sheets.add(new Sheet().setProperties(new SheetProperties().setTitle("Sheet${i}")))
    }

    // Setup mock behavior
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.get(anyString())).thenReturn(getRequest)
    when(getRequest.execute()).thenReturn(spreadsheet)
    when(spreadsheet.getSheets()).thenReturn(sheets)

    // Test the method
    List<String> sheetNames = getSheetNames("test-id", sheetsService)

    // Verify order is preserved
    assertEquals(5, sheetNames.size())
    assertEquals("Sheet1", sheetNames[0])
    assertEquals("Sheet2", sheetNames[1])
    assertEquals("Sheet3", sheetNames[2])
    assertEquals("Sheet4", sheetNames[3])
    assertEquals("Sheet5", sheetNames[4])
  }

  @Test
  void testGetSheetNamesWithUnicodeCharacters() {
    // Test sheet names with Unicode characters
    def sheetsService = mock(Sheets)
    def spreadsheets = mock(Sheets.Spreadsheets)
    def getRequest = mock(Sheets.Spreadsheets.Get)
    def spreadsheet = mock(Spreadsheet)

    // Sheets with Unicode characters
    def sheet1 = new Sheet().setProperties(new SheetProperties().setTitle("日本語シート"))
    def sheet2 = new Sheet().setProperties(new SheetProperties().setTitle("Données françaises"))
    def sheet3 = new Sheet().setProperties(new SheetProperties().setTitle("Datos españoles"))

    // Setup mock behavior
    when(sheetsService.spreadsheets()).thenReturn(spreadsheets)
    when(spreadsheets.get(anyString())).thenReturn(getRequest)
    when(getRequest.execute()).thenReturn(spreadsheet)
    when(spreadsheet.getSheets()).thenReturn([sheet1, sheet2, sheet3])

    // Test the method
    List<String> sheetNames = getSheetNames("unicode-test-id", sheetsService)

    // Verify Unicode characters are preserved
    assertEquals(3, sheetNames.size())
    assertEquals("日本語シート", sheetNames[0])
    assertEquals("Données françaises", sheetNames[1])
    assertEquals("Datos españoles", sheetNames[2])
  }

  @Test
  void testDeleteSheetSuccess() {
    // Create mock Drive service and related objects
    def driveService = mock(Drive)
    def files = mock(Drive.Files)
    def deleteRequest = mock(Drive.Files.Delete)

    // Setup mock behavior - execute() is void, so use doNothing()
    when(driveService.files()).thenReturn(files)
    when(files.delete(anyString())).thenReturn(deleteRequest)
    doNothing().when(deleteRequest).execute()

    // Test the method - should not throw exception
    assertDoesNotThrow({ deleteSheet("test-spreadsheet-id", driveService) } as org.junit.jupiter.api.function.Executable)

    // Verify interactions
    verify(driveService).files()
    verify(files).delete("test-spreadsheet-id")
    verify(deleteRequest).execute()
  }

  @Test
  void testDeleteSheetFailure() {
    // Create mock Drive service that throws IOException
    def driveService = mock(Drive)
    def files = mock(Drive.Files)
    def deleteRequest = mock(Drive.Files.Delete)

    // Setup mock to throw IOException
    when(driveService.files()).thenReturn(files)
    when(files.delete(anyString())).thenReturn(deleteRequest)
    when(deleteRequest.execute()).thenThrow(new IOException("File not found"))

    // Test the method - should throw SheetOperationException
    def exception = assertThrows(se.alipsa.matrix.gsheets.SheetOperationException, () ->
      deleteSheet("non-existent-id", driveService)
    )

    // Verify exception details
    assertEquals("delete", exception.operation)
    assertEquals("non-existent-id", exception.spreadsheetId)

    // Verify interactions
    verify(driveService).files()
    verify(files).delete("non-existent-id")
    verify(deleteRequest).execute()
  }

  @Test
  void testDeleteSheetWithNullDriveService() {
    assertThrows(IllegalArgumentException, () -> deleteSheet("some-id", (Drive)null))
  }

  @Test
  void testDeleteSheetWithNullSpreadsheetIdAndDriveService() {
    def driveService = mock(Drive)
    assertThrows(IllegalArgumentException, () -> deleteSheet(null, driveService))
  }

  @Test
  void testDeleteSheetWithEmptySpreadsheetIdAndDriveService() {
    def driveService = mock(Drive)
    assertThrows(IllegalArgumentException, () -> deleteSheet("", driveService))
    assertThrows(IllegalArgumentException, () -> deleteSheet("  ", driveService))
  }
}
