package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelUtil

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ValueConverter.asBigDecimal
import static se.alipsa.matrix.core.ValueConverter.asDouble

class FExcelImporterTest {

  @Test
  void testExcelImport() {
    //println 'url is ' + this.class.getResource("/Book1.xlsx")
    def table = FExcelImporter.create().importSpreadsheet(
        this.class.getResource("/Book1.xlsx"), 'Sheet1',
        1, 12,
        1, 4,
        true
    )

    table = table.convert(id: Integer, bar: LocalDate, baz: BigDecimal)
    //println(table.content())
    assertEquals(3, table[2, 0])
    assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    assertEquals(17.4, table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void TestImportWithColnames() {
    def table = FExcelImporter.create().importSpreadsheet(
        this.class.getResource("/Book1.xlsx"),
        1,
        1, 12,
        'A', 'D',
        true
    )
    //println(table.content())
    assertEquals(3.0d, table[2, 0])
    assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    assertEquals(17.4, table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void testImportFromStream() {
    try(InputStream is = this.getClass().getResourceAsStream("/Book1.xlsx")) {
      Matrix table = FExcelImporter.create().importSpreadsheet(
          is, 'Sheet1', 1, 12, 'A', 'D', true
      )
      assertEquals(3.0d, table[2, 0])
      def date = table[6, 2]
      assertEquals(LocalDate.parse("2023-05-06"), date)
      assertEquals(17.4, table['baz'][table.rowCount()-1])
      assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
    }
  }

  @Test
  void testImportMultipleSheets() {
    try(InputStream is = this.getClass().getResourceAsStream("/Book2.xlsx")) {
      Map<String, Matrix> sheets = FExcelImporter.create().importSpreadsheets(is,
          [
              [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 6, firstRowAsColNames: true],
              [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true],
              ['key': 'comp', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: false],
              ['key': 'comp2', sheetName: 'Sheet2', startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: true]
          ])
      assertEquals(4, sheets.size())
      Matrix table2 = sheets.Sheet2
      assertEquals(asBigDecimal(3), table2[2, 0])
      def date = table2[6, 2]
      assertEquals(LocalDate.parse("2023-05-06"), date)
      assertEquals(17.4 as Double, table2['baz'][table2.rowCount()-1] as Double, 0.000001)
      assertEquals(['id', 'foo', 'bar', 'baz'], table2.columnNames())

      Matrix table1 = sheets.Sheet1
      //println(table1.content())
      //println(table1.types())
      assertEquals(asBigDecimal(710381), table1[0,0])
      assertEquals(103599.04 as Double, table1[1,1, Double], 0.000001)
      assertEquals(66952.95, table1[2,2])
      assertEquals(asBigDecimal(0), table1[3,3, BigDecimal])
      assertEquals(-0.00982, table1[6, 'percentdiff'] as BigDecimal, 0.00001)

      Matrix comp = sheets.comp.clone()
      //println comp.content()
      assertEquals('Component', comp[0,0])
      assertEquals(5, comp.rowCount())
      assertEquals(32, comp.columnCount())
      assertEquals(31, comp[0,31, Integer])

      def headerRow = comp.row(0)
      List<String> names = headerRow.collect{
        if (it == "Component") {
          String.valueOf(it)
        } else {
          String.valueOf(asDouble(it).intValue())
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
          String.valueOf(asDouble(it).intValue())
        }
      })
      assertIterableEquals(comp.columnNames(), comp2.columnNames(), "column names differ")
      comp2 = comp2.convert([String] + [Integer]*31 as List<Class<?>>)
      assertIterableEquals(comp.typeNames(), comp2.typeNames(), "column types differ after column name setting")

    }
  }

  @Test
  void testGetFormatInfo() {
    URL url = this.getClass().getResource("/Book2.xlsx")
    Map<String, ?> info = FExcelUtil.getFormat(url, 'Sheet1', 'C', 4)
    //assertEquals(165, info.formatId)
    assertEquals('[$$-409]#,##0.00;\\-[$$-409]#,##0.00', info.formatString)
    assertEquals('19632.75', info.rawValue)
  }

  @Test
  void testFormulas() {
    URL file = this.getClass().getResource("/Book3.xlsx")
    Matrix m = FExcelImporter.create().importSpreadsheet(file, 1, 2, 8, 'A', 'G', false)
        .convert('c3': LocalDate)
    //println m.content()
    //println FExcelUtil.getFormat(file, 'Sheet1', 'G', 2)
    assertEquals(21, m[6, 0, Integer])
    assertEquals(LocalDate.parse('2025-03-20'), m[5, 2])
    assertEquals(m['c4'].subList(0..5).average() as double, m[6,'c4'] as double, 0.000001)
    assertEquals('foobar1', m[0, 'c7'])
  }

  @Test
  void testInvalidSheetNumberThrows() {
    URL url = this.getClass().getResource("/Book1.xlsx")

    // Sheet number 0 should throw
    assertThrows(IllegalArgumentException.class, () -> {
      FExcelImporter.create().importSpreadsheet(url, 0, 1, 12, 'A', 'D', true)
    }, "Sheet number 0 should throw IllegalArgumentException")

    // Negative sheet number should throw
    assertThrows(IllegalArgumentException.class, () -> {
      FExcelImporter.create().importSpreadsheet(url, -1, 1, 12, 'A', 'D', true)
    }, "Negative sheet number should throw IllegalArgumentException")

    // Non-existent sheet number should throw
    assertThrows(IllegalArgumentException.class, () -> {
      FExcelImporter.create().importSpreadsheet(url, 999, 1, 12, 'A', 'D', true)
    }, "Non-existent sheet number should throw IllegalArgumentException")
  }

  @Test
  void testInvalidSheetNameThrows() {
    URL url = this.getClass().getResource("/Book1.xlsx")

    // Non-existent sheet name should throw (NoSuchElementException from orElseThrow)
    assertThrows(NoSuchElementException.class, () -> {
      FExcelImporter.create().importSpreadsheet(url, 'NonExistentSheet', 1, 12, 'A', 'D', true)
    }, "Non-existent sheet name should throw NoSuchElementException")
  }

  @Test
  void testNullUrlThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      FExcelImporter.create().importSpreadsheet((URL) null, 1, 1, 12, 'A', 'D', true)
    }, "Null URL should throw IllegalArgumentException")
  }
}
