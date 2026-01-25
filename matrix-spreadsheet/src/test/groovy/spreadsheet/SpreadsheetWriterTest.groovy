package spreadsheet

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter
import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

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
  void testWriteExcelWithStartPosition() {
    def file = File.createTempFile("matrix-writer-offset", ".xlsx")
    if (file.exists()) {
      file.delete()
    }

    String sheetName = SpreadsheetWriter.write(table, file, "Offset", "B3")
    SpreadsheetUtil.CellPosition position = SpreadsheetUtil.parseCellPosition("B3")
    int endRow = position.row + table.rowCount()
    int endCol = position.column + table.columnCount() - 1
    Matrix imported = SpreadsheetImporter.importSpreadsheet(file.absolutePath, sheetName, position.row, endRow, position.column, endCol, true)

    assertTrue(table.equals(imported, false, true, true), "Imported matrix should match written data")
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
  void testWriteMultipleSheetsWithPositionsMap() {
    def file = File.createTempFile("matrix-writer-map-pos", ".xlsx")
    if (file.exists()) {
      file.delete()
    }

    LinkedHashMap<String, String> positions = ["First": "B2", "Second": "D4"]
    List<String> sheetNames = SpreadsheetWriter.writeSheets([table2, table3], file, positions)

    assertNotNull(sheetNames)
    assertEquals(["First", "Second"], sheetNames)

    SpreadsheetUtil.CellPosition pos1 = SpreadsheetUtil.parseCellPosition("B2")
    SpreadsheetUtil.CellPosition pos2 = SpreadsheetUtil.parseCellPosition("D4")
    Matrix imported1 = SpreadsheetImporter.importSpreadsheet(
        file.absolutePath,
        "First",
        pos1.row,
        pos1.row + table2.rowCount(),
        pos1.column,
        pos1.column + table2.columnCount() - 1,
        true
    )
    Matrix imported2 = SpreadsheetImporter.importSpreadsheet(
        file.absolutePath,
        "Second",
        pos2.row,
        pos2.row + table3.rowCount(),
        pos2.column,
        pos2.column + table3.columnCount() - 1,
        true
    )

    assertTrue(table2.equals(imported1, false, true, true), "First sheet should match written data")
    assertTrue(table3.equals(imported2, false, true, true), "Second sheet should match written data")
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
  void testWriteOdsWithStartPosition() {
    File odsFile = File.createTempFile("matrix-writer-ods-offset", ".ods")
    if (odsFile.exists()) {
      odsFile.delete()
    }

    String sheetName = SpreadsheetWriter.write(table, odsFile, "Sheet 1", "C2")
    SpreadsheetUtil.CellPosition position = SpreadsheetUtil.parseCellPosition("C2")
    int endRow = position.row + table.rowCount()
    int endCol = position.column + table.columnCount() - 1
    Matrix imported = SpreadsheetImporter.importSpreadsheet(odsFile.absolutePath, sheetName, position.row, endRow, position.column, endCol, true)

    assertTrue(table.equals(imported, false, true, true), "Imported ODS data should match written data")
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
    String sheetName = se.alipsa.matrix.spreadsheet.SpreadsheetExporter.exportSpreadsheet(file, table)

    assertNotNull(sheetName)
    assertTrue(file.exists())
  }

  @Test
  void testWriteXlsThrows() {
    def file = File.createTempFile("matrix-writer-xls", ".xls")
    assertThrows(IllegalArgumentException.class, () -> {
      SpreadsheetWriter.write(table, file)
    }, "Should throw on unsupported .xls format")
  }

  @Test
  void testAppendXlsxPreservesExistingSheet() {
    File file = copyResourceToTempFile("Book3.xlsx", ".xlsx")
    Matrix before = SpreadsheetImporter.importSpreadsheet(file.absolutePath, "Sheet1", 1, 3, 1, 3, true)
    Matrix newSheet = Matrix.builder().data(id: [1, 2], name: ["a", "b"]).build()
    SpreadsheetWriter.write(newSheet, file, "Appended")
    Matrix after = SpreadsheetImporter.importSpreadsheet(file.absolutePath, "Sheet1", 1, 3, 1, 3, true)
    assertEquals(before, after)
    try (def reader = SpreadsheetReader.Factory.create(file)) {
      assertTrue(reader.sheetNames.contains("Appended"))
    }
  }

  @Test
  void testAppendXlsxInheritsBaseFormatting() {
    File file = copyResourceToTempFile("Book3.xlsx", ".xlsx")
    SpreadsheetWriter.write(table3, file, "Appended")

    String baseXml = readXlsxSheetXml(file, "Sheet1")
    String appendedXml = readXlsxSheetXml(file, "Appended")

    String baseRowHeight = extractSheetFormatAttribute(baseXml, "defaultRowHeight")
    String appendedRowHeight = extractSheetFormatAttribute(appendedXml, "defaultRowHeight")
    assertNotNull(baseRowHeight)
    assertEquals(baseRowHeight, appendedRowHeight)

    String baseColWidth = extractFirstColumnWidth(baseXml)
    String appendedColWidth = extractFirstColumnWidth(appendedXml)
    assertNotNull(baseColWidth)
    assertEquals(baseColWidth, appendedColWidth)

    String baseLeftMargin = extractPageMargin(baseXml, "left")
    String appendedLeftMargin = extractPageMargin(appendedXml, "left")
    assertNotNull(baseLeftMargin)
    assertEquals(baseLeftMargin, appendedLeftMargin)
  }

  @Test
  void testReplaceXlsxSheet() {
    File file = File.createTempFile("matrix-replace", ".xlsx")
    Matrix original = Matrix.builder().data(id: [1], name: ["a"]).build()
    SpreadsheetWriter.write(original, file, "Data")
    Matrix replacement = Matrix.builder().data(id: [2], name: ["b"]).build()
    SpreadsheetWriter.write(replacement, file, "Data")
    Matrix imported = SpreadsheetImporter.importSpreadsheet(file.absolutePath, "Data", 1, 2, 1, 2, true)
    assertEquals(replacement, imported)
  }

  @Test
  void testAppendOdsPreservesExistingSheet() {
    File file = copyResourceToTempFile("Book3.ods", ".ods")
    Matrix before = SpreadsheetImporter.importSpreadsheet(file.absolutePath, "Sheet1", 1, 3, 1, 3, true)
    Matrix newSheet = Matrix.builder().data(id: [1, 2], name: ["a", "b"]).build()
    SpreadsheetWriter.write(newSheet, file, "Appended")
    Matrix after = SpreadsheetImporter.importSpreadsheet(file.absolutePath, "Sheet1", 1, 3, 1, 3, true)
    assertEquals(before, after)
    try (def reader = SpreadsheetReader.Factory.create(file)) {
      assertTrue(reader.sheetNames.contains("Appended"))
    }
  }

  @Test
  void testAppendOdsInheritsTableStyle() {
    File file = copyResourceToTempFile("Book3.ods", ".ods")
    SpreadsheetWriter.write(table3, file, "Appended")

    String contentXml = readZipEntry(file, "content.xml")
    String baseStyle = extractOdsTableStyle(contentXml, "Sheet1")
    String appendedStyle = extractOdsTableStyle(contentXml, "Appended")
    assertNotNull(baseStyle)
    assertEquals(baseStyle, appendedStyle)

    String baseColumnStyle = extractOdsFirstColumnStyle(contentXml, "Sheet1")
    String appendedColumnStyle = extractOdsFirstColumnStyle(contentXml, "Appended")
    assertNotNull(baseColumnStyle)
    assertEquals(baseColumnStyle, appendedColumnStyle)
  }

  @Test
  void testOdsStopsAtTrailingEmptyRows() {
    File odsFile = createOdsWithTrailingEmptyRows()
    try (def reader = SpreadsheetReader.Factory.create(odsFile)) {
      assertEquals(2, reader.findLastRow("Sheet1"))
    }
  }

  @Test
  void testReplaceOdsSheet() {
    File file = File.createTempFile("matrix-replace", ".ods")
    Matrix original = Matrix.builder().data(id: [1], name: ["a"]).build()
    SpreadsheetWriter.write(original, file, "Data")
    Matrix replacement = Matrix.builder().data(id: [2], name: ["b"]).build()
    SpreadsheetWriter.write(replacement, file, "Data")
    Matrix imported = SpreadsheetImporter.importSpreadsheet(file.absolutePath, "Data", 1, 2, 1, 2, true)
    assertEquals(replacement, imported)
  }

  private static File copyResourceToTempFile(String resourceName, String suffix) {
    File file = File.createTempFile("matrix-resource", suffix)
    InputStream is = SpreadsheetWriterTest.class.getResourceAsStream("/${resourceName}")
    assertNotNull(is, "Missing test resource ${resourceName}")
    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    is.close()
    file
  }

  private static File createOdsWithTrailingEmptyRows() {
    File file = File.createTempFile("matrix-trailing-empty", ".ods")
    String contentXml = '''<?xml version="1.0" encoding="UTF-8"?>
<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
  xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" office:version="1.2">
  <office:body>
    <office:spreadsheet>
      <table:table table:name="Sheet1">
        <table:table-row>
          <table:table-cell office:value-type="string"><text:p>col1</text:p></table:table-cell>
        </table:table-row>
        <table:table-row>
          <table:table-cell office:value-type="string"><text:p>val1</text:p></table:table-cell>
        </table:table-row>
        <table:table-row table:number-rows-repeated="2000"/>
      </table:table>
    </office:spreadsheet>
  </office:body>
</office:document-content>
'''
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
      ZipEntry mimetype = new ZipEntry("mimetype")
      zos.putNextEntry(mimetype)
      zos.write("application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.UTF_8))
      zos.closeEntry()

      ZipEntry content = new ZipEntry("content.xml")
      zos.putNextEntry(content)
      zos.write(contentXml.getBytes(StandardCharsets.UTF_8))
      zos.closeEntry()
    }
    file
  }

  private static String readXlsxSheetXml(File file, String sheetName) {
    try (ZipFile zip = new ZipFile(file)) {
      String workbookXml = readZipEntry(zip, "xl/workbook.xml")
      String relsXml = readZipEntry(zip, "xl/_rels/workbook.xml.rels")
      String relId = extractSheetRelId(workbookXml, sheetName)
      assertNotNull(relId, "Missing sheet ${sheetName}")
      String target = extractRelationshipTarget(relsXml, relId)
      assertNotNull(target, "Missing relationship ${relId}")
      String path = target.startsWith("xl/") ? target : "xl/${target}"
      return readZipEntry(zip, path)
    }
  }

  private static String readZipEntry(File file, String path) {
    try (ZipFile zip = new ZipFile(file)) {
      return readZipEntry(zip, path)
    }
  }

  private static String readZipEntry(ZipFile zip, String path) {
    def entry = zip.getEntry(path)
    assertNotNull(entry, "Missing zip entry ${path}")
    return new String(zip.getInputStream(entry).bytes, "UTF-8")
  }

  private static String extractSheetFormatAttribute(String xml, String attribute) {
    Pattern pattern = Pattern.compile("<sheetFormatPr[^>]*\\b${attribute}=\"([^\"]+)\"")
    def matcher = pattern.matcher(xml)
    return matcher.find() ? matcher.group(1) : null
  }

  private static String extractFirstColumnWidth(String xml) {
    Pattern pattern = Pattern.compile("<col[^>]*\\bwidth=\"([^\"]+)\"")
    def matcher = pattern.matcher(xml)
    return matcher.find() ? matcher.group(1) : null
  }

  private static String extractPageMargin(String xml, String attribute) {
    Pattern pattern = Pattern.compile("<pageMargins[^>]*\\b${attribute}=\"([^\"]+)\"")
    def matcher = pattern.matcher(xml)
    return matcher.find() ? matcher.group(1) : null
  }

  private static String extractOdsTableStyle(String contentXml, String sheetName) {
    String escapedName = Pattern.quote(sheetName)
    Pattern tablePattern = Pattern.compile("<table:table\\b[^>]*table:name=\"${escapedName}\"[^>]*>")
    def tableMatcher = tablePattern.matcher(contentXml)
    if (!tableMatcher.find()) {
      return null
    }
    String tableTag = tableMatcher.group()
    Pattern stylePattern = Pattern.compile("table:style-name=\"([^\"]+)\"")
    def styleMatcher = stylePattern.matcher(tableTag)
    return styleMatcher.find() ? styleMatcher.group(1) : null
  }

  private static String extractSheetRelId(String workbookXml, String sheetName) {
    String escapedName = Pattern.quote(sheetName)
    Pattern sheetPattern = Pattern.compile("<sheet\\b[^>]*\\bname=\"${escapedName}\"[^>]*>")
    def sheetMatcher = sheetPattern.matcher(workbookXml)
    if (!sheetMatcher.find()) {
      return null
    }
    String sheetTag = sheetMatcher.group()
    Pattern relPattern = Pattern.compile("\\br:id=\"([^\"]+)\"")
    def relMatcher = relPattern.matcher(sheetTag)
    return relMatcher.find() ? relMatcher.group(1) : null
  }

  private static String extractRelationshipTarget(String relsXml, String relId) {
    String escapedId = Pattern.quote(relId)
    Pattern relPattern = Pattern.compile("<Relationship\\b[^>]*\\bId=\"${escapedId}\"[^>]*>")
    def relMatcher = relPattern.matcher(relsXml)
    if (!relMatcher.find()) {
      return null
    }
    String relTag = relMatcher.group()
    Pattern targetPattern = Pattern.compile("\\bTarget=\"([^\"]+)\"")
    def targetMatcher = targetPattern.matcher(relTag)
    return targetMatcher.find() ? targetMatcher.group(1) : null
  }

  private static String extractOdsFirstColumnStyle(String contentXml, String sheetName) {
    String escapedName = Pattern.quote(sheetName)
    Pattern tablePattern = Pattern.compile("(?s)<table:table\\b[^>]*table:name=\"${escapedName}\"[^>]*>(.*?)</table:table>")
    def tableMatcher = tablePattern.matcher(contentXml)
    if (!tableMatcher.find()) {
      return null
    }
    String tableBody = tableMatcher.group(1)
    Pattern columnPattern = Pattern.compile("<table:table-column\\b[^>]*>")
    def columnMatcher = columnPattern.matcher(tableBody)
    if (!columnMatcher.find()) {
      return null
    }
    String columnTag = columnMatcher.group()
    Pattern stylePattern = Pattern.compile("table:style-name=\"([^\"]+)\"")
    def styleMatcher = stylePattern.matcher(columnTag)
    return styleMatcher.find() ? styleMatcher.group(1) : null
  }
}
