package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Factor

import static org.junit.jupiter.api.Assertions.*

class FactorTest {

  @Test
  void testDefaultNames() {
    assertEquals('factor_class', new Factor('class').name)
    assertEquals('factor_blank', new Factor('   ').name)
    assertEquals('factor_const_1', new Factor(1).name)
    assertEquals('factor_foo_bar', new Factor('foo bar').name)
  }

  @Test
  void testAddToMatrixConstant() {
    def data = Matrix.builder()
        .columnNames(['a'])
        .rows([[1], [2], [3]])
        .build()
    def factor = new Factor(1)
    String colName = factor.addToMatrix(data)

    assertTrue(data.columnNames().contains(colName))
    assertEquals(['1', '1', '1'], data[colName])
  }

  @Test
  void testAddToMatrixList() {
    def data = Matrix.builder()
        .columnNames(['a'])
        .rows([[1], [2], [3]])
        .build()
    def factor = new Factor(['x', 'y', 'z'])
    String colName = factor.addToMatrix(data)

    assertEquals(['x', 'y', 'z'], data[colName])
  }

  @Test
  void testAddToMatrixColumnReference() {
    def data = Matrix.builder()
        .columnNames(['col'])
        .rows([['a'], ['b']])
        .build()
    def factor = new Factor('col')
    String colName = factor.addToMatrix(data)

    assertEquals(['a', 'b'], data[colName])
  }

  @Test
  void testNameCollisionAddsSuffix() {
    def data = Matrix.builder()
        .columnNames(['factor_blank'])
        .rows([[1], [2]])
        .build()
    def factor = new Factor(' ')
    String colName = factor.addToMatrix(data)

    assertEquals('factor_blank_1', colName)
  }

  @Test
  void testListSizeMismatchThrows() {
    def data = Matrix.builder()
        .columnNames(['a'])
        .rows([[1], [2]])
        .build()
    def factor = new Factor(['x'])

    def ex = assertThrows(IllegalArgumentException) {
      factor.addToMatrix(data)
    }
    assertTrue(ex.message.contains('Factor list size'))
  }

  @Test
  void testToStringEscapes() {
    def factor = new Factor("my'value")
    assertEquals("factor('my\\'value')", factor.toString())
  }
}
