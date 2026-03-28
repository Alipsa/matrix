package se.alipsa.matrix.stats.timeseries

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

/**
 * Regression tests for shared linear algebra helpers used by the time-series package.
 */
@CompileStatic
class TimeSeriesUtilsTest {

  @Test
  void solveLinearSystemThrowsOnSingularSquareMatrix() {
    double[][] A = [
      [1.0, 2.0] as double[],
      [2.0, 4.0] as double[]
    ] as double[][]
    double[] b = [3.0, 6.0] as double[]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      TimeSeriesUtils.solveLinearSystem(A, b)
    }

    assertEquals("Singular matrix at column 1 - cannot solve linear system", exception.message)
  }

  @Test
  void invertMatrixUsesPartialPivoting() {
    double[][] A = [
      [0.0, 1.0] as double[],
      [1.0, 0.0] as double[]
    ] as double[][]

    double[][] inverse = TimeSeriesUtils.invertMatrix(A)

    assertEquals(0.0, inverse[0][0], 1e-12)
    assertEquals(1.0, inverse[0][1], 1e-12)
    assertEquals(1.0, inverse[1][0], 1e-12)
    assertEquals(0.0, inverse[1][1], 1e-12)
  }

  @Test
  void invertMatrixThrowsOnSingularMatrix() {
    double[][] A = [
      [1.0, 2.0] as double[],
      [2.0, 4.0] as double[]
    ] as double[][]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      TimeSeriesUtils.invertMatrix(A)
    }

    assertEquals("Singular matrix at column 1 - cannot invert matrix", exception.message)
  }
}
