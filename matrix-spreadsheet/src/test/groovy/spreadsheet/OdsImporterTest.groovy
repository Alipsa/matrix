package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.sods.SOdsImporter

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals

class OdsImporterTest {

  @Test
  void testOdsImport() {
    def table = SpreadsheetImporter.importSpreadsheet(file: "Book1.ods", endRow: 12, endCol: 4, firstRowAsColNames: true)
    table = table.convert(id: Integer, bar: LocalDate, baz: BigDecimal, 'yyyy-MM-dd HH:mm:ss.SSS')
    //println(table.content())
    assertEquals(3, table[2, 0])
    assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    assertEquals(17.4, table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void TestImportWithColnames() {
    def table = SpreadsheetImporter.importSpreadsheet(
        "file": "Book1.ods",
        "endRow": 12,
        "startCol": 'A',
        "endCol": 'D'
    )
    //println(table.content())
    assertEquals(3.0, table[2, 0])
    assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    assertEquals(17.4, table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void testImportFromStream() {
    try(InputStream is = this.getClass().getResourceAsStream("/Book1.ods")) {
      Matrix table = SOdsImporter.importOds(
          is, 'Sheet1', 1, 12, 'A', 'D', true
      )
      assertEquals(3.0, table[2, 0])
      def date = table[6, 2]
      assertEquals(LocalDate.parse("2023-05-06"), date)
      assertEquals(17.4, table['baz'][table.rowCount()-1])
      assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
    }
  }

  @Test
  void testImportMultipleSheets() {
    try(InputStream is = this.getClass().getResourceAsStream("/Book2.ods")) {
      Map<String, Matrix> sheets = SOdsImporter.importOdsSheets(is,
       [
           [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 6, firstRowAsColNames: true],
           [sheetName: 'Sheet2', startRow: 2, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: false],
           ['key': 'comp', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: false],
           ['key': 'comp2', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: true]
       ])
      assertEquals(4, sheets.size())
      Matrix table2 = sheets.Sheet2
      table2.columnNames(['id', 'foo', 'bar', 'baz'])
      assertEquals(3.0, table2[2, 0])
      def date = table2[6, 2, LocalDate]
      assertEquals(LocalDate.parse("2023-05-06"), date)
      assertEquals(17.4, table2['baz'][table2.rowCount()-1])
      assertEquals(['id', 'foo', 'bar', 'baz'], table2.columnNames())

      Matrix table1 = sheets.Sheet1
      assertEquals(710381, table1[0,0, Integer])
      assertEquals(103599.04, table1[1,1])
      assertEquals(66952.95, table1[2,2, Double])
      assertEquals(0.0G, table1[3,3, BigDecimal])
      assertEquals(-0.00982, table1[6, 'percentdiff'], 0.00001)
      assertIterableEquals(['id',	'OB',	'IB',	'deferred_interest_amount', 'percentdiff'], table1.columnNames())

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

  @Test
  void testFormulas() {
    URL file = this.getClass().getResource("/Book3.ods")
    Matrix m = SOdsImporter.importOds(file, 1, 2, 8, 'A', 'G', false)
        .convert('c3': LocalDate)
    println m.content()
    assertEquals(21, m[6, 0, Integer])
    assertEquals(LocalDate.parse('2025-03-20'), m[5, 2])
    assertEquals(m['c4'].subList(0..5).average() as double, m[6,'c4'] as double, 0.000001)
    assertEquals('foobar1', m[0, 'c7'])
  }
}
