package util

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.util.LeastSquaresKernel

class LeastSquaresKernelTest {

  @Test
  void rankIsRelativeToEachColumnAndAliasesPreserveOrder() {
    double[] y = [3.1d, 4.9d, 7.2d, 8.8d, 11.1d, 13.0d] as double[]
    double[] z = [1, 0, 1, 0, 1, 1] as double[]
    [1e-8d, 1.0d, 1e8d].each { double scale ->
      double[][] x = (0..<6).collect { int j -> [1.0d, (j + 1) * scale, 2 * (j + 1) * scale, z[j]] as double[] } as double[][]
      def result = LeastSquaresKernel.fitRankRevealing(y, x)
      // R 4.6.1: lm(y ~ x + I(2*x) + z); x=1:6, z=c(1,0,1,0,1,1).
      assertEquals(3, result.rank())
      assertArrayEquals([0.921641791d, 1.976119403d, 0.267910448d] as double[],
        [result.coefficients()[0], result.coefficients()[1] * scale, result.coefficients()[3]] as double[], 1e-8d)
      assertEquals(true, result.aliased()[2])
      assertArrayEquals([0.073084814d, 0.017533343d, 0.063520571d] as double[],
        [result.standardErrors()[0], result.standardErrors()[1] * scale, result.standardErrors()[3]] as double[], 1e-8d)
    }
  }

  @Test
  void inconsistentRectangularSystemUsesAllRows() {
    // R 4.6.1: coef(lm(c(1,3,5,8) ~ c(0,1,2,3)))
    double[][] x = [[1, 0], [1, 1], [1, 2], [1, 3]] as double[][]
    assertArrayEquals([0.8d, 2.3d] as double[], LeastSquaresKernel.fitOls([1, 3, 5, 8] as double[], x), 1e-12d)
  }

  @Test
  void testFitMultipleLinearRegressionMatchesExpectedCoefficientsAndStandardErrors() {
    double[] response = [2.1d, 3.9d, 5.8d, 8.2d, 9.7d, 11.9d] as double[]
    double[][] predictors = [
      [1.0d, 0.5d, 1.2d],
      [1.0d, 1.0d, 1.8d],
      [1.0d, 1.5d, 2.1d],
      [1.0d, 2.0d, 2.9d],
      [1.0d, 2.5d, 3.2d],
      [1.0d, 3.0d, 3.8d]
    ] as double[][]

    LeastSquaresKernel.OlsComputation result = LeastSquaresKernel.fitMultipleLinearRegression(response, predictors)
    OLSMultipleLinearRegression apacheModel = new OLSMultipleLinearRegression()
    apacheModel.noIntercept = true
    apacheModel.newSampleData(response, predictors)
    double[] apacheCoefficients = apacheModel.estimateRegressionParameters()
    double[] apacheResiduals = new double[response.length]
    for (int row = 0; row < response.length; row++) {
      double fitted = 0.0d
      for (int column = 0; column < apacheCoefficients.length; column++) {
        fitted += predictors[row][column] * apacheCoefficients[column]
      }
      apacheResiduals[row] = response[row] - fitted
    }
    double apacheResidualSumOfSquares = apacheResiduals.collect { double residual -> residual * residual }.sum() as double
    double apacheErrorVariance = apacheResidualSumOfSquares / (response.length - predictors[0].length)

    assertArrayEquals(apacheCoefficients, result.coefficients(), 1e-12d)
    assertArrayEquals(apacheModel.estimateRegressionParametersStandardErrors(), result.standardErrors(), 1e-12d)
    assertEquals(apacheResidualSumOfSquares, result.residualSumOfSquares(), 1e-12d)
    assertEquals(apacheErrorVariance, result.errorVariance(), 1e-12d)
  }

  @Test
  void testFitOlsMatchesExpectedCoefficients() {
    double[][] predictors = [
      [1.0d, 0.0d] as double[],
      [1.0d, 1.0d] as double[],
      [1.0d, 2.0d] as double[],
      [1.0d, 3.0d] as double[]
    ] as double[][]
    double[] response = [1.0d, 3.0d, 5.0d, 7.0d] as double[]

    double[] coefficients = LeastSquaresKernel.fitOls(response, predictors)

    assertArrayEquals([1.0d, 2.0d] as double[], coefficients, 1e-12d)
  }

  @Test
  void testCalculateResidualSumOfSquaresMatchesExpectedValue() {
    double[][] predictors = [
      [1.0d, 0.0d] as double[],
      [1.0d, 1.0d] as double[],
      [1.0d, 2.0d] as double[],
      [1.0d, 3.0d] as double[]
    ] as double[][]
    double[] response = [1.0d, 3.0d, 5.0d, 7.0d] as double[]
    double[] coefficients = [1.0d, 2.0d] as double[]

    assertEquals(0.0d, LeastSquaresKernel.calculateResidualSumOfSquares(response, predictors, coefficients), 1e-12d)
  }

  @Test
  void testCalculateResidualSumOfSquaresRejectsRaggedMatrix() {
    double[][] predictors = [
      [1.0d, 0.0d] as double[],
      [1.0d] as double[]
    ] as double[][]
    double[] response = [1.0d, 3.0d] as double[]
    double[] coefficients = [1.0d, 2.0d] as double[]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      LeastSquaresKernel.calculateResidualSumOfSquares(response, predictors, coefficients)
    }

    assertEquals('Design matrix rows must all have the same length', exception.message)
  }
}
