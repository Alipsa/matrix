import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import static se.alipsa.matrix.core.ValueConverter.asLocalDate

class RowTest {

  @Test
  void testMinus() {
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

    Row row = empData.row(1)
    List minusRow = row - 'salary'
    assert [2, 'Dan', asLocalDate('2013-09-23')] == minusRow
    minusRow = row - 0
    assert ['Dan', 515.2, asLocalDate('2013-09-23')] == minusRow
  }

  @Test
  void testGetAtWithStringCollection() {
    Matrix empData = Matrix.builder()
        .matrixName('empData')
        .data(
            emp_id: 1..2,
            emp_name: ['Rick', 'Dan'],
            salary: [623.3, 515.2],
            start_date: toLocalDates('2012-01-01', '2013-09-23')
        )
        .types([int, String, Number, LocalDate])
        .build()

    Row row = empData.row(1)

    assertIterableEquals([2, 'Dan', 515.2], row[['emp_id', 'emp_name', 'salary']])
  }

  @Test
  void testGetAtRejectsMixedCollectionTypes() {
    Matrix empData = Matrix.builder()
        .data(a: [1], b: ['x'])
        .build()

    Row row = empData.row(0)

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      row[[0, 'b']]
    }

    assertEquals("Dont know what to do with 2 parameters ([0, b]) to getAt()", ex.message)
  }
}
