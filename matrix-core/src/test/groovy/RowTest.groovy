import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.time.LocalDate

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
}
