import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.sql.Sql

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Converter
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.Stat

class Polish390Test {

  // ── 1. Column lookup hardening ─────────────────────────────────────────────

  @Test
  void unknownColumnNamesAreRejectedWithClearMessage() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4]).types(int, int).build()

    assertLookupFails { m.type('zzz') }
    assertLookupFails { m[0, 'zzz'] }
    assertLookupFails { m.getAt(0, 'zzz', String) }
    assertLookupFails { m.getAt(0, 'zzz', String, 'x') }
    assertLookupFails { m.putAt(0, 'zzz', 1) }
    assertLookupFails { m.rename('zzz', 'z') }
    assertLookupFails { m.apply('zzz', { it > 1 }) { it * 2 } }
    assertLookupFails { m.apply('zzz', [0]) { it * 2 } }
    assertLookupFails { m.convert('zzz', Integer) { it } }
    assertLookupFails { m.convert([new Converter('zzz', Integer, { it })] as Converter[]) }
    assertLookupFails { m.moveColumn('zzz', 0) }
    assertLookupFails { m.moveValue(0, 'zzz', 'a') }
    assertLookupFails { m.moveValue(0, 'a', 'zzz') }
    assertLookupFails { m.moveValue(0, 0, 'zzz') }
  }

  private static void assertLookupFails(Closure<?> code) {
    def ex = assertThrows(IllegalArgumentException, code)
    assertTrue(ex.message.contains('There is no column called'), "Unexpected message: ${ex.message}")
  }

  @Test
  void dropExceptIntResetsIndexWhenIndexedColumnRemoved() {
    def m = Matrix.builder().data(k: ['a', 'b', 'c'], v: [1, 2, 3]).types(String, Integer).build()
    m.createIndex('k')
    m.dropExcept(1)
    assertTrue(!m.hasIndex())
  }

  @Test
  void dropExceptIntPreservesIndexWhenIndexedColumnRetained() {
    def m = Matrix.builder().data(k: ['a', 'b'], v: [1, 2]).types(String, Integer).build()
    m.createIndex('k')
    m.dropExcept(0)
    assertTrue(m.hasIndex())
    assertIterableEquals([0], m.lookupIndices('a'))
  }

  // ── 2. Rectangular data invariants ─────────────────────────────────────────

  @Test
  void builderMapRejectsRaggedColumns() {
    def ex = assertThrows(IllegalArgumentException) {
      Matrix.builder().data(a: [1, 2, 3], b: [4]).build()
    }
    assertTrue(ex.message.contains("Column 'b' has 1 rows but previous columns have 3 rows"))
  }

  @Test
  void addColumnRejectsNonMatchingWidth() {
    def m = Matrix.builder().data(a: [1, 2, 3]).types(int).build()
    def ex = assertThrows(IllegalArgumentException) {
      m.addColumn('b', int, [1, 2])
    }
    assertTrue(ex.message.contains('does not match row count'))
  }

  @Test
  void upsertAssignmentRejectsNonMatchingWidthOnSchemaOnlyMatrix() {
    def m = Matrix.builder().columnNames(['a']).types(Integer).build()
    def ex = assertThrows(IllegalArgumentException) {
      m['a'] = [1, 2, 3]
    }
    assertTrue(ex.message.contains('does not match the number of rows (0)'))
  }

  @Test
  void typedUpsertAssignmentRejectsNonMatchingWidth() {
    def m = Matrix.builder().columnNames(['a']).types(Integer).build()
    def ex = assertThrows(IllegalArgumentException) {
      m['a', Integer] = [1, 2, 3]
    }
    assertTrue(ex.message.contains('does not match row count'))
  }

  @Test
  void andMapExpandsNullEntriesToTargetWidth() {
    def m = Matrix.builder().data(a: [1, 2, 3]).build()
    m & [b: null, c: [4, 5, 6]]
    assertEquals(3, m.columnCount())
    assertIterableEquals([null, null, null], m['b'])
    assertIterableEquals([4, 5, 6], m['c'])
  }

  @Test
  void andMapRejectsConflictingNonNullSizesBeforeMutation() {
    def m = Matrix.builder().data(a: [1, 2, 3]).build()
    def ex = assertThrows(IllegalArgumentException) {
      m & [b: [1, 2], c: [4, 5, 6]]
    }
    assertTrue(ex.message.contains('target width is 3'))
    assertEquals(1, m.columnCount())
  }

  @Test
  void firstColumnAssignmentEstablishesMatrixWidth() {
    def m = Matrix.builder().build()
    m['a'] = [1, 2, 3]
    m['b'] = [4, 5, 6]
    assertEquals(2, m.columnCount())
    assertThrows(IllegalArgumentException) { m['c'] = [1, 2] }
  }

  // ── 3. Row view safety ─────────────────────────────────────────────────────

  @Test
  void rowListIteratorSetWritesThroughToMatrix() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4]).types(int, int).build()
    def row = m.row(0)
    def it = row.listIterator()
    it.next()
    it.set(99)
    assertEquals(99, m[0, 'a'])
  }

  @Test
  void rowReplaceAllWritesThroughToMatrix() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4]).types(int, int).build()
    def row = m.row(0)
    row.replaceAll { it * 10 }
    assertEquals(10, m[0, 'a'])
    assertEquals(30, m[0, 'b'])
  }

  @Test
  void rowSortIsRejected() {
    def m = Matrix.builder().data(a: [2], b: [1]).types(int, int).build()
    def row = m.row(0)
    def ex = assertThrows(UnsupportedOperationException) { row.sort(null) }
    assertTrue(ex.message.contains('Sorting a row is not supported'))
  }

  @Test
  void rowSubListSortIsRejected() {
    def m = Matrix.builder().data(a: [3], b: [1], c: [2]).types(int, int, int).build()
    def row = m.row(0)
    assertThrows(UnsupportedOperationException) { row.subList(0, 3).sort(null) }
    assertIterableEquals([3, 1, 2], m.row(0).toList())
  }

  @Test
  void rowRemoveIfIsRejectedWithoutPartialMutation() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4]).types(int, int).build()
    def row = m.row(0)
    assertThrows(UnsupportedOperationException) { row.removeIf { it == 1 } }
    assertEquals(1, m[0, 'a'])
  }

  @Test
  void rowSubListIntIntIsLiveAndStructuralMutationRejected() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4], c: [5, 6]).types(int, int, int).build()
    def row = m.row(0)
    def sub = row.subList(0, 2)
    sub[1] = 99
    assertEquals(99, m[0, 'b'])
    assertThrows(UnsupportedOperationException) { sub.add(7) }
    assertThrows(UnsupportedOperationException) { sub.remove(0) }
  }

  @Test
  void rowSubListRangeReturnsCopy() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4], c: [5, 6]).types(int, int, int).build()
    def row = m.row(0)
    def copy = row.subList(0..1)
    copy[0] = 99
    assertEquals(1, m[0, 'a'])
  }

  @Test
  void rowMinusColumnUnknownNameRejects() {
    def m = Matrix.builder().data(a: [1]).types(int).build()
    def row = m.row(0)
    def ex = assertThrows(IllegalArgumentException) { row - 'zzz' }
    assertTrue(ex.message.contains('Failed to find a column with the name'))
  }

  // ── 4. Row and column movement validation ──────────────────────────────────

  @Test
  void moveColumnUnknownNameRejects() {
    def m = Matrix.builder().data(a: [1], b: [2]).types(int, int).build()
    def ex = assertThrows(IllegalArgumentException) { m.moveColumn('zzz', 0) }
    assertTrue(ex.message.contains('There is no column called'))
  }

  @Test
  void moveRowSameIndexIsNoOpAndBoundsAreValidated() {
    def m = Matrix.builder().data(a: [1, 2, 3]).types(int).build()
    assertEquals(m.rowList(), m.moveRow(0, 0).rowList())
    assertThrows(IndexOutOfBoundsException) { m.moveRow(-1, 0) }
    assertThrows(IndexOutOfBoundsException) { m.moveRow(0, 3) }
  }

  @Test
  void addRowsIsAtomicWhenLaterRowHasWrongWidth() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4]).types(int, int).build()
    def ex = assertThrows(IllegalArgumentException) {
      m.addRows([[5, 6], [7]])
    }
    assertTrue(ex.message.contains('does not match the number of columns'))
    assertEquals(2, m.rowCount())
  }

  @Test
  void addRowPositionOutOfBoundsRejectsBeforeMutation() {
    def m = Matrix.builder().data(a: [1, 2], b: [3, 4]).types(int, int).build()
    def ex = assertThrows(IndexOutOfBoundsException) { m.addRow(5, [5, 6]) }
    assertTrue(ex.message.contains('out of range'))
    assertEquals(2, m.rowCount())
  }

  @Test
  void removeRowsDeduplicatesAndValidatesIndexes() {
    def m = Matrix.builder().data(a: [1, 2, 3, 4], b: [5, 6, 7, 8]).types(int, int).build()
    m.removeRows([0, 2, 2, 0])
    assertEquals(2, m.rowCount())
    assertEquals(2, m[0, 'a'])
    assertEquals(4, m[1, 'a'])
  }

  // ── 5. Statistical summaries ───────────────────────────────────────────────

  @Test
  void medianAndQuartilesIgnoreNullAndNonNumericValues() {
    assertEquals(2G, Stat.median([1, null, 3]))
    assertIterableEquals([2, 3], Stat.quartiles([1, 2, 3, 4]))
    assertEquals(1, Stat.iqr([1, 2, 3, 4]))
    assertIterableEquals([2, 3], Stat.quartiles([1, null, 2, 3, 4]))
    assertEquals(1, Stat.iqr([1, null, 2, 3, 4]))
  }

  @Test
  void medianQuartilesAndIqrHandleEmptyAndAllNull() {
    assertNull(Stat.median([null, null]))
    assertIterableEquals([null, null], Stat.quartiles([null, null]))
    assertNull(Stat.iqr([null, null]))
    assertNull(Stat.median([]))
    assertIterableEquals([null, null], Stat.quartiles([]))
  }

  @Test
  void summaryHandlesAllNullNumericColumn() {
    def m = Matrix.builder().data(a: [null, null]).types(Integer).build()
    def s = Stat.summary(m)
    assertNull(s['a']['Median'])
    assertNull(s['a']['1st Q'])
  }

  // ── 6. Equality semantics ──────────────────────────────────────────────────

  @Test
  void defaultEqualsUsesExactComparison() {
    def m1 = Matrix.builder().data(a: [1.0G]).build()
    def m2 = Matrix.builder().data(a: [1.0001G]).build()
    assertNotEquals(m1, m2)
    assertTrue(m1.equals(m2, true, true, false, 0.001G, false))
  }

  @Test
  void equalNumericMatricesWithDifferentScaleOrBoxingMayHaveDifferentHashCodes() {
    def m1 = Matrix.builder().data(a: [1.0G]).build()
    def m2 = Matrix.builder().data(a: [1.00G]).build()
    assertEquals(m1, m2)
    // Documented limitation: hash codes may differ for scale/boxing variants.
    // The assertion below records the current behavior rather than a contract.
    assertNotEquals(m1.hashCode(), m2.hashCode())
  }

  // ── 7. Precision and input adapters ────────────────────────────────────────

  @Test
  void rollingMeanPreservesSmallValuesFromRawSum() {
    def m = Matrix.builder().data(x: [5E-17G, 5E-17G, 5E-17G, 5E-17G]).types(BigDecimal).build()
    def rm = m.rolling(4).mean()
    assertTrue(rm['x'][3] == 5E-17G)
  }

  @Test
  void rollingMeanAllZerosYieldsNumericZero() {
    def m = Matrix.builder().data(x: [0G, 0G, 0G, 0G]).types(BigDecimal).build()
    def rm = m.rolling(4).mean()
    assertEquals(0G, rm['x'][3])
  }

  @Test
  void rollingMeanDistinguishesRawAccumulationFromPerAddendRounding() {
    def m = Matrix.builder().data(
        x: [0.12345678901234564G, 0.12345678901234565G]
    ).types(BigDecimal).build()
    def rm = m.rolling(2).mean()
    BigDecimal expected = 0.1234567890123456G
    assertTrue(rm['x'][1] == expected)
  }

  @Test
  void csvStringCommentOnlyHeaderlessTypesBuildsEmptyTypedMatrix() {
    def m = Matrix.builder()
        .csvString('#name: demo\n#types: Integer, String\n', [firstRowAsHeader: false])
        .build()
    assertEquals('demo', m.matrixName)
    assertIterableEquals([Integer, String], m.types())
    assertIterableEquals(['c1', 'c2'], m.columnNames())
    assertEquals(0, m.rowCount())
  }

  @Test
  void csvStringHeaderlessMissingIndexColumnRejects() {
    def ex = assertThrows(IllegalArgumentException) {
      Matrix.builder().csvString('#index: id\n', [firstRowAsHeader: false]).build()
    }
    assertTrue(ex.message.contains('Index column(s) do not exist'))
  }

  @Test
  void csvStringHeaderedMissingIndexColumnUsesNeutralMessage() {
    def ex = assertThrows(IllegalArgumentException) {
      Matrix.builder().csvString('#index: zzz\nid,v\n1,2\n', [firstRowAsHeader: true]).build()
    }
    assertTrue(ex.message.contains('resolved column names'))
    assertTrue(!ex.message.contains('Headerless'))
  }

  @Test
  void csvStringCommentOnlyWithHeaderMissingRejects() {
    def ex = assertThrows(IllegalArgumentException) {
      Matrix.builder().csvString('#types: Integer\n', [firstRowAsHeader: true]).build()
    }
    assertTrue(ex.message.contains('No data row available to use as header'))
  }

  @Test
  void csvStringEmptyRejectsWithDirectMessage() {
    def ex = assertThrows(IllegalArgumentException) {
      Matrix.builder().csvString('')
    }
    assertEquals('Empty CSV content', ex.message)
  }

  @Test
  void resultSetUsesColumnLabelForMatrixColumnNames() {
    def url = 'jdbc:h2:mem:polish390'
    def conn = java.sql.DriverManager.getConnection(url, 'sa', '')
    def sql = new Sql(conn)
    try {
      sql.execute('CREATE TABLE t (id INT, name VARCHAR(20))')
      sql.execute("INSERT INTO t VALUES (1, 'Alice')")
      def stmt = conn.prepareStatement('SELECT id AS alias_id, name AS alias_name FROM t')
      def rs = stmt.executeQuery()
      try {
        def m = Matrix.builder().data(rs).build()
        assertIterableEquals(['ALIAS_ID', 'ALIAS_NAME'], m.columnNames())
        assertEquals(1, m[0, 'ALIAS_ID'])
        assertEquals('Alice', m[0, 'ALIAS_NAME'])
      } finally {
        rs.close()
        stmt.close()
      }
    } finally {
      sql.close()
    }
  }

  // ── Grid width enforcement and snapshotting ────────────────────────────────

  @Test
  void gridConstructorCopiesSourceLists() {
    def outer = [[1, 2], [3, 4]]
    def g = new Grid<Integer>(outer, Integer)
    outer[0][0] = 99
    outer << [5, 6]
    assertEquals(1, g[0, 0])
    assertEquals(2, g.dimensions().observations)
  }

  @Test
  void gridOneArgumentConstructorCopiesSourceLists() {
    def outer = [[1, 2], [3, 4]]
    def g = new Grid<Integer>(outer)
    outer[0][0] = 99
    outer << [5, 6]
    assertEquals(1, g[0, 0])
    assertEquals(2, g.dimensions().observations)
  }

  @Test
  void gridReplaceRowCopiesAliasedCheckedViewBeforeClearing() {
    def g = new Grid<Integer>([[1, 2], [3, 4]], Integer)
    g.replaceRow(0, g[0])
    assertEquals([[1, 2], [3, 4]], g.getRowList())
  }

  @Test
  void gridWidthIsEnforcedForRowMutators() {
    def g = new Grid<Integer>([[1, 2], [3, 4]], Integer)
    assertThrows(IllegalArgumentException) { g << [1, 2, 3] }
    assertThrows(IllegalArgumentException) { g.add(0, [1]) }
    assertThrows(IllegalArgumentException) { g.addAll([[1, 2], [3]]) }
    assertThrows(IllegalArgumentException) { g[0] = [1] }
  }

  @Test
  void gridLeftShiftReturnsGridForChaining() {
    def g = new Grid<Integer>()
    g << [1, 2] << [3, 4]
    assertEquals([[1, 2], [3, 4]], g.getRowList())
  }

  @Test
  void gridAddAllRejectsNullRowsAndNullInput() {
    def g = new Grid<Integer>()
    assertThrows(IllegalArgumentException) { g.addAll([null]) }
    assertThrows(IllegalArgumentException) { g.addAll(null) }
  }

  @Test
  void gridIteratorRemovalIsRejected() {
    def g = new Grid<Integer>([[1, 2]], Integer)
    def iterator = g.iterator()
    iterator.next()
    assertThrows(UnsupportedOperationException) { iterator.remove() }
  }

  @Test
  void gridGetAtAndIteratorRowsAllowWriteThroughValueAssignment() {
    def g = new Grid<Integer>([[1, 2], [3, 4]], Integer)
    g[0][1] = 99
    assertEquals(99, g[0, 1])
    g.iterator().next()[0] = 77
    assertEquals(77, g[0, 0])
  }

  @Test
  void gridDataViewRejectsStructuralMutationButAllowsValueWrites() {
    def g = new Grid<Integer>([[1, 2], [3, 4]], Integer)
    assertThrows(UnsupportedOperationException) { g.data << [5, 6] }
    assertThrows(UnsupportedOperationException) { g.data[0] = [9, 9] }
    assertThrows(UnsupportedOperationException) { g.data[0].add(9) }
    g.data[0][1] = 55
    assertEquals(55, g[0, 1])
  }

  @Test
  void gridDataReassignmentIsRejected() {
    def g = new Grid<Integer>([[1, 2]], Integer)
    assertThrows(UnsupportedOperationException) { g.data = [[3, 4]] }
  }

}
