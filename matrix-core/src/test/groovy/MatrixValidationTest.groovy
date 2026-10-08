import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixValidationCode

class MatrixValidationTest {

  @Test
  void testValidationAndRawTypes() {
    def matrix = Matrix.builder('diagnostics').data(a: [1, null], b: [2, 3]).types(int, Integer).build()
    matrix.createIndex('a')
    def kept = matrix.row(0)
    assertEquals([], matrix.validate(true))
    matrix.column('b').remove(1)
    matrix.column('b').set(0, '5')
    matrix.column('b').name = 'a'
    def before = matrix.columns().collect { new ArrayList(it) }
    def issues = matrix.validate(true)
    assertIterableEquals([MatrixValidationCode.UNEQUAL_COLUMN_LENGTH,
                          MatrixValidationCode.DUPLICATE_COLUMN_NAME,
                          MatrixValidationCode.INCOMPATIBLE_CELL_TYPE], issues*.code)
    assertEquals(2, issues[0].expectedLength)
    assertEquals(1, issues[0].actualLength)
    assertEquals(0, issues[2].rowIndex)
    assertEquals(Integer, issues[2].expectedType)
    assertEquals(String, issues[2].actualType)
    assertEquals(before, matrix.columns())
    assertIterableEquals(['a'], matrix.indexedColumns())
    kept.set(0, 7) // Validation does not invalidate attached writes.
    assertEquals(7, matrix.column(0).get(0))
    assertThrows(ReadOnlyPropertyException) { issues[0].columnIndex = 5 }
  }

  @Test
  void testNamesTypesAndOrdering() {
    def matrix = Matrix.builder().data(a: [1, null], b: [2L, 3L], c: ['5', '6']).types(Number, Integer, Integer).build()
    matrix.column(0).name = null
    matrix.column(1).name = ' '
    def issues = matrix.validate(true)
    assertIterableEquals([0, 1, 1, 1, 2, 2], issues*.columnIndex)
    assertIterableEquals([null, null, 0, 1, 0, 1], issues*.rowIndex)
    assertEquals(2, matrix.validate().size())
    matrix.column(1).type = Object
    matrix.column(2).type = null
    assertEquals(2, matrix.validate(true).size())
    assertEquals([], Matrix.builder().build().validate(true))
    assertEquals([], Matrix.builder().columnNames(['a']).types(Integer).build().validate(true))
  }
}
