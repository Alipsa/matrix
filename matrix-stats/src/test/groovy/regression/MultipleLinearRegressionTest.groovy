package regression

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import groovy.transform.CompileStatic

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.regression.MultipleLinearRegression
import se.alipsa.matrix.stats.util.LeastSquaresKernel

/**
 * Tests for native ordinary least squares multiple linear regression.
 */
@CompileStatic
class MultipleLinearRegressionTest {

  @Test
  void testMatchesApacheOlsCoefficientsAndStandardErrors() {
    double[] response = [2.1d, 3.9d, 5.8d, 8.2d, 9.7d, 11.9d] as double[]
    double[][] predictors = [
      [1.0d, 0.5d, 1.2d],
      [1.0d, 1.0d, 1.8d],
      [1.0d, 1.5d, 2.1d],
      [1.0d, 2.0d, 2.9d],
      [1.0d, 2.5d, 3.2d],
      [1.0d, 3.0d, 3.8d]
    ] as double[][]

    MultipleLinearRegression nativeModel = new MultipleLinearRegression(response, predictors)
    OLSMultipleLinearRegression apacheModel = new OLSMultipleLinearRegression()
    apacheModel.noIntercept = true
    apacheModel.newSampleData(response, predictors)

    double[] expectedCoefficients = apacheModel.estimateRegressionParameters()
    double[] expectedStandardErrors = apacheModel.estimateRegressionParametersStandardErrors()

    for (int i = 0; i < expectedCoefficients.length; i++) {
      assertEquals(expectedCoefficients[i], nativeModel.coefficients[i], 1e-12d, "Coefficient mismatch at ${i}")
      assertEquals(expectedStandardErrors[i], nativeModel.standardErrors[i], 1e-12d, "SE mismatch at ${i}")
    }
  }

  @Test
  void testRejectsUnderdeterminedModel() {
    double[] response = [1.0d, 2.0d] as double[]
    double[][] predictors = [
      [1.0d, 0.0d],
      [1.0d, 1.0d]
    ] as double[][]

    assertThrows(IllegalArgumentException) {
      new MultipleLinearRegression(response, predictors)
    }
  }

  @Test
  void testGroovyFacingConstructorAndValueAccessors() {
    List<BigDecimal> response = [2.1, 3.9, 5.8, 8.2, 9.7, 11.9]
    List<List<BigDecimal>> predictors = [
      [1.0, 0.5, 1.2],
      [1.0, 1.0, 1.8],
      [1.0, 1.5, 2.1],
      [1.0, 2.0, 2.9],
      [1.0, 2.5, 3.2],
      [1.0, 3.0, 3.8]
    ]

    MultipleLinearRegression model = new MultipleLinearRegression(response, predictors)

    assertEquals(model.coefficients.length, model.coefficientValues.size())
    assertEquals(model.standardErrors.length, model.standardErrorValues.size())
    assertEquals(model.coefficients[0], model.coefficientValues[0] as double, 1e-12d)
    assertEquals(model.standardErrors[0], model.standardErrorValues[0] as double, 1e-12d)
    assertEquals(model.residualSumOfSquares, model.residualSumOfSquaresValue as double, 1e-12d)
    assertEquals(model.errorVariance, model.errorVarianceValue as double, 1e-12d)
  }

  @Test
  void testGroovyFacingConstructorMatchesLeastSquaresKernel() {
    List<BigDecimal> response = [2.1, 3.9, 5.8, 8.2, 9.7, 11.9]
    List<List<BigDecimal>> predictors = [
      [1.0, 0.5, 1.2],
      [1.0, 1.0, 1.8],
      [1.0, 1.5, 2.1],
      [1.0, 2.0, 2.9],
      [1.0, 2.5, 3.2],
      [1.0, 3.0, 3.8]
    ]

    MultipleLinearRegression model = new MultipleLinearRegression(response, predictors)
    LeastSquaresKernel.OlsComputation kernel = LeastSquaresKernel.fitMultipleLinearRegression(
      response.collect { BigDecimal value -> value as double } as double[],
      predictors.collect { List<BigDecimal> row -> row.collect { BigDecimal value -> value as double } as double[] } as double[][]
    )

    assertEquals(kernel.coefficients().toList(), model.coefficients.toList())
    assertEquals(kernel.standardErrors().toList(), model.standardErrors.toList())
    assertEquals(kernel.residualSumOfSquares(), model.residualSumOfSquares, 1e-12d)
    assertEquals(kernel.errorVariance(), model.errorVariance, 1e-12d)
  }
}
