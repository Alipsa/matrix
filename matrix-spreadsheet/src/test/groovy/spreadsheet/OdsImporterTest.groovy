package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.groovy.spreadsheet.SpreadsheetImporter

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.assertEquals

class OdsImporterTest {

  @Test
  void testOdsImport() {
    def table = SpreadsheetImporter.importSpreadsheet(file: "Book1.ods", endRow: 12, endCol: 4, firstRowAsColNames: true)
    table = table.convert(id: Integer, bar: LocalDate, baz: BigDecimal, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss.SSS'))
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
    assertEquals("3.0", table[2, 0])
    def date = table[6, 2]
    assertEquals("2023-05-06 00:00:00.000", date)
    assertEquals("17.4", table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }
}
