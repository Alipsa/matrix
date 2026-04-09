package util

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.util.LeastSquaresKernel

class LeastSquaresKernelTest {

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

    assertEquals("Design matrix rows must all have the same length", exception.message)
  }
}
