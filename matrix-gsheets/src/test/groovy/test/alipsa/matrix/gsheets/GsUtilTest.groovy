package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import static se.alipsa.matrix.gsheets.GsUtil.*

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsConverter
import se.alipsa.matrix.gsheets.GsExporter
import se.alipsa.matrix.gsheets.GsUtil

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GsUtilTest {

  @Test
  void testColumnCountForRange() {
    def range = 'Arkiv!B2:H100'
    def columns = columnCountForRange(range)
    //println "The number of columns in the range is: ${columns}"
    assertEquals(7, columns)

    // Example with a single-letter range
    def simpleRange = 'C:F'
    def simpleColumns = columnCountForRange(simpleRange)
    //println "The number of columns in the simple range is: ${simpleColumns}"
    assertEquals(4, simpleColumns)

    // Example with a multi-letter range
    def multiRange = 'A1:AB10'
    def multiColumns = columnCountForRange(multiRange)
    //println "The number of columns in the multi-letter range is: ${multiColumns}"
    assertEquals(28, multiColumns)

    range = "'Amorteringsfrihet '!A1:L16"
    columns = columnCountForRange(range)
    //println "The number of columns in the range is: ${columns}"
    assertEquals(12, columns)

    assertEquals(1, columnCountForRange('A1'))
    assertEquals(1, columnCountForRange('Sheet1!A1'))
  }

  @Test
  @Tag('external')
  void testGetSheetNames() {
    // Create a test spreadsheet with multiple sheets
    def empData = Matrix.builder()
        .matrixName('Employee Data')
        .data(
            emp_id: 1..3,
            emp_name: ['Alice', 'Bob', 'Charlie'],
            salary: [50000, 60000, 70000],
            start_date: toLocalDates('2020-01-01', '2021-02-15', '2022-03-20')
        )
        .types([Integer, String, BigDecimal, LocalDate])
        .build()

    // Export creates spreadsheet with sheet named after matrix
    String spreadsheetId = GsExporter.exportSheet(empData)

    try {
      // Get sheet names from the spreadsheet
      List<String> sheetNames = getSheetNames(spreadsheetId)

      // Verify the method returns a list with at least one sheet
      assertNotNull(sheetNames, 'Sheet names list should not be null')
      assertFalse(sheetNames.isEmpty(), 'Sheet names list should not be empty')

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
  @Tag('external')
  void testGetSheetNamesWithMultipleSheets() {
    // This test verifies getSheetNames works with spreadsheets containing multiple sheets
    // Note: Currently GsExporter creates one sheet per export
    // This test creates a spreadsheet and verifies at least one sheet is returned

    def testData = Matrix.builder()
        .matrixName('Test Sheet')
        .data(
            id: [1, 2, 3],
            value: ['A', 'B', 'C']
        )
        .types([Integer, String])
        .build()

    String spreadsheetId = GsExporter.exportSheet(testData)

    try {
      List<String> sheetNames = getSheetNames(spreadsheetId)

      // Verify basic functionality
      assertNotNull(sheetNames)
      assertTrue(sheetNames.size() >= 1, 'Should have at least one sheet')
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
    // Missing row number
    assertThrows(IllegalArgumentException, () -> columnCountForRange('A'))
    assertThrows(IllegalArgumentException, () -> columnCountForRange('Sheet1!A'))

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
  void testSanitizeSheetNameRemovesInvalidCharacters() {
    // Google Sheets doesn't allow: : \ / ? * [ ]
    assertEquals('My Sheet Name', GsUtil.sanitizeSheetName('My:Sheet\\Name'))
    assertEquals('Data Report', GsUtil.sanitizeSheetName('Data/Report'))
    assertEquals('Query Results', GsUtil.sanitizeSheetName('Query?Results'))
    assertEquals('Array   Data', GsUtil.sanitizeSheetName('Array[*]Data'))  // [*] becomes 2 spaces
  }

  @Test
  void testSanitizeSheetNameTruncatesLongNames() {
    // Names longer than 100 characters should be truncated
    String longName = 'A' * 150
    String result = GsUtil.sanitizeSheetName(longName)
    assertEquals(100, result.length())
  }

  @Test
  void testSanitizeSheetNameHandlesEmptyString() {
    assertEquals('Sheet1', GsUtil.sanitizeSheetName(''))
    assertEquals('Sheet1', GsUtil.sanitizeSheetName('   '))
  }

  @Test
  void testSanitizeSheetNameHandlesOnlyInvalidCharacters() {
    // If the result would be empty after removing invalid chars, use 'Sheet1'
    assertEquals('Sheet1', GsUtil.sanitizeSheetName(':\\/?*[]'))
    assertEquals('Sheet1', GsUtil.sanitizeSheetName('///'))
  }

  @Test
  void testSanitizeSheetNamePreservesValidCharacters() {
    assertEquals('Employee Data 2024', GsUtil.sanitizeSheetName('Employee Data 2024'))
    assertEquals('Sales_Q1', GsUtil.sanitizeSheetName('Sales_Q1'))
    assertEquals('Report-Final', GsUtil.sanitizeSheetName('Report-Final'))
  }

  @Test
  void testQuoteSheetNamePlainName() {
    assertEquals("'Sheet1'", GsUtil.quoteSheetName('Sheet1'))
  }

  @Test
  void testQuoteSheetNameWithSpace() {
    assertEquals("'Employee Data'", GsUtil.quoteSheetName('Employee Data'))
  }

  @Test
  void testQuoteSheetNameWithEmbeddedQuote() {
    // Google requires embedded single quotes to be doubled
    assertEquals("'It''s a sheet'", GsUtil.quoteSheetName("It's a sheet"))
  }

  @Test
  void testToCellWithNull() {
    // Test null handling with convertNullsToEmptyString
    assertEquals('', GsUtil.toCell(null, true, false))
    assertNull(GsUtil.toCell(null, false, false))
  }

  @Test
  void testToCellWithNumbers() {
    assertEquals(42, GsUtil.toCell(42, true, false))
    assertEquals(3.14, GsUtil.toCell(3.14, true, false))
    assertEquals(123.45, GsUtil.toCell(123.45, true, false))
  }

  @Test
  void testToCellWithHighPrecisionBigDecimalThrows() {
    // Fractional values with 16 significant digits exceed the conservative precision guard.
    BigDecimal tooPrecise = new BigDecimal('123456789012345.6')
    assertThrows(IllegalArgumentException, () -> GsUtil.toCell(tooPrecise, true, false))
  }

  @Test
  void testToCellWithCompactLargeIntegerBigDecimalThrows() {
    // Precision alone is not enough: exponent notation can keep significant digits low
    // while the integer value is still outside double's exact integer range.
    BigDecimal tooLarge = new BigDecimal('999999999999999E10')
    assertEquals(15, tooLarge.precision())
    assertThrows(IllegalArgumentException, () -> GsUtil.toCell(tooLarge, true, false))
  }

  @Test
  void testToCellWithBoundaryPrecisionBigDecimalPasses() {
    // Exactly 15 significant digits is still safe
    BigDecimal boundary = new BigDecimal('123456789012345')
    assertEquals(boundary, GsUtil.toCell(boundary, true, false))
  }

  @Test
  void testToCellWithExactMaxDoubleIntegerBigDecimalPasses() {
    BigDecimal maxExactInteger = new BigDecimal('9007199254740992')
    assertEquals(maxExactInteger, GsUtil.toCell(maxExactInteger, true, false))
  }

  @Test
  void testToCellWithFirstUnsafeDoubleIntegerBigDecimalThrows() {
    BigDecimal firstUnsafeInteger = new BigDecimal('9007199254740993')
    assertThrows(IllegalArgumentException, () -> GsUtil.toCell(firstUnsafeInteger, true, false))
  }

  @Test
  void testToCellWithBoolean() {
    assertEquals(true, GsUtil.toCell(true, true, false))
    assertEquals(false, GsUtil.toCell(false, true, false))
  }

  @Test
  void testToCellWithString() {
    assertEquals('Hello, World!', GsUtil.toCell('Hello, World!', true, false))
    assertEquals('Test', GsUtil.toCell('Test', false, false))
  }

  @Test
  void testToCellWithLocalDateAsString() {
    def date = LocalDate.parse('2024-06-24')
    // Without date conversion, should return ISO string
    assertEquals('2024-06-24', GsUtil.toCell(date, true, false))
  }

  @Test
  void testToCellWithLocalDateAsSerial() {
    def date = LocalDate.parse('2024-06-24')
    // With date conversion, should return serial number
    def result = GsUtil.toCell(date, true, true)
    assertEquals(GsConverter.asSerial(date), result)
  }

  @Test
  void testToCellWithLocalDateTimeAsString() {
    def dateTime = LocalDateTime.parse('2022-01-14T12:33:20')
    // Without date conversion, should return ISO string
    assertEquals('2022-01-14T12:33:20', GsUtil.toCell(dateTime, true, false))
  }

  @Test
  void testToCellWithLocalDateTimeAsSerial() {
    def dateTime = LocalDateTime.parse('2022-01-14T12:33:20')
    // With date conversion, should return serial number
    def result = GsUtil.toCell(dateTime, true, true)
    assertEquals(GsConverter.asSerial(dateTime), result)
  }

  @Test
  void testToCellWithLocalTimeAsString() {
    def time = LocalTime.parse('18:25:44')
    // Without date conversion, should return ISO string
    assertEquals('18:25:44', GsUtil.toCell(time, true, false))
  }

  @Test
  void testToCellWithLocalTimeAsSerial() {
    def time = LocalTime.parse('18:25:44')
    // With date conversion, should return serial number
    def result = GsUtil.toCell(time, true, true)
    assertEquals(GsConverter.asSerial(time), result)
  }

  @Test
  void testToCellWithDateAsSerial() {
    def date = Date.from(LocalDateTime.parse('2022-01-14T12:33:20').atZone(java.time.ZoneId.systemDefault()).toInstant())
    // With date conversion, should return serial number
    def result = GsUtil.toCell(date, true, true)
    assertNotNull(result)
    assertTrue(result instanceof BigDecimal)
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
