package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsExporter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static se.alipsa.matrix.gsheets.GsUtil.*
import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.toLocalDates

class GsUtilTest {

  @Test
  void testColumnCountForRange() {
    def range = "Arkiv!B2:H100"
    def columns = columnCountForRange(range)
    //println "The number of columns in the range is: ${columns}"
    assertEquals(7, columns)

    // Example with a single-letter range
    def simpleRange = "C:F"
    def simpleColumns = columnCountForRange(simpleRange)
    //println "The number of columns in the simple range is: ${simpleColumns}"
    assertEquals(4, simpleColumns)

    // Example with a multi-letter range
    def multiRange = "A1:AB10"
    def multiColumns = columnCountForRange(multiRange)
    //println "The number of columns in the multi-letter range is: ${multiColumns}"
    assertEquals(28, multiColumns)

    range = "'Amorteringsfrihet '!A1:L16"
    columns = columnCountForRange(range)
    //println "The number of columns in the range is: ${columns}"
    assertEquals(12, columns)
  }

  @Test
  @Tag("external")
  void testGetSheetNames() {
    // Create a test spreadsheet with multiple sheets
    def empData = Matrix.builder()
        .matrixName('Employee Data')
        .data(
            emp_id: 1..3,
            emp_name: ["Alice", "Bob", "Charlie"],
            salary: [50000, 60000, 70000],
            start_date: toLocalDates("2020-01-01", "2021-02-15", "2022-03-20")
        )
        .types([Integer, String, BigDecimal, LocalDate])
        .build()

    // Export creates spreadsheet with sheet named after matrix
    String spreadsheetId = GsExporter.exportSheet(empData)

    try {
      // Get sheet names from the spreadsheet
      List<String> sheetNames = getSheetNames(spreadsheetId)

      // Verify the method returns a list with at least one sheet
      assertNotNull(sheetNames, "Sheet names list should not be null")
      assertFalse(sheetNames.isEmpty(), "Sheet names list should not be empty")

      // Verify the first sheet name matches the exported data
      assertTrue(sheetNames.contains('Employee Data'),
          "Sheet names should contain 'Employee Data', got: ${sheetNames}")

      //println "Retrieved sheet names: ${sheetNames}"
    } finally {
      // Clean up
      deleteSheet(spreadsheetId)
    }
  }

  @Test
  @Tag("external")
  void testGetSheetNamesWithMultipleSheets() {
    // This test verifies getSheetNames works with spreadsheets containing multiple sheets
    // Note: Currently GsExporter creates one sheet per export
    // This test creates a spreadsheet and verifies at least one sheet is returned

    def testData = Matrix.builder()
        .matrixName('Test Sheet')
        .data(
            id: [1, 2, 3],
            value: ["A", "B", "C"]
        )
        .types([Integer, String])
        .build()

    String spreadsheetId = GsExporter.exportSheet(testData)

    try {
      List<String> sheetNames = getSheetNames(spreadsheetId)

      // Verify basic functionality
      assertNotNull(sheetNames)
      assertTrue(sheetNames.size() >= 1, "Should have at least one sheet")
      assertTrue(sheetNames.contains('Test Sheet'))

      //println "Spreadsheet contains sheets: ${sheetNames.join(', ')}"
    } finally {
      deleteSheet(spreadsheetId)
    }
  }

  // Error case tests
  @Test
  void testColumnCountForRangeWithNull() {
    assertThrows(IllegalArgumentException, () -> columnCountForRange(null))
  }

  @Test
  void testColumnCountForRangeWithEmpty() {
    assertThrows(IllegalArgumentException, () -> columnCountForRange(''))
    assertThrows(IllegalArgumentException, () -> columnCountForRange('  '))
  }

  @Test
  void testColumnCountForRangeWithInvalidFormat() {
    // Missing colon
    assertThrows(IllegalArgumentException, () -> columnCountForRange('A1'))
    assertThrows(IllegalArgumentException, () -> columnCountForRange('Sheet1!A1'))

    // Invalid format - too many colons
    assertThrows(IllegalArgumentException, () -> columnCountForRange('A1:B2:C3'))
  }

  @Test
  void testAsColumnNumberWithNull() {
    assertThrows(IllegalArgumentException, () -> asColumnNumber(null))
  }

  @Test
  void testAsColumnNumberWithEmpty() {
    assertThrows(IllegalArgumentException, () -> asColumnNumber(''))
    assertThrows(IllegalArgumentException, () -> asColumnNumber('  '))
  }

  @Test
  void testAsColumnNumberWithInvalidCharacters() {
    assertThrows(IllegalArgumentException, () -> asColumnNumber('A1'))
    assertThrows(IllegalArgumentException, () -> asColumnNumber('123'))
    assertThrows(IllegalArgumentException, () -> asColumnNumber('A-B'))
    assertThrows(IllegalArgumentException, () -> asColumnNumber('A$B'))
  }

  @Test
  void testDeleteSheetWithNull() {
    assertThrows(IllegalArgumentException, () -> deleteSheet(null))
  }

  @Test
  void testDeleteSheetWithEmpty() {
    assertThrows(IllegalArgumentException, () -> deleteSheet(''))
    assertThrows(IllegalArgumentException, () -> deleteSheet('  '))
  }

  @Test
  void testGetSheetNamesWithNull() {
    assertThrows(IllegalArgumentException, () -> getSheetNames(null))
  }

  @Test
  void testGetSheetNamesWithEmpty() {
    assertThrows(IllegalArgumentException, () -> getSheetNames(''))
    assertThrows(IllegalArgumentException, () -> getSheetNames('  '))
  }

  @Test
  void testAsColumnNumberValidInputs() {
    // Test valid column names
    assertEquals(1, asColumnNumber('A'))
    assertEquals(26, asColumnNumber('Z'))
    assertEquals(27, asColumnNumber('AA'))
    assertEquals(702, asColumnNumber('ZZ'))
    assertEquals(703, asColumnNumber('AAA'))

    // Test lowercase is converted to uppercase
    assertEquals(1, asColumnNumber('a'))
    assertEquals(26, asColumnNumber('z'))
    assertEquals(27, asColumnNumber('aa'))
  }

}
