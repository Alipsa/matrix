package spreadsheet

import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static se.alipsa.matrix.spreadsheet.SpreadsheetImporter.importExcel
import static se.alipsa.matrix.spreadsheet.SpreadsheetImporter.importSpreadsheet

import groovy.transform.CompileDynamic

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.Importer
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.fastods.FastOdsException

import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SpreadsheetImporterTest {

  @Test
  void testExcelImport() {
    book1ImportAssertions(
      importSpreadsheet(file: "Book1.xlsx", endRow: 12, endCol: 4, firstRowAsColNames: true)
    )
    URL url = getClass().getResource("/Book1.xlsx")
    book1ImportAssertions(
      importExcel(url, 1, 1, 12, 1, 4, true)
    )
  }

  @Test
  void testOdsImport() {
    Matrix m = importSpreadsheet("Book1.ods", 1, 1, 12, 1, 4, true)
    book1ImportAssertions(m)
    Matrix m2 = importSpreadsheet("Book1.ods", 1, 1, 12, 'A', 'D', true)
    book1ImportAssertions(m2)
    Matrix m3 = importSpreadsheet("transactions.ods", 1, 1, 100, 'A', 'D', true)
      .withMatrixName('transactions')
    Assertions.assertEquals('transactions', m3.matrixName)
    Assertions.assertEquals(4, m3.columnCount())
  }

  static void book1ImportAssertions(Matrix table) {

    table = table.convert(id: Integer, bar: LocalDate, baz: BigDecimal, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss.SSS'))
    //println(table.content())
    Assertions.assertEquals(3, table[2, 0])
    Assertions.assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    Assertions.assertEquals(17.4, table['baz'][table.rowCount()-1])
    Assertions.assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void TestImportWithColnames() {
    importWithColnamesAssertions()
  }

  static void importWithColnamesAssertions() {
    def table = importSpreadsheet(
        "file": "Book1.xlsx",
        "endRow": 12,
        "startCol": 'A',
        "endCol": 'D'
    )
    //println(table.content())
    def value = table[2, 0]
    Assertions.assertEquals(3.0, value, "Attempt to compare BigDecimal with " + value.class.simpleName)
    Assertions.assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    Assertions.assertEquals(17.4, table['baz'][table.rowCount()-1])
    Assertions.assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void testImportMultipleSheets() {
    importMultipleSheetsAssertions()
  }

  @CompileDynamic
  void importMultipleSheetsAssertions() {
    Map<String, Matrix> sheets = SpreadsheetImporter.importSpreadsheets("Book2.xlsx",
        [
            [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 6, firstRowAsColNames: true],
            [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true],
            ['key': 'comp', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: false],
            ['key': 'comp2', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: true]
        ])
    Assertions.assertEquals(4, sheets.size())
    Matrix table2 = sheets.Sheet2
    Assertions.assertEquals(3i, table2[2, 0, Integer])
    def date = table2[6, 2]
    Assertions.assertEquals(LocalDate.parse("2023-05-06"), date)
    Assertions.assertEquals(17.4, table2[table2.rowCount()-1, 'baz', BigDecimal].setScale(1, RoundingMode.HALF_UP))
    Assertions.assertEquals(['id', 'foo', 'bar', 'baz'], table2.columnNames())

    Matrix table1 = sheets.Sheet1
    //println(table1.content())
    //println(table1.types())
    Assertions.assertEquals(710381i, table1[0,0, Integer])
    Assertions.assertEquals(103599.04, table1[1,1, BigDecimal].setScale(2, RoundingMode.HALF_UP))
    Assertions.assertEquals(66952.95, table1[2,2])
    Assertions.assertEquals(0.0G, table1[3,3, BigDecimal].setScale(1))
    Assertions.assertEquals(-0.00982d, table1[6, 'percentdiff'] as double, 0.00001)

    Matrix comp = sheets.comp.clone()
    Assertions.assertEquals('Component', comp[0,0])
    Assertions.assertEquals(5, comp.rowCount())
    Assertions.assertEquals(32, comp.columnCount())
    Assertions.assertEquals(31, comp[0,31, Integer])

    def headerRow = comp.row(0)
    List<String> names = headerRow.collect{
      if (it == "Component") {
        String.valueOf(it)
      } else {
        String.valueOf(ValueConverter.asDouble(it).intValue())
      }
    }
    Assertions.assertEquals(32, names.size())
    comp.columnNames(names)
    comp.removeRows(0)
    comp = comp.convert([String] + [Integer]*31 as List<Class<?>>)
    Assertions.assertEquals(2043, comp[0,1])
    Assertions.assertEquals(2790, comp[3,31])

    Matrix comp2 = sheets.comp2
    assertIterableEquals(sheets.comp.typeNames(), comp2.typeNames(), "column types differ")
    comp2.columnNames(comp2.columnNames().collect{
      if (it == "Component") {
        String.valueOf(it)
      } else {
        String.valueOf(ValueConverter.asDouble(it).intValue())
      }
    })
    assertIterableEquals(comp.columnNames(), comp2.columnNames(), "column names differ")
    comp2 = comp2.convert([String] + [Integer]*31 as List<Class<?>>)
    assertIterableEquals(comp.typeNames(), comp2.typeNames(), "column types differ after column name setting")
  }

  @Test
  void testFastExcelSheetNumberIsOneIndexedForFileImport() {
    assertSheetNumberIsOneIndexedForFile(FExcelImporter.create())
  }

  @Test
  void testFastExcelSheetNumberIsOneIndexedForStreamImport() {
    assertSheetNumberIsOneIndexedForStream(FExcelImporter.create())
  }

  @Test
  void testImportXlsThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      SpreadsheetImporter.importSpreadsheet("Book1.xls", 1, 1, 2, 1, 2, true)
    })
  }

  private static void assertSheetNumberIsOneIndexedForFile(Importer importer) {
    Matrix book1Sheet1ByName = importer.importSpreadsheet("Book1.xlsx", "Sheet1", 1, 12, 1, 4, true)
    Matrix book1Sheet1ByNumber = importer.importSpreadsheet("Book1.xlsx", 1, 1, 12, 1, 4, true)
    MatrixAssertions.assertEquals(book1Sheet1ByName, book1Sheet1ByNumber)

    Matrix book2Sheet2ByName = importer.importSpreadsheet("Book2.xlsx", "Sheet2", 1, 12, 1, 4, true)
    Matrix book2Sheet2ByNumber = importer.importSpreadsheet("Book2.xlsx", 2, 1, 12, 1, 4, true)
    MatrixAssertions.assertEquals(book2Sheet2ByName, book2Sheet2ByNumber)
  }

  private static void assertSheetNumberIsOneIndexedForStream(Importer importer) {
    Matrix book1Sheet1ByName = importFromStream(importer, "Book1.xlsx", "Sheet1", 1, 12, 1, 4)
    Matrix book1Sheet1ByNumber = importFromStream(importer, "Book1.xlsx", 1, 1, 12, 1, 4)
    MatrixAssertions.assertEquals(book1Sheet1ByName, book1Sheet1ByNumber)

    Matrix book2Sheet2ByName = importFromStream(importer, "Book2.xlsx", "Sheet2", 1, 12, 1, 4)
    Matrix book2Sheet2ByNumber = importFromStream(importer, "Book2.xlsx", 2, 1, 12, 1, 4)
    MatrixAssertions.assertEquals(book2Sheet2ByName, book2Sheet2ByNumber)
  }

  private static Matrix importFromStream(Importer importer, String fileName, String sheetName,
                                         int startRow, int endRow, int startCol, int endCol) {
    InputStream stream = SpreadsheetImporterTest.class.getResourceAsStream("/${fileName}")
    assertNotNull(stream, "Missing test resource ${fileName}")
    try {
      return importer.importSpreadsheet(stream, sheetName, startRow, endRow, startCol, endCol, true)
    } finally {
      stream.close()
    }
  }

  private static Matrix importFromStream(Importer importer, String fileName, int sheetNumber,
                                         int startRow, int endRow, int startCol, int endCol) {
    InputStream stream = SpreadsheetImporterTest.class.getResourceAsStream("/${fileName}")
    assertNotNull(stream, "Missing test resource ${fileName}")
    try {
      return importer.importSpreadsheet(stream, sheetNumber, startRow, endRow, startCol, endCol, true)
    } finally {
      stream.close()
    }
  }

  @Test
  void testAutoDetectStringPathConvenienceForm() {
    Matrix m = importSpreadsheet("Book1.xlsx")
    book1ImportAssertions(m)
  }

  @Test
  void testAutoDetectXlsxByFile() throws Exception {
    File file = new File(SpreadsheetImporterTest.class.getResource("/Book1.xlsx").toURI())
    Matrix m = importSpreadsheet(file, 1, true)
    book1ImportAssertions(m)
  }

  @Test
  void testAutoDetectXlsxByName() {
    Matrix m = importSpreadsheet("Book1.xlsx", "Sheet1", true)
    book1ImportAssertions(m)
  }

  @Test
  void testAutoDetectOdsByFile() throws Exception {
    File file = new File(SpreadsheetImporterTest.class.getResource("/Book1.ods").toURI())
    Matrix m = importSpreadsheet(file, 1, true)
    book1ImportAssertions(m)
  }

  @Test
  void testAutoDetectOdsByName() {
    Matrix m = importSpreadsheet("Book1.ods", "Sheet1", true)
    book1ImportAssertions(m)
  }

  @Test
  void testAutoDetectXlsxByFileAndSheetName() throws Exception {
    File file = new File(SpreadsheetImporterTest.class.getResource("/Book1.xlsx").toURI())
    Matrix m = importSpreadsheet(file, "Sheet1", true)
    book1ImportAssertions(m)
  }

  @Test
  void testFileBasedDelegationWithExplicitRange() throws Exception {
    File file = new File(SpreadsheetImporterTest.class.getResource("/Book1.xlsx").toURI())
    Matrix m = importSpreadsheet(file, 1, 1, 12, 1, 4, true)
    book1ImportAssertions(m)
  }

  @Test
  void testMissingSheetThrowsWithSheetName() {
    assertThrows(NoSuchElementException.class, {
      importSpreadsheet("Book1.xlsx", "NonExistentSheet", 1, 12, 1, 4, true)
    })
  }

  @Test
  void testMissingOdsSheetThrowsFastOdsException() {
    assertThrows(FastOdsException.class, {
      importSpreadsheet("Book1.ods", "NonExistentSheet", 1, 12, 1, 4, true)
    })
  }

  @Test
  void testImportSpreadsheetsWithFile() throws Exception {
    File file = new File(SpreadsheetImporterTest.class.getResource("/Book2.xlsx").toURI())
    Map<Object, Matrix> sheets = SpreadsheetImporter.importSpreadsheets(file,
        [
            [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 6, firstRowAsColNames: true],
            [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true]
        ])
    Assertions.assertEquals(2, sheets.size())
    assertNotNull(sheets.Sheet1)
    assertNotNull(sheets.Sheet2)
  }
}
