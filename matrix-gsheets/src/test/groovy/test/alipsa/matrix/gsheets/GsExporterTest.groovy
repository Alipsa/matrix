package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsConverter
import se.alipsa.matrix.gsheets.GsExporter
import se.alipsa.matrix.gsheets.GsUtil

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for GsExporter utility methods.
 */
class GsExporterTest {

  @Test
  void testSanitizeSheetNameRemovesInvalidCharacters() {
    // Google Sheets doesn't allow: : \ / ? * [ ]
    assertEquals("My Sheet Name", GsUtil.sanitizeSheetName("My:Sheet\\Name"))
    assertEquals("Data Report", GsUtil.sanitizeSheetName("Data/Report"))
    assertEquals("Query Results", GsUtil.sanitizeSheetName("Query?Results"))
    assertEquals("Array   Data", GsUtil.sanitizeSheetName("Array[*]Data"))  // [*] becomes 2 spaces
  }

  @Test
  void testSanitizeSheetNameTruncatesLongNames() {
    // Names longer than 100 characters should be truncated
    String longName = "A" * 150
    String result = GsUtil.sanitizeSheetName(longName)
    assertEquals(100, result.length())
  }

  @Test
  void testSanitizeSheetNameHandlesEmptyString() {
    assertEquals("Sheet1", GsUtil.sanitizeSheetName(""))
    assertEquals("Sheet1", GsUtil.sanitizeSheetName("   "))
  }

  @Test
  void testSanitizeSheetNameHandlesOnlyInvalidCharacters() {
    // If the result would be empty after removing invalid chars, use "Sheet1"
    assertEquals("Sheet1", GsUtil.sanitizeSheetName(":\\/?*[]"))
    assertEquals("Sheet1", GsUtil.sanitizeSheetName("///"))
  }

  @Test
  void testSanitizeSheetNamePreservesValidCharacters() {
    assertEquals("Employee Data 2024", GsUtil.sanitizeSheetName("Employee Data 2024"))
    assertEquals("Sales_Q1", GsUtil.sanitizeSheetName("Sales_Q1"))
    assertEquals("Report-Final", GsUtil.sanitizeSheetName("Report-Final"))
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
    assertEquals(new BigDecimal("123.45"), GsUtil.toCell(new BigDecimal("123.45"), true, false))
  }

  @Test
  void testToCellWithBoolean() {
    assertEquals(true, GsUtil.toCell(true, true, false))
    assertEquals(false, GsUtil.toCell(false, true, false))
  }

  @Test
  void testToCellWithString() {
    assertEquals("Hello, World!", GsUtil.toCell("Hello, World!", true, false))
    assertEquals("Test", GsUtil.toCell("Test", false, false))
  }

  @Test
  void testToCellWithLocalDateAsString() {
    def date = LocalDate.parse('2024-06-24')
    // Without date conversion, should return ISO string
    assertEquals("2024-06-24", GsUtil.toCell(date, true, false))
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
    assertEquals("2022-01-14T12:33:20", GsUtil.toCell(dateTime, true, false))
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
    def time = LocalTime.parse("18:25:44")
    // Without date conversion, should return ISO string
    assertEquals("18:25:44", GsUtil.toCell(time, true, false))
  }

  @Test
  void testToCellWithLocalTimeAsSerial() {
    def time = LocalTime.parse("18:25:44")
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
  void testExportSheetWithNullMatrix() {
    assertThrows(IllegalArgumentException, () -> GsExporter.exportSheet(null))
  }

  @Test
  void testExportSheetWithEmptyColumns() {
    // Create a matrix with no columns
    def matrix = Matrix.builder('test').data([:]).build()
    assertThrows(IllegalArgumentException, () -> GsExporter.exportSheet(matrix))
  }

  @Test
  void testExportSheetWithEmptyRows() {
    // Create a matrix with columns but no rows
    def matrix = Matrix.builder('test').columnNames(['col1', 'col2']).types([String, String]).build()
    assertThrows(IllegalArgumentException, () -> GsExporter.exportSheet(matrix))
  }
}
