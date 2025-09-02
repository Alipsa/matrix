package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsExporter
import se.alipsa.matrix.gsheets.GsImporter
import se.alipsa.matrix.gsheets.GsUtil

import java.time.LocalDate

import static se.alipsa.matrix.core.ListConverter.toLocalDates

class GsTest {

  def empData = Matrix.builder()
      .matrixName('empData')
      .data(
          emp_id: 1..5,
          emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
          salary: [623.3, 515.2, 611.0, 729.0, 843.25],
          start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
      )
      .types([Integer, String, BigDecimal, LocalDate])
      .build()

  @Test
  void testExport() {
    String spreadsheetId = GsExporter.exportSheet(empData)

    println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
    GsUtil.deleteSheet(spreadsheetId)
  }

  @Test
  void testExportImport() {
    String spreadsheetId = GsExporter.exportSheet(empData)

    println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
    Matrix m = GsImporter.importSheet(spreadsheetId, "empData!A1:D6", true).withMatrixName(empData.matrixName)
    assertTrue(empData.equals(m, true, true, true))
    println "Data is basically correct"
    m.convert('emp_id': Integer,
        'emp_name': String,
        'salary': BigDecimal,
        'start_date': LocalDate)
    println m.typeNames()
    m.row(0).each { println "$it $it.class.simpleName" }
    assert empData == m
    GsUtil.deleteSheet(spreadsheetId)
  }
}
