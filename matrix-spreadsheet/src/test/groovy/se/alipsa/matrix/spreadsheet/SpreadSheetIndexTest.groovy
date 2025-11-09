package se.alipsa.matrix.spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions

import java.net.URL
import java.nio.file.Paths
import java.util.Objects

import static org.junit.jupiter.api.Assertions.*

class SpreadSheetIndexTest {

  private static final int HEADER_ROW_INDEX = 1
  private static final int FIRST_DATA_ROW_INDEX = 2
  private static final int FIRST_COLUMN_INDEX = 1
  private static final int SECOND_COLUMN_INDEX = 2

  @Test
  void importSpreadsheetFilePathIntIndicesAreOneBased() {
    assertFilePathImportIndicesAreOneBased("Book1.xlsx")
    assertFilePathImportIndicesAreOneBased("Book1.ods")
  }

  @Test
  void importSpreadsheetFilePathWithStringColumnsStillHonoursOneBasedIntIndices() {
    assertFilePathImportWithStringColumns("Book1.xlsx")
    assertFilePathImportWithStringColumns("Book1.ods")
  }

  @Test
  void importOdsUrlIntIndicesAreOneBased() {
    URL url = resourceUrl("Book1.ods")
    Matrix header = SpreadsheetImporter.importOds(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importOds(url, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertFirstDataCell(firstDataRow)

    Matrix secondColumnHeader = SpreadsheetImporter.importOds(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        SECOND_COLUMN_INDEX, SECOND_COLUMN_INDEX, false)
    assertSecondColumnHeader(secondColumnHeader)

    Matrix sheetByNumber = SpreadsheetImporter.importOds(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    Matrix sheetByName = SpreadsheetImporter.importOds(url, "Sheet1", HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    MatrixAssertions.assertEquals(sheetByName, sheetByNumber)
  }

  @Test
  void importOdsUrlStringColumnsStillHonoursOneBasedIntIndices() {
    URL url = resourceUrl("Book1.ods")
    Matrix header = SpreadsheetImporter.importOds(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        'A', 'A', false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importOds(url, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        'A', 'A', false)
    assertFirstDataCell(firstDataRow)

    Matrix stringSheetHeader = SpreadsheetImporter.importOds(url, "Sheet1", HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        'A', 'A', false)
    assertHeaderCell(stringSheetHeader)

    Matrix stringSheetFirstRow = SpreadsheetImporter.importOds(url, "Sheet1", FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        'A', 'A', false)
    assertFirstDataCell(stringSheetFirstRow)
  }

  @Test
  void importOdsUrlStringSheetIntColumnsAreOneBased() {
    URL url = resourceUrl("Book1.ods")
    Matrix header = SpreadsheetImporter.importOds(url, "Sheet1", HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importOds(url, "Sheet1", FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertFirstDataCell(firstDataRow)

    Matrix secondColumnHeader = SpreadsheetImporter.importOds(url, "Sheet1", HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        SECOND_COLUMN_INDEX, SECOND_COLUMN_INDEX, false)
    assertSecondColumnHeader(secondColumnHeader)
  }

  @Test
  void importExcelUrlIntIndicesAreOneBased() {
    URL url = resourceUrl("Book1.xlsx")
    Matrix header = SpreadsheetImporter.importExcel(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importExcel(url, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertFirstDataCell(firstDataRow)

    Matrix secondColumnHeader = SpreadsheetImporter.importExcel(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        SECOND_COLUMN_INDEX, SECOND_COLUMN_INDEX, false)
    assertSecondColumnHeader(secondColumnHeader)

    Matrix sheetByNumber = SpreadsheetImporter.importExcel(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    Matrix sheetByName = SpreadsheetImporter.importExcel(url, "Sheet1", HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    MatrixAssertions.assertEquals(sheetByName, sheetByNumber)
  }

  @Test
  void importExcelUrlStringColumnsStillHonoursOneBasedIntIndices() {
    URL url = resourceUrl("Book1.xlsx")
    Matrix header = SpreadsheetImporter.importExcel(url, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        'A', 'A', false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importExcel(url, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        'A', 'A', false)
    assertFirstDataCell(firstDataRow)

    Matrix stringSheetHeader = SpreadsheetImporter.importExcel(url, "Sheet1", HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        'A', 'A', false)
    assertHeaderCell(stringSheetHeader)

    Matrix stringSheetFirstRow = SpreadsheetImporter.importExcel(url, "Sheet1", FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        'A', 'A', false)
    assertFirstDataCell(stringSheetFirstRow)
  }

  @Test
  void importExcelUrlStringSheetIntColumnsAreOneBased() {
    URL url = resourceUrl("Book1.xlsx")
    Matrix header = SpreadsheetImporter.importExcel(url, "Sheet1", HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importExcel(url, "Sheet1", FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertFirstDataCell(firstDataRow)

    Matrix secondColumnHeader = SpreadsheetImporter.importExcel(url, "Sheet1", HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        SECOND_COLUMN_INDEX, SECOND_COLUMN_INDEX, false)
    assertSecondColumnHeader(secondColumnHeader)
  }

  @Test
  void spreadsheetReaderIntIndicesAreOneBasedForExcelAndOds() {
    assertReaderIndicesAreOneBased("Book1.xlsx")
    assertReaderIndicesAreOneBased("Book1.ods")
  }

  @Test
  void exporterProducesSheetsReadableWithOneBasedIndices() {
    Matrix sheet1 = SpreadsheetImporter.importSpreadsheet(resourcePath("Book1.xlsx"), HEADER_ROW_INDEX,
        HEADER_ROW_INDEX, HEADER_ROW_INDEX + 11, FIRST_COLUMN_INDEX, SECOND_COLUMN_INDEX + 2, true)
    Matrix sheet2 = SpreadsheetImporter.importSpreadsheet(resourcePath("Book1.xlsx"), HEADER_ROW_INDEX,
        HEADER_ROW_INDEX, HEADER_ROW_INDEX + 11, FIRST_COLUMN_INDEX, SECOND_COLUMN_INDEX + 2, true)

    File tempFile = File.createTempFile("spreadsheet-index", ".xlsx")
    tempFile.deleteOnExit()
    SpreadsheetExporter.exportSpreadsheet(tempFile, sheet1, "First")
    SpreadsheetExporter.exportSpreadsheet(tempFile, sheet2, "Second")

    try (SpreadsheetReader reader = SpreadsheetReader.Factory.create(tempFile)) {
      assertEquals(2, reader.sheetNames.size())
      assertEquals("First", reader.sheetNames.get(0))
      assertTrue(reader.findLastRow(HEADER_ROW_INDEX) >= sheet1.rowCount())
      assertTrue(reader.findLastRow(HEADER_ROW_INDEX + 1) >= sheet2.rowCount())
    } finally {
      assertTrue(tempFile.delete() || !tempFile.exists())
    }
  }

  private static void assertFilePathImportIndicesAreOneBased(String fileName) {
    String path = resourcePath(fileName)
    Matrix header = SpreadsheetImporter.importSpreadsheet(path, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importSpreadsheet(path, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    assertFirstDataCell(firstDataRow)

    Matrix secondColumnHeader = SpreadsheetImporter.importSpreadsheet(path, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        SECOND_COLUMN_INDEX, SECOND_COLUMN_INDEX, false)
    assertSecondColumnHeader(secondColumnHeader)

    Matrix sheetByNumber = SpreadsheetImporter.importSpreadsheet(path, HEADER_ROW_INDEX, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    Matrix sheetByName = SpreadsheetImporter.importSpreadsheet(path, "Sheet1", HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        FIRST_COLUMN_INDEX, FIRST_COLUMN_INDEX, false)
    MatrixAssertions.assertEquals(sheetByName, sheetByNumber)
  }

  private static void assertFilePathImportWithStringColumns(String fileName) {
    String path = resourcePath(fileName)
    Matrix header = SpreadsheetImporter.importSpreadsheet(path, HEADER_ROW_INDEX, HEADER_ROW_INDEX, HEADER_ROW_INDEX,
        'A', 'A', false)
    assertHeaderCell(header)

    Matrix firstDataRow = SpreadsheetImporter.importSpreadsheet(path, HEADER_ROW_INDEX, FIRST_DATA_ROW_INDEX, FIRST_DATA_ROW_INDEX,
        'A', 'A', false)
    assertFirstDataCell(firstDataRow)
  }

  private static void assertReaderIndicesAreOneBased(String resource) {
    try (SpreadsheetReader reader = SpreadsheetReader.Factory.create(resource)) {
      assertEquals(12, reader.findLastRow(HEADER_ROW_INDEX), "Last row should be 12 for $resource")
      assertEquals(4, reader.findLastCol(HEADER_ROW_INDEX), "Last column should be 4 for $resource")
      assertEquals(5, reader.findRowNum(HEADER_ROW_INDEX, SECOND_COLUMN_INDEX, 'gång'), "Row lookup failed for $resource")
      assertEquals(5, reader.findRowNum(HEADER_ROW_INDEX, 'B', 'gång'), "Row lookup by column name failed for $resource")
      assertEquals(5, reader.findRowNum('Sheet1', SECOND_COLUMN_INDEX, 'gång'), "Row lookup by sheet name failed for $resource")
      assertEquals(4, reader.findColNum(HEADER_ROW_INDEX, 8, '20.9'), "Column lookup failed for $resource")
      assertEquals(4, reader.findColNum('Sheet1', 8, '20.9'), "Column lookup by sheet name failed for $resource")
    }
  }

  private static void assertHeaderCell(Matrix matrix) {
    assertEquals('id', String.valueOf(matrix[0, 0]))
  }

  private static void assertFirstDataCell(Matrix matrix) {
    Object value = matrix[0, 0]
    assertTrue(value instanceof Number, 'Expected a numeric value for the first data cell')
    assertEquals(1, ((Number) value).intValue())
  }

  private static void assertSecondColumnHeader(Matrix matrix) {
    assertEquals('foo', String.valueOf(matrix[0, 0]))
  }

  private static String resourcePath(String resource) {
    URL url = resourceUrl(resource)
    return Paths.get(url.toURI()).toString()
  }

  private static URL resourceUrl(String resource) {
    return Objects.requireNonNull(SpreadSheetIndexTest.class.getResource("/${resource}"), "Missing resource ${resource}")
  }
}
