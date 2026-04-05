package util

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.util.NumericConversion

class NumericConversionTest {

  @Test
  void testRejectsNonFiniteBigDecimalInput() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      NumericConversion.toBigDecimal(Double.NaN, 'value')
    }

    assertTrue(exception.message.contains('finite'))
  }

  @Test
  void testRejectsNonNumericBigDecimalInput() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      NumericConversion.toBigDecimal('not a number', 'value')
    }

    assertTrue(exception.message.contains('numeric'))
  }

  @Test
  void testToAlphaValidation() {
    assertEquals(0.05, NumericConversion.toAlpha(0.05))

    IllegalArgumentException low = assertThrows(IllegalArgumentException) {
      NumericConversion.toAlpha(0.0)
    }
    IllegalArgumentException high = assertThrows(IllegalArgumentException) {
      NumericConversion.toAlpha(1.0)
    }

    assertTrue(low.message.contains('between 0 and 1'))
    assertTrue(high.message.contains('between 0 and 1'))
  }

  @Test
  void testToDoubleArrayForMatrixAndGridColumns() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [1.0, 2.0],
        [3.0, 4.0],
      ])
      .types([Double, Double])
      .build()
    Grid<Number> grid = new Grid<Number>([
      [1.0, 2.0],
      [3.0, 4.0],
    ])

    assertEquals([1.0d, 3.0d], NumericConversion.toDoubleArray(matrix, 'x').toList())
    assertEquals([2.0d, 4.0d], NumericConversion.toDoubleArray(grid, 1).toList())
  }

  @Test
  void testToDoubleArrayForMatrixAndGrid() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [1.0, 2.0],
        [3.0, 4.0],
      ])
      .types([Double, Double])
      .build()
    Grid<Number> grid = new Grid<Number>([
      [1.0, 2.0],
      [3.0, 4.0],
    ])

    assertEquals([[1.0d, 2.0d], [3.0d, 4.0d]], NumericConversion.toDoubleArray(matrix).collect { it.toList() })
    assertEquals([[1.0d, 2.0d], [3.0d, 4.0d]], NumericConversion.toDoubleArray(grid).collect { it.toList() })
  }

  @Test
  void testToBigDecimalGridForMatrixAndGrid() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [1.0, 2.0],
        [3.0, 4.0],
      ])
      .types([Double, Double])
      .build()
    Grid<Number> grid = new Grid<Number>([
      [1.0, 2.0],
      [3.0, 4.0],
    ])

    Grid<BigDecimal> matrixGrid = NumericConversion.toBigDecimalGrid(matrix)
    Grid<BigDecimal> numericGrid = NumericConversion.toBigDecimalGrid(grid)

    assertEquals(1.0, matrixGrid[0, 0])
    assertEquals(4.0, matrixGrid[1, 1])
    assertEquals(1.0, numericGrid[0, 0])
    assertEquals(4.0, numericGrid[1, 1])
  }

  @Test
  void testValidateRectangularDenseArray() {
    int[] shape = NumericConversion.validateRectangular([
      [1.0d, 2.0d] as double[],
      [3.0d, 4.0d] as double[],
    ] as double[][], 'values')

    assertEquals([2, 2], shape.toList())
  }

  @Test
  void testRejectsRaggedGrid() {
    Grid<Number> grid = new Grid<Number>()
    grid << [1.0, 2.0]
    grid << [3.0]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      NumericConversion.validateRectangular(grid, 'grid')
    }

    assertTrue(exception.message.contains('same length'))
  }
}
