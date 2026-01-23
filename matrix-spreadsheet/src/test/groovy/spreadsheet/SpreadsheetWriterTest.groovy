package spreadsheet

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter
import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.ExcelImplementation

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.*

class SpreadsheetWriterTest {

  static Matrix table
  static Matrix table2
  static Matrix table3

  @BeforeAll
  static void init() {
    def matrix = [
        id     : [null, 2, 3, 4, -5],
        name   : ['foo', 'bar', 'baz', 'bla', null],
        start  : toLocalDates('2021-01-04', null, '2023-03-13', '2024-04-15', '2025-05-20'),
        end    : toLocalDateTimes(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), '2021-02-04 12:01:22', '2022-03-12 13:14:15', '2023-04-13 15:16:17', null, '2025-06-20 17:18:19'),
        measure: [12.45, null, 14.11, 15.23, 10.99],
        active : [true, false, null, true, false]
    ]
    table = Matrix.builder().data(matrix).types(Integer, String, LocalDate, LocalDateTime, BigDecimal, Boolean).build()

    def stats = [
        id : [null, 2, 3, 4, -5],
        jan: toBigDecimals([1123.1234, 2341.234, 1010.00122, 991, 1100.1]),
        feb: [1111.1235, 2312.235, 1001.00121, 999, 1200.7]
    ]
    table2 = Matrix.builder().data(stats).types(Integer, BigDecimal, BigDecimal).build()

    table3 = Matrix.builder()
        .data(id: [1, 2, 3], name: ['Alice', 'Bob', 'Charlie'])
        .build()
  }

  @Test
  void testWriteExcelBasic() {
    def file = File.createTempFile("matrix-writer", ".xlsx")
    String sheetName = SpreadsheetWriter.write(table, file)

    assertNotNull(sheetName, "Sheet name should not be null")
    assertTrue(file.exists(), "File should exist")

    try (def reader = SpreadsheetReader.Factory.create(file)) {
      assertEquals(1, reader.sheetNames.size(), "Should have one sheet")
    }
  }

  @Test
  void testWriteExcelWithSheetName() {
    def file = File.createTempFile("matrix-writer-named", ".xlsx")
    String sheetName = SpreadsheetWriter.write(table, file, "MySheet")

    assertNotNull(sheetName)
    assertEquals("MySheet", sheetName)
    assertTrue(file.exists())

    try (def reader = SpreadsheetReader.Factory.create(file)) {
      assertEquals(1, reader.sheetNames.size())
      assertEquals("MySheet", reader.sheetNames[0])
    }
  }

  @Test
  void testWriteMultipleSheets() {
    def file = File.createTempFile("matrix-writer-multi", ".xlsx")
    if (file.exists()) {
      file.delete()
    }

    List<String> sheetNames = SpreadsheetWriter.writeSheets([table, table2, table3], file, ["Sheet1", "Sheet2", "Sheet3"])

    assertNotNull(sheetNames)
    assertEquals(3, sheetNames.size())
    assertTrue(file.exists())

    try (def reader = SpreadsheetReader.Factory.create(file)) {
      assertEquals(3, reader.sheetNames.size(), "Should have three sheets")
      assertEquals(["Sheet1", "Sheet2", "Sheet3"], reader.sheetNames)
    }
  }

  @Test
  void testWriteMultipleSheetsWithMap() {
    def file = File.createTempFile("matrix-writer-map", ".xlsx")
    if (file.exists()) {
      file.delete()
    }

    List<String> sheetNames = SpreadsheetWriter.writeSheets([
        file: file,
        data: [table, table2],
        sheetNames: ["Data1", "Data2"]
    ])

    assertNotNull(sheetNames)
    assertEquals(2, sheetNames.size())
    assertTrue(file.exists())

    try (def reader = SpreadsheetReader.Factory.create(file)) {
      assertEquals(2, reader.sheetNames.size())
    }
  }

  @Test
  void testWriteOds() {
    File odsFile = File.createTempFile("matrix-writer-ods", ".ods")
    if (odsFile.exists()) {
      odsFile.delete()
    }

    String sheetName = SpreadsheetWriter.write(table, odsFile, "Sheet 1")
    assertNotNull(sheetName)
    assertTrue(odsFile.exists())

    try (def reader = SpreadsheetReader.Factory.create(odsFile)) {
      assertEquals(1, reader.sheetNames.size())
    }
  }

  @Test
  void testWriteOdsMultipleSheets() {
    File odsFile = File.createTempFile("matrix-writer-ods-multi", ".ods")
    if (odsFile.exists()) {
      odsFile.delete()
    }

    List<String> sheetNames = SpreadsheetWriter.writeSheets([table, table2], odsFile, ["Sheet1", "Sheet2"])

    assertNotNull(sheetNames)
    assertTrue(odsFile.exists())

    try (def reader = SpreadsheetReader.Factory.create(odsFile)) {
      assertEquals(2, reader.sheetNames.size())
    }
  }

  @Test
  void testWriteWithDifferentImplementation() {
    // Test with POI
    SpreadsheetWriter.excelImplementation = ExcelImplementation.POI
    def poiFile = File.createTempFile("matrix-writer-poi", ".xlsx")
    String sheetName1 = SpreadsheetWriter.write(table3, poiFile)
    assertNotNull(sheetName1)
    assertTrue(poiFile.exists())

    // Test with FastExcel
    SpreadsheetWriter.excelImplementation = ExcelImplementation.FastExcel
    def fastFile = File.createTempFile("matrix-writer-fast", ".xlsx")
    String sheetName2 = SpreadsheetWriter.write(table3, fastFile)
    assertNotNull(sheetName2)
    assertTrue(fastFile.exists())
  }

  @Test
  void testWriteNullMatrixThrows() {
    def file = File.createTempFile("matrix-writer-null", ".xlsx")
    assertThrows(IllegalArgumentException.class, () -> {
      SpreadsheetWriter.write(null, file)
    }, "Should throw on null matrix")
  }

  @Test
  void testWriteNullFileThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      SpreadsheetWriter.write(table, (File) null)
    }, "Should throw on null file")
  }

  @Test
  void testWriteNullSheetNameThrows() {
    def file = File.createTempFile("matrix-writer-nullsheet", ".xlsx")
    assertThrows(IllegalArgumentException.class, () -> {
      SpreadsheetWriter.write(table, file, null)
    }, "Should throw on null sheet name")
  }

  @Test
  void testWriteSheetsMismatchedSizesThrows() {
    def file = File.createTempFile("matrix-writer-mismatch", ".xlsx")
    assertThrows(IllegalArgumentException.class, () -> {
      SpreadsheetWriter.writeSheets([table, table2], file, ["Sheet1"])
    }, "Should throw when lists have different sizes")
  }

  @Test
  void testDeprecatedExporterStillWorks() {
    // Verify deprecated class still delegates correctly
    def file = File.createTempFile("matrix-deprecated", ".xlsx")
    se.alipsa.matrix.spreadsheet.SpreadsheetExporter.excelImplementation = ExcelImplementation.POI
    String sheetName = se.alipsa.matrix.spreadsheet.SpreadsheetExporter.exportSpreadsheet(file, table)

    assertNotNull(sheetName)
    assertTrue(file.exists())
  }
}
