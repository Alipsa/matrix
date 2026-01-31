package test.alipsa.matrix.gsheets

import se.alipsa.matrix.gsheets.GsheetsReader
import se.alipsa.matrix.gsheets.GsheetsWriter

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsUtil
import org.junit.jupiter.api.Tag

import java.time.LocalDate

import static se.alipsa.matrix.core.ListConverter.toLocalDates

@Tag("external")
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

    String spreadsheetId = GsheetsWriter.write(empData)

    //println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
    GsUtil.deleteSheet(spreadsheetId)
  }

  @Test
  void testExportImport() {
    String spreadsheetId = GsheetsWriter.write(empData)

    //println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
    Matrix m = GsheetsReader.readAsStrings(spreadsheetId, "empData!A1:D6", true).withMatrixName(empData.matrixName)
    assertTrue(empData.equals(m, true, true, true))
    //println "Data is basically correct"
    m.convert('emp_id': Integer,
        'emp_name': String,
        'salary': BigDecimal,
        'start_date': LocalDate)
    //println m.typeNames()
    //m.row(0).each { println "$it $it.class.simpleName" }
    assert empData == m
    GsUtil.deleteSheet(spreadsheetId)
  }

  @Test
  void testImportWithNull() {
    Matrix ed = empData.clone()
    ed[2,'salary'] = null
    ed[4,'emp_id'] = null

    //println ed.withMatrixName('original').content()
    String spreadsheetId = GsheetsWriter.write(ed.withMatrixName('empData'))

    println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
    Matrix m = GsheetsReader.readAsStrings(spreadsheetId, "empData!A1:D6", true).withMatrixName(empData.matrixName)
    //println m.withMatrixName('imported').content()
    assertTrue(ed.equals(m, true, true, true))
    //println "Data is basically correct"
    m.convert('emp_id': Integer,
        'emp_name': String,
        'salary': BigDecimal,
        'start_date': LocalDate)
    //println m.typeNames()
    //m.row(0).each { println "$it $it.class.simpleName" }
    assert ed == m
    GsUtil.deleteSheet(spreadsheetId)
  }

  @Test
  void testImportAsObjectWithNull() {
    Matrix ed = empData.clone().withMatrixName('empData')
    ed[2,'salary'] = null
    ed[4,'emp_id'] = null

    //println ed.withMatrixName('original').content()
    String spreadsheetId = GsheetsWriter.write(ed, false)
    //println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
    Matrix m = GsheetsReader
        .readAsObject(spreadsheetId, "empData!A1:D6", true)
        .withMatrixName(empData.matrixName)
    //println m.withMatrixName('imported').content()
    m.moveValue(2, 'start_date', 'salary')
    m.moveValue(4, 'start_date', 'emp_id')
    //println m.withMatrixName('imported').content()
    assertTrue(ed.equals(m, true, true, true))
    //println "Data is basically correct"
    m.convert('emp_id': Integer,
        'emp_name': String,
        'salary': BigDecimal,
        'start_date': LocalDate)
    //println m.typeNames()
    //m.row(0).each { println "$it $it.class.simpleName" }
    assert ed == m
    GsUtil.deleteSheet(spreadsheetId)
  }
}
