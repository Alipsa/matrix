package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetExporter
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static se.alipsa.matrix.core.ListConverter.toLocalDateTimes
import static se.alipsa.matrix.core.ListConverter.toLocalDates

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SpreadsheetTest {

  @Test
  void testSpreadsheet() {
    def matrix = [
        id: [1,2,3,4,-5],
        name: ['foo', 'bar', 'baz', 'bla', 'que'],
        start: toLocalDates('2021-01-04', '2021-02-24', '2023-03-13', '2024-04-15', '2025-05-20'),
        end: toLocalDateTimes(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), '2021-02-04 12:01:22', '2022-03-12 13:14:15', '2023-04-13 15:16:17', '2023-05-11 16:17:18', '2025-06-20 17:18:19'),
        measure: [12.45, 11.45, 14.11, 15.23, 10.99],
        active: [true, false, false, true, false]
    ]
    Matrix table = Matrix.builder("table")
        .data(matrix)
        .types(int, String, LocalDate, LocalDateTime, BigDecimal, Boolean)
        .build()
    def file = new File("build/table.xlsx")
    if (file.exists()) file.delete()
    SpreadsheetExporter.exportSpreadsheet(file, table)

    Matrix m2 = SpreadsheetImporter.importSpreadsheet([
        file: file,
        sheet: 'table',
        endRow: 33,
        endCol: 'F',
        firstRowAsColNames: true]
    ).convert([int, String, LocalDate, LocalDateTime, BigDecimal, Boolean])
    assertEquals(table, m2)
  }
}
