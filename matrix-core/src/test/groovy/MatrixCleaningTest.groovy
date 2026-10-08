import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

class MatrixCleaningTest {

  @Test
  void testNullSelectionAndCopies() {
    def matrix = Matrix.builder('source').data(id: [1, 2, 3, 4], price: [null, 3, 4, 5],
        country: ['SE', null, '', 'NO']).types(Integer, Integer, String).build()
    matrix.createIndex('id')
    matrix.withoutNullRows()
    assertEquals(4, matrix.rowCount())
    def all = matrix.withoutNullRows()
    assertIterableEquals([3, 4], all['id'])
    def selected = matrix.withoutNullRows(['price'])
    assertIterableEquals([2, 3, 4], selected['id'])
    assertIterableEquals(matrix.types(), selected.types())
    assertEquals('source', selected.matrixName)
    assertIterableEquals(['id'], selected.indexedColumns())
    selected[0, 'id'] = 99
    assertEquals(2, matrix[1, 'id'])
    [[], ['absent']].each { names ->
      assertThrows(IllegalArgumentException) { matrix.withoutNullRows(names) }
    }
    assertThrows(IllegalArgumentException) { matrix.withoutNullRows((List<String>) null) }
    def special = Matrix.builder().data(a: ['', Double.NaN, Float.POSITIVE_INFINITY, null]).build()
    assertEquals(3, special.withoutNullRows().rowCount())
    def empty = Matrix.builder('empty').data(a: [null]).types(Integer).build()
    empty.createIndex('a')
    def cleaned = empty.withoutNullRows()
    assertEquals(0, cleaned.rowCount())
    assertIterableEquals([Integer], cleaned.types())
    assertIterableEquals(['a'], cleaned.indexedColumns())
  }

  @Test
  void testFillTypesAndIndexes() {
    def matrix = Matrix.builder('fill').data(a: [null, 2], b: [null, 'x']).types(Integer, String).build()
    matrix.createIndex('a')
    matrix.lookup((Object) null) // Build the original index before copying.
    def filled = matrix.fillNulls([a: 1L, b: 'unknown'])
    assertEquals(Number, filled.type('a'))
    assertEquals(String, filled.type('b'))
    assertEquals(1L, filled.column('a').get(0))
    assertEquals('unknown', filled[0, 'b'])
    assertEquals(1, filled.lookup(1L).rowCount())
    assertEquals(1, matrix.lookup((Object) null).rowCount())
    assertEquals('fill', filled.matrixName)
    assertNull(matrix.column('a').get(0))
    assertEquals(Object, matrix.fillNulls([a: '5']).type('a'))
    assertEquals(Integer, matrix.fillNulls([a: null]).type('a'))
    def copy = matrix.fillNulls([:])
    copy[1, 'a'] = 9
    assertEquals(2, matrix[1, 'a'])
    assertThrows(IllegalArgumentException) { matrix.fillNulls(null) }
    assertThrows(IllegalArgumentException) { matrix.fillNulls([missing: 3]) }
    def noNull = Matrix.builder().data(a: [1]).types(Integer).build()
    assertEquals(Integer, noNull.fillNulls([a: 'ignored']).type('a'))
    def unconstrained = Matrix.builder().data(a: [null], b: [null]).types(Object, Object).build()
    unconstrained.column('b').type = null
    assertIterableEquals([Object, null], unconstrained.fillNulls([a: 1, b: 'x']).types())
  }

  @Test
  void testDuplicateKeysAndCopies() {
    def matrix = Matrix.builder('events').data(id: [1, 1L, 1.0, '1', '1' as Character,
        null, null, Double.NaN, Float.NaN, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 9007199254740992L, 9007199254740993L],
        source: ['a'] * 15).types(Object, String).build()
    matrix.createIndex('source')
    assertIterableEquals([false, true, true, false, false, false, true, false, true,
                          false, true, false, true, false, false], matrix.duplicated(['id']))
    assertEquals(matrix.duplicated(), matrix.duplicated(['source', 'id']))
    matrix.withoutDuplicateRows()
    assertEquals(15, matrix.rowCount())
    def distinct = matrix.withoutDuplicateRows(['id'])
    assertEquals(9, distinct.rowCount())
    assertEquals('events', distinct.matrixName)
    assertIterableEquals(matrix.types(), distinct.types())
    assertIterableEquals(['source'], distinct.indexedColumns())
    assertEquals(9, distinct.lookup('a').rowCount())
    distinct.column('id').set(0, 99)
    assertEquals(1, matrix.column('id').get(0))
    [[], ['absent']].each { names ->
      assertThrows(IllegalArgumentException) { matrix.duplicated(names) }
      assertThrows(IllegalArgumentException) { matrix.withoutDuplicateRows(names) }
    }
  }

  @Test
  void testEmptyDuplicateAndValueEquality() {
    def empty = Matrix.builder('empty').columnNames(['id']).types(Integer).build()
    empty.createIndex('id')
    assertEquals([], empty.duplicated())
    assertIterableEquals([Integer], empty.withoutDuplicateRows().types())
    assertIterableEquals(['id'], empty.withoutDuplicateRows().indexedColumns())
    def zeroColumns = Matrix.builder().rows([[], [], []]).build()
    assertIterableEquals([false, true, true], zeroColumns.duplicated())
    assertEquals(1, zeroColumns.withoutDuplicateRows().rowCount())
    def values = Matrix.builder().data(a: [new Date(0), new Date(0), new Date(1)]).build()
    assertIterableEquals([false, true, false], values.duplicated())
  }
}
