package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsExporter

import java.time.LocalDate

import static se.alipsa.matrix.core.ListConverter.toLocalDates

class GsExporterTest {

  @Test
  void testExport() {
    def empData = Matrix.builder()
        .matrixName('empData')
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        )
        .types([int, String, Number, LocalDate])
        .build()
    String spreadsheetId = GsExporter.exportSheet(empData)
    println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
  }
}
