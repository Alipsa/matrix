package linalg

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.linalg.Linalg

class LinalgAdapterTest {

  @Test
  void testRejectsNonNumericMatrixColumns() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'label'])
      .rows([
        [1.0, 'A'],
        [2.0, 'B'],
      ])
      .types([Double, String])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Linalg.inverse(matrix)
    }

    assertTrue(exception.message.contains('must be numeric'))
  }

  @Test
  void testRejectsRaggedGrid() {
    Grid<Number> grid = new Grid<Number>([
      [1.0, 2.0],
      [3.0],
    ])

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Linalg.svd(grid)
    }

    assertNotNull(exception.message)
  }

  @Test
  void testMatrixOutputsUseSyntheticNames() {
    Matrix matrix = Matrix.builder()
      .columnNames(['alpha', 'beta'])
      .rows([
        [4.0, 7.0],
        [2.0, 6.0],
      ])
      .types([Double, Double])
      .build()

    Matrix inverse = Linalg.inverse(matrix)
    def svd = Linalg.svd(matrix)

    assertEquals(['c0', 'c1'], inverse.columnNames())
    assertEquals(['c0', 'c1'], svd.uMatrix().columnNames())
    assertEquals(['c0', 'c1'], svd.vtMatrix().columnNames())
  }
}
