package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.spreadsheet.excel.ExcelImporter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import java.time.LocalDate

import static se.alipsa.groovy.spreadsheet.SpreadsheetImporter.*
import static org.junit.jupiter.api.Assertions.*

class ExcelImporterTest {

    @Test
    void testExcelImport() {
        def table = importSpreadsheet(file: "Book1.xlsx", endRow: 12, endCol: 4, firstRowAsColNames: true)
        table = table.convert(id: Integer, bar: LocalDate, baz: BigDecimal, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss.SSS'))
        //println(table.content())
        assertEquals(3, table[2, 0])
        assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
        assertEquals(17.4, table['baz'][table.rowCount()-1])
        assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
    }

    @Test
    void TestImportWithColnames() {
        def table = importSpreadsheet(
                "file": "Book1.xlsx",
                "endRow": 12,
                "startCol": 'A',
                "endCol": 'D'
        )
        //println(table.content())
        assertEquals(3.0d, table[2, 0])
        assertEquals(LocalDateTime.parse("2023-05-06T00:00:00.000"), table[6, 2])
        assertEquals(17.4, table['baz'][table.rowCount()-1])
        assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
    }

    @Test
    void testImportFromStream() {
        try(InputStream is = this.getClass().getResourceAsStream("/Book1.xlsx")) {
            Matrix table = ExcelImporter.importExcel(
                is, 'Sheet1', 1, 12, 'A', 'D', true
            )
            assertEquals(3.0d, table[2, 0])
            def date = table[6, 2]
            assertEquals(LocalDateTime.parse("2023-05-06T00:00:00.000"), date)
            assertEquals(17.4, table['baz'][table.rowCount()-1])
            assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
        }
    }
}
