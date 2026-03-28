package regression

import static org.junit.jupiter.api.Assertions.*

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.LUDecomposition
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.regression.RegressionUtils

class RegressionUtilsTest {

  @Test
  void testPolynomialXtxInverseReturnsBigDecimalMatrix() {
    List<Number> xValues = [1, 2, 3, 4, 5]

    BigDecimal[][] inverse = RegressionUtils.polynomialXtxInverse(xValues, 2)

    assertNotNull(inverse)
    assertTrue(inverse.length > 0)
    assertTrue(inverse[0].length > 0)
    assertTrue(inverse[0][0] instanceof BigDecimal)
  }

  @Test
  void testPolynomialXtxInverseReturnsEmptyArrayForSingularMatrix() {
    List<Number> xValues = [1, 1, 1, 1]

    BigDecimal[][] inverse = RegressionUtils.polynomialXtxInverse(xValues, 1)

    assertNotNull(inverse)
    assertEquals(0, inverse.length)
  }

  @Test
  void testPolynomialXtxInverseReturnsEmptyArrayForInvalidInput() {
    BigDecimal[][] inverse = RegressionUtils.polynomialXtxInverse([], 0)

    assertNotNull(inverse)
    assertEquals(0, inverse.length)
  }

  @Test
  void testPolynomialLeverageUsesBigDecimalInverse() {
    List<Number> xValues = [1, 2, 3, 4, 5]
    BigDecimal[][] inverse = RegressionUtils.polynomialXtxInverse(xValues, 2)

    BigDecimal leverage = RegressionUtils.polynomialLeverage(inverse, 3.0, 2)

    assertNotNull(leverage)
    assertTrue(leverage > 0.0)
  }

  @Test
  void testPolynomialXtxInverseMatchesApacheReference() {
    List<Number> xValues = [1, 2, 3, 4, 5]

    BigDecimal[][] actual = RegressionUtils.polynomialXtxInverse(xValues, 2)
    double[][] expected = apachePolynomialXtxInverse(xValues, 2)

    assertEquals(expected.length, actual.length)
    for (int i = 0; i < expected.length; i++) {
      for (int j = 0; j < expected[i].length; j++) {
        assertEquals(expected[i][j], actual[i][j].doubleValue(), 1e-12, "Mismatch at [$i,$j]")
      }
    }
  }

  private static double[][] apachePolynomialXtxInverse(List<? extends Number> xValues, int degree) {
    double[][] design = new double[xValues.size()][degree + 1]
    for (int i = 0; i < xValues.size(); i++) {
      double x = xValues[i].doubleValue()
      double term = 1.0d
      for (int j = 0; j <= degree; j++) {
        design[i][j] = term
        term *= x
      }
    }
    def xMatrix = new Array2DRowRealMatrix(design, false)
    def xtx = xMatrix.transpose() * xMatrix
    new LUDecomposition(xtx).solver.inverse.data
  }
}
