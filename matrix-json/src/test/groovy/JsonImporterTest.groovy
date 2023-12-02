import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrixjson.*

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.groovy.matrix.ListConverter.toLocalDates

class JsonImporterTest {

  @Test
  void testJsonImport() {
    def importer = new JsonImporter()
    def table = importer.parse('''[
        {
          "emp_id": 1,
          "emp_name": "Rick",
          "salary": 623.3,
          "start_date": "2012-01-01"
        },
        {
          "emp_id": 2,
          "emp_name": "Dan",
          "salary": 515.2,
          "start_date": "2013-09-23"
        },
        {
          "emp_id": 3,
          "emp_name": "Michelle",
          "salary": 611.0,
          "start_date": "2014-11-15"
        }
    ]''').convert([int, String, Number, LocalDate])

    Matrix empData = new Matrix(
        emp_id: 1..3,
        emp_name: ["Rick","Dan","Michelle"],
        salary: [623.3,515.2,611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"),
        [int, String, Number, LocalDate]
    )

    assertEquals(empData, table, empData.diff(table))
  }
}
