package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.spreadsheet.excel.ExcelImporter
import se.alipsa.groovy.spreadsheet.ods.OdsImporter

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

    @Test
    void testImportMultipleSheets() {
        try(InputStream is = this.getClass().getResourceAsStream("/Book2.xlsx")) {
            Map<String, Matrix> sheets = ExcelImporter.importExcelSheets(is,
                [
                    [sheetName: 'Sheet1', startRow: 3, endRow: 11, startCol: 2, endCol: 5, firstRowAsColNames: true],
                    [sheetName: 'Sheet2', startRow: 1, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: true]
                ])
            assertEquals(2, sheets.size())
            Matrix table2 = sheets.Sheet2
            assertEquals(3.0, table2[2, 0])
            def date = table2[6, 2]
            assertEquals(LocalDateTime.parse("2023-05-06T00:00"), date)
            assertEquals(17.4, table2['baz'][table2.rowCount()-1])
            assertEquals(['id', 'foo', 'bar', 'baz'], table2.columnNames())

            Matrix table1 = sheets.Sheet1
            assertEquals(710381, table1[0,0])
            assertEquals(103599.04, table1[1,1])
            assertEquals(66952.95, table1[2,2])
            assertEquals(0.0G, table1[3,3, BigDecimal])
        }
    }
}
