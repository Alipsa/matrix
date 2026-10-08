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


  @Test
  void testSublistAssignmentBounds() {
    def matrix = Matrix.builder().data(a: [1], b: [2], c: [3]).build()
    def row = matrix.row(0)
    def slice = row.subList(1, 2)
    [-1, 1].each { index ->
      assertThrows(IndexOutOfBoundsException) { slice.set(index, 99) }
      assertIterableEquals([1, 2, 3], matrix.row(0))
      assertIterableEquals([1, 2, 3], row)
      assertIterableEquals([2], slice)
    }
    assertThrows(IndexOutOfBoundsException) { row.subList(1, 1).set(0, 99) }
    def nested = row.subList(0, 3).subList(1, 2)
    assertThrows(IndexOutOfBoundsException) { nested.set(1, 99) }
    assertEquals(2, nested.set(0, 4))
    assertEquals(4, matrix[0, 'b'])
  }


  @Test
  void testStaleWritesAndSnapshotReads() {
    def mutations = [
        { m -> m.addRow([3, 30]) }, { m -> m.addRow(0, [3, 30]) },
        { m -> m.removeRows(0) }, { m -> m.removeRows(1) },
        { m -> m.moveRow(0, 1) }, { m -> m.orderBy('a') },
        { m -> m.addColumn('c', Integer, [3, 4]) },
        { m -> m.addColumn('c', Integer, 0, [3, 4]) },
        { m -> m.moveColumn('b', 0) }, { m -> m.drop('b') },
        { m -> m.dropExcept('b') }, { m -> m.dropExcept(1) },
        { m -> m.putAt('c', Integer, [3, 4]) },
        { m -> m.putAt(2, 0, 3) }]
    mutations.each { mutate ->
      def matrix = Matrix.builder().data(a: [2, 1], b: [20, 10]).types(Integer, Integer).build()
      def row = matrix.row(1)
      def slice = row.subList(0, 1)
      mutate(matrix)
      def before = matrix.columns().collect { new ArrayList(it) }
      assertThrows(ConcurrentModificationException) { row.set(0, 99) }
      assertThrows(ConcurrentModificationException) { slice.set(0, 99) }
      def iterator = row.listIterator()
      iterator.next()
      assertThrows(ConcurrentModificationException) { iterator.set(99) }
      assertEquals(before, matrix.columns())
      assertIterableEquals([1, 10], row)
      assertIterableEquals([1], slice)
      row.detach()
      row.set(0, 9)
      assertEquals(before, matrix.columns())
      matrix.row(0).set(0, 8)
      assertEquals(8, matrix[0, 0])
    }
  }

  @Test
  void testUntrackedColumnBoundsAndRejectionAtomicity() {
    def matrix = Matrix.builder().data(a: [1, 2], b: [3, 4]).build()
    def row = matrix.row(1)
    def slice = row.subList(0, 1)
    matrix.column('a').remove(1)
    assertThrows(IndexOutOfBoundsException) { row.set(0, 99) }
    assertThrows(IndexOutOfBoundsException) { slice.set(0, 99) }
    assertEquals(2, row[0])
    assertEquals(2, slice[0])
    assertIterableEquals([1], matrix['a'])
    // An in-range direct Column reorder intentionally cannot be detected.
    def kept = matrix.row(0)
    matrix.column('b').reverse(true)
    kept.set(1, 8)
    assertEquals(8, matrix[0, 1])
  }

  @Test
  void testSnapshotMetadataAndSafeAssignments() {
    def matrix = Matrix.builder().data(a: [1], b: [2]).types(Integer, Integer).build()
    def rows = matrix.rows()
    def row = rows[0]
    def originalHash = row.hashCode()
    def originalText = row.toString()
    def set = new HashSet([row])
    def map = new HashMap([(row): 'found'])
    matrix[0, 0] = 5
    assertEquals(1, row[0])
    assertEquals(5, matrix.row(0)[0])
    assertEquals(originalHash, row.hashCode())
    assertEquals(originalText, row.toString())
    assertEquals('found', map.get(row))
    assertEquals(true, set.contains(row))
    matrix.rename('a', 'bTemp').rename('b', 'a').rename('bTemp', 'b')
    row.a = 7
    assertEquals(7, matrix[0, 'b'])
    row['b'] = 8
    assertEquals(8, matrix[0, 'a'])
    matrix.row(0)['a'] = 9
    assertEquals(9, matrix[0, 1])
    matrix.replace('b', String, ['new'])
    row.subList(0, 1).set(0, 10)
    assertEquals(10, matrix.column('b').get(0))
    def iterator = row.listIterator()
    iterator.next()
    iterator.set(11)
    assertEquals(11, matrix.column('b').get(0))
    assertIterableEquals(['a', 'b'], row.columnNames())
    assertIterableEquals([Integer, Integer], row.types())
    [row.columnNames(), row.types()].each { metadata ->
      assertThrows(UnsupportedOperationException) { metadata.set(0, null) }
      assertThrows(UnsupportedOperationException) { metadata.add(null) }
      assertThrows(UnsupportedOperationException) { metadata.remove(0) }
      assertThrows(UnsupportedOperationException) { metadata.clear() }
    }
    def copy = row as List
    copy.set(0, 99)
    copy.add(100)
    copy.remove(0)
    copy.clear()
    assertEquals(11, row[0])
    assertEquals(11, matrix.column(0).get(0))
  }

  @Test
  void testRetainedNameAfterRenameAndSharedMetadata() {
    def matrix = Matrix.builder().data(a: [1, 2]).types(Integer).build()
    def rows = matrix.rows()
    assertEquals(true, rows[0].columnNames().is(rows[1].columnNames()))
    assertEquals(true, rows[0].types().is(rows[1].types()))
    matrix.rename('a', 'b')
    rows[0].a = 3
    rows[0]['a'] = 4
    assertEquals(4, matrix[0, 'b'])
    assertThrows(IllegalArgumentException) { rows[0]['b'] = 9 }
    matrix.row(0)['b'] = 5
    assertEquals(5, matrix[0, 0])
    matrix.addColumn('c', [8, 9])
    assertEquals(4, rows[0][0])
    assertIterableEquals(['a'], rows[1].columnNames())
    def rebuilt = Matrix.builder().rowList(rows).build()
    assertIterableEquals(['a'], rebuilt.columnNames())
    assertIterableEquals([4, 2], rebuilt['a'])
  }

  @Test
  void testRowWritesRefreshIndexesAndParentChangesKeepHashKeys() {
    def matrix = Matrix.builder().data(a: [1, 2], b: [3, 4]).types(Integer, Integer).build()
    matrix.createIndex('a')
    assertEquals(1, matrix.lookup(1).rowCount())
    def row = matrix.row(0)
    row.set(0, 5)
    assertEquals(0, matrix.lookup(1).rowCount())
    assertEquals(1, matrix.lookup(5).rowCount())
    def hash = row.hashCode()
    def text = row.toString()
    def keys = new HashSet([row])
    def map = new HashMap([(row): 'found'])
    def equalSnapshot = new ArrayList(row)
    def hits = matrix.findAll { it.a > 1 }
    matrix.addColumn('c', [6, 7])
    assertEquals(hash, row.hashCode())
    assertEquals(text, row.toString())
    assertEquals(true, row == equalSnapshot)
    assertEquals(true, keys.contains(row))
    assertEquals('found', map.get(row))
    assertEquals([5, 2], hits*.a)
    def cell = [1]
    def shared = Matrix.builder().data(a: [cell]).build().row(0)
    def copy = shared.asType(List)
    copy[0].add(2)
    assertEquals([1, 2], shared[0])
    def sorted = Matrix.builder().data(a: ['a', 'b']).build()
    sorted.orderBy('a', Matrix.DESC)
    assertIterableEquals(['b', 'a'], sorted['a'])
  }
}
