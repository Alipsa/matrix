package regression

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.regression.MultipleLinearRegression

/**
 * Tests for native ordinary least squares multiple linear regression.
 */
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
}
