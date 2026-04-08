package se.alipsa.matrix.stats.timeseries

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.util.LeastSquaresKernel

/**
 * Regression tests for shared linear algebra helpers used by the time-series package.
 */
@CompileStatic
class TimeSeriesUtilsTest {

  @Test
  void solveLinearSystemSolvesWellConditionedSquareSystem() {
    double[][] a = [
      [2.0, 1.0, -1.0] as double[],
      [-3.0, -1.0, 2.0] as double[],
      [-2.0, 1.0, 2.0] as double[]
    ] as double[][]
    double[] b = [8.0, -11.0, -3.0] as double[]

    double[] solution = TimeSeriesUtils.solveLinearSystem(a, b)

    assertArrayEquals([2.0, 3.0, -1.0] as double[], solution, 1e-12)
  }

  @Test
  void solveLinearSystemSolvesWellConditionedOverdeterminedSystem() {
    double[][] a = [
      [1.0, 0.0] as double[],
      [1.0, 1.0] as double[],
      [1.0, 2.0] as double[],
      [1.0, 3.0] as double[]
    ] as double[][]
    double[] b = [1.0, 3.0, 5.0, 7.0] as double[]

    double[] solution = TimeSeriesUtils.solveLinearSystem(a, b)

    assertArrayEquals([1.0, 2.0] as double[], solution, 1e-12)
  }

  @Test
  void solveLinearSystemThrowsOnSingularSquareMatrix() {
    double[][] a = [
      [1.0, 2.0] as double[],
      [2.0, 4.0] as double[]
    ] as double[][]
    double[] b = [3.0, 6.0] as double[]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      TimeSeriesUtils.solveLinearSystem(a, b)
    }

    assertEquals("Singular matrix at column 1 - cannot solve linear system", exception.message)
  }

  @Test
  void solveLinearSystemThrowsOnSingularOverdeterminedSystem() {
    double[][] a = [
      [1.0, 1.0] as double[],
      [1.0, 1.0] as double[],
      [1.0, 1.0] as double[],
      [1.0, 1.0] as double[]
    ] as double[][]
    double[] b = [2.0, 2.0, 2.0, 2.0] as double[]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      TimeSeriesUtils.solveLinearSystem(a, b)
    }

    assertEquals("Singular matrix at column 1 - cannot solve linear system", exception.message)
  }

  @Test
  void invertMatrixUsesPartialPivoting() {
    double[][] a = [
      [0.0, 1.0] as double[],
      [1.0, 0.0] as double[]
    ] as double[][]

    double[][] inverse = TimeSeriesUtils.invertMatrix(a)

    assertEquals(0.0, inverse[0][0], 1e-12)
    assertEquals(1.0, inverse[0][1], 1e-12)
    assertEquals(1.0, inverse[1][0], 1e-12)
    assertEquals(0.0, inverse[1][1], 1e-12)
  }

  @Test
  void invertMatrixThrowsOnSingularMatrix() {
    double[][] a = [
      [1.0, 2.0] as double[],
      [2.0, 4.0] as double[]
    ] as double[][]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      TimeSeriesUtils.invertMatrix(a)
    }

    assertEquals("Singular matrix at column 1 - cannot invert matrix", exception.message)
  }

  @Test
  void fitOLSReturnsExpectedCoefficients() {
    double[][] x = [
      [1.0, 0.0] as double[],
      [1.0, 1.0] as double[],
      [1.0, 2.0] as double[],
      [1.0, 3.0] as double[]
    ] as double[][]
    double[] y = [1.0, 3.0, 5.0, 7.0] as double[]

    double[] beta = TimeSeriesUtils.fitOLS(y, x)

    assertArrayEquals([1.0, 2.0] as double[], beta, 1e-12)
  }

  @Test
  void fitOLSMatchesLeastSquaresKernel() {
    double[][] x = [
      [1.0, 0.0] as double[],
      [1.0, 1.0] as double[],
      [1.0, 2.0] as double[],
      [1.0, 3.0] as double[],
      [1.0, 4.0] as double[]
    ] as double[][]
    double[] y = [1.2, 2.9, 5.1, 7.05, 9.2] as double[]

    assertArrayEquals(LeastSquaresKernel.fitOls(y, x), TimeSeriesUtils.fitOLS(y, x), 1e-12d)
  }

  @Test
  void calculateRSSReturnsExpectedValue() {
    double[][] x = [
      [1.0, 0.0] as double[],
      [1.0, 1.0] as double[],
      [1.0, 2.0] as double[],
      [1.0, 3.0] as double[]
    ] as double[][]
    double[] y = [1.0, 3.0, 5.0, 7.0] as double[]
    double[] beta = [1.0, 2.0] as double[]

    double rss = TimeSeriesUtils.calculateRSS(y, x, beta)

    assertEquals(0.0, rss, 1e-12)
  }

  @Test
  void calculateRSSMatchesLeastSquaresKernel() {
    double[][] x = [
      [1.0, 0.0] as double[],
      [1.0, 1.0] as double[],
      [1.0, 2.0] as double[],
      [1.0, 3.0] as double[],
      [1.0, 4.0] as double[]
    ] as double[][]
    double[] y = [1.2, 2.9, 5.1, 7.05, 9.2] as double[]
    double[] beta = TimeSeriesUtils.fitOLS(y, x)

    assertEquals(LeastSquaresKernel.calculateResidualSumOfSquares(y, x, beta), TimeSeriesUtils.calculateRSS(y, x, beta), 1e-12d)
  }

  @Test
  void calculateRSSThrowsOnRaggedMatrix() {
    double[][] x = [
      [1.0, 0.0] as double[],
      [1.0] as double[]
    ] as double[][]
    double[] y = [1.0, 3.0] as double[]
    double[] beta = [1.0, 2.0] as double[]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      TimeSeriesUtils.calculateRSS(y, x, beta)
    }

    assertEquals("Design matrix rows must all have the same length", exception.message)
  }
}
