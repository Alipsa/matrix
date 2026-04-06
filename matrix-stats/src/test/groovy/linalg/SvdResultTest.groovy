package linalg

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.linalg.Linalg
import se.alipsa.matrix.stats.linalg.SvdResult

class SvdResultTest {

  @Test
  void testSigmaMatrixShapeAndDiagonal() {
    SvdResult result = new SvdResult(
      matrix([
        [1.0d, 0.0d],
        [0.0d, 1.0d],
        [0.0d, 0.0d],
      ]),
      matrix([
        [1.0d, 0.0d],
        [0.0d, 1.0d],
      ]),
      [5.0d, 2.0d]
    )

    Matrix sigma = result.sigma()

    assertEquals(3, sigma.rowCount())
    assertEquals(2, sigma.columnCount())
    assertEquals(5.0d, sigma[0, 0] as double, 1e-12)
    assertEquals(2.0d, sigma[1, 1] as double, 1e-12)
    assertEquals(0.0d, sigma[2, 0] as double, 1e-12)
  }

  @Test
  void testRejectsEmptySingularValueVector() {
    assertThrows(IllegalArgumentException) {
      new SvdResult(
        matrix([[1.0d]]),
        matrix([[1.0d]]),
        []
      )
    }
  }

  @Test
  void testReconstructReturnsOriginalMatrix() {
    Matrix original = Matrix.builder()
      .columnNames(['c0', 'c1'])
      .rows([
        [3.0d, 1.0d],
        [1.0d, 3.0d],
      ])
      .types([Double, Double])
      .build()

    def svd = Linalg.svd(original)
    Matrix reconstructed = svd.reconstruct()

    assertEquals(2, reconstructed.rowCount())
    assertEquals(2, reconstructed.columnCount())
    assertEquals(3.0d, reconstructed[0, 0] as double, 1e-10)
    assertEquals(1.0d, reconstructed[0, 1] as double, 1e-10)
    assertEquals(1.0d, reconstructed[1, 0] as double, 1e-10)
    assertEquals(3.0d, reconstructed[1, 1] as double, 1e-10)
  }

  private static Matrix matrix(List<List<Double>> rows) {
    Matrix.builder()
      .columnNames((0..<rows[0].size()).collect { "x${it}" })
      .rows(rows)
      .types(([Double] * rows[0].size()) as List<Class>)
      .build()
  }
}
