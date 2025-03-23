package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.ExcelImplementation
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static se.alipsa.matrix.spreadsheet.SpreadsheetImporter.importSpreadsheet

class SpreadsheetImporterTest {

  @Test
  void testExcelImport() {
    excelImportAssertions(ExcelImplementation.POI)
    excelImportAssertions(ExcelImplementation.FastExcel)

  }

  static void excelImportAssertions(ExcelImplementation excelImplementation) {
    def table = importSpreadsheet(file: "Book1.xlsx", endRow: 12, endCol: 4, firstRowAsColNames: true, excelImplementation: excelImplementation)
    table = table.convert(id: Integer, bar: LocalDate, baz: BigDecimal, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss.SSS'))
    //println(table.content())
    assertEquals(3, table[2, 0])
    assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    assertEquals(17.4, table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void TestImportWithColnames() {
    importWithColnamesAssertions(ExcelImplementation.POI)
    importWithColnamesAssertions(ExcelImplementation.FastExcel)
  }

  static void importWithColnamesAssertions(ExcelImplementation excelImplementation) {
    def table = importSpreadsheet(
        "file": "Book1.xlsx",
        "endRow": 12,
        "startCol": 'A',
        "endCol": 'D',
        'excelImplementation': excelImplementation
    )
    //println(table.content())
    assertEquals(3.0d, table[2, 0])
    assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    assertEquals(17.4, table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void testImportMultipleSheets() {
    importMultipleSheetsAssertions(ExcelImplementation.POI)
    importMultipleSheetsAssertions(ExcelImplementation.FastExcel)
  }

  void importMultipleSheetsAssertions(ExcelImplementation excelImplementation) {

      Map<String, Matrix> sheets = SpreadsheetImporter.importSpreadsheets("Book2.xlsx",
          [
              [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 6, firstRowAsColNames: true],
              [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true],
              ['key': 'comp', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: false],
              ['key': 'comp2', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: true]
          ], excelImplementation)
      assertEquals(4, sheets.size())
      Matrix table2 = sheets.Sheet2
      assertEquals(3i, table2[2, 0, Integer])
      def date = table2[6, 2]
      assertEquals(LocalDate.parse("2023-05-06"), date)
      assertEquals(17.4, table2['baz'][table2.rowCount()-1])
      assertEquals(['id', 'foo', 'bar', 'baz'], table2.columnNames())

      Matrix table1 = sheets.Sheet1
      //println(table1.content())
      //println(table1.types())
      assertEquals(710381i, table1[0,0, Integer])
      assertEquals(103599.04, table1[1,1])
      assertEquals(66952.95, table1[2,2])
      assertEquals(0.0G, table1[3,3, BigDecimal].setScale(1))
      assertEquals(-0.00982d, table1[6, 'percentdiff'] as double, 0.00001)

      Matrix comp = sheets.comp.clone()
      assertEquals('Component', comp[0,0])
      assertEquals(5, comp.rowCount())
      assertEquals(32, comp.columnCount())
      assertEquals(31, comp[0,31, Integer])

      def headerRow = comp.row(0)
      List<String> names = headerRow.collect{
        if (it == "Component") {
          String.valueOf(it)
        } else {
          String.valueOf(ValueConverter.asDouble(it).intValue())
        }
      }
      assertEquals(32, names.size())
      comp.columnNames(names)
      comp.removeRows(0)
      comp = comp.convert([String] + [Integer]*31 as List<Class<?>>)
      assertEquals(2043, comp[0,1])
      assertEquals(2790, comp[3,31])

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
}
