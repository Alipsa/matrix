import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import static se.alipsa.matrix.core.ValueConverter.asLocalDate

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.time.LocalDate

class RowTest {

  @Test
  void testMinus() {
    def empData = Matrix.builder()
        .matrixName('empData')
        .data(
            emp_id: 1..5,
            emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27')
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

    assertIterableEquals([2, 'Dan', 515.2], row.subList('emp_id', 'emp_name', 'salary'))
    assertIterableEquals([2, 'Dan', 515.2], row['emp_id', 'emp_name', 'salary'])
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

    assertEquals('Dont know what to do with 2 parameters ([0, b]) to getAt()', ex.message)
  }

  @Test
  void testGetAtReturnsNullForNullCells() {
    Matrix table = Matrix.builder()
        .columns(id: [1, null], name: ['Rick', null])
        .types(Integer, String)
        .build()

    Row row = table.row(1)

    assertNull(row[0])
    assertNull(row[0 as Number])
    assertNull(row['name'])
  }

  @Test
  void testPutAtRejectsMissingColumnName() {
    Matrix table = Matrix.builder()
        .columns(id: [1], name: ['Rick'])
        .types(Integer, String)
        .build()

    Row row = table.row(0)

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      row['salary'] = 99
    }

    assertEquals('Failed to find a column with the name salary', ex.message)
    assertEquals([1, 'Rick'], row)
  }

  @Test
  void testEqualsComparesContentNotIdentity() {
    Matrix table1 = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()
    Matrix table2 = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()

    Row row1 = table1.row(0)
    Row row2 = table2.row(0)

    assertNotSame(row1, row2)
    assertEquals(row1, row2)
    assertEquals(row1.hashCode(), row2.hashCode())
  }

  @Test
  void testEqualsReturnsFalseForDifferentContent() {
    Matrix table = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()

    assertNotEquals(table.row(0), table.row(1))
  }

  @Test
  void testEqualsAgainstPlainList() {
    Matrix table = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()

    Row row = table.row(0)

    assertEquals([1, 'Rick'], row)
    assertEquals(row, [1, 'Rick'])
    assertNotEquals(row, [1, 'Someone Else'])
    assertNotEquals(row, 'not a list at all')
  }

  @Test
  void testEqualsKeepsSetCellsDistinctFromListCells() {
    Matrix listMatrix = Matrix.builder().data(value: [[1, 2]]).types(Object).build()
    Matrix firstSetMatrix = Matrix.builder().data(value: [[1, 2] as LinkedHashSet]).types(Object).build()
    Matrix secondSetMatrix = Matrix.builder().data(value: [[2, 1] as LinkedHashSet]).types(Object).build()

    assertNotEquals(listMatrix.row(0), firstSetMatrix.row(0))
    assertEquals(firstSetMatrix.row(0), secondSetMatrix.row(0))
    assertEquals(firstSetMatrix.row(0).hashCode(), secondSetMatrix.row(0).hashCode())
  }

}
