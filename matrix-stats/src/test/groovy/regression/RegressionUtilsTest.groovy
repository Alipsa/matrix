package regression

import static org.junit.jupiter.api.Assertions.*

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
}
