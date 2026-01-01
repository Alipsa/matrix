import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.regression.PolynomialRegression

import static org.junit.jupiter.api.Assertions.*

class PolynomialRegressionTest {

  @Test
  void testQuadraticFit() {
    // y = x^2 (perfect quadratic)
    def x = [1, 2, 3, 4, 5]
    def y = [1, 4, 9, 16, 25]

    def poly = new PolynomialRegression(x, y, 2)

    // Check predictions
    assertEquals(1.0, poly.predict(1).doubleValue(), 0.01)
    assertEquals(4.0, poly.predict(2).doubleValue(), 0.01)
    assertEquals(36.0, poly.predict(6).doubleValue(), 0.5)  // Extrapolation

    // R-squared should be very high for perfect fit
    assertTrue(poly.rsquared > 0.99, "R-squared should be > 0.99 for perfect fit")

    // Check degree
    assertEquals(2, poly.degree)
  }

  @Test
  void testLinearWithPolynomialDegree1() {
    // y = 2x + 1
    def x = [1, 2, 3, 4, 5]
    def y = [3, 5, 7, 9, 11]

    def poly = new PolynomialRegression(x, y, 1)

    // Should behave like linear regression
    assertEquals(3.0, poly.predict(1).doubleValue(), 0.01)
    assertEquals(13.0, poly.predict(6).doubleValue(), 0.01)

    // Check coefficients: y = a0 + a1*x where a0=1, a1=2
    assertEquals(1.0, poly.getCoefficient(0).doubleValue(), 0.01)
    assertEquals(2.0, poly.getCoefficient(1).doubleValue(), 0.01)
  }

  @Test
  void testCubicFit() {
    // y = x^3
    def x = [1, 2, 3, 4, 5]
    def y = [1, 8, 27, 64, 125]

    def poly = new PolynomialRegression(x, y, 3)

    assertEquals(1.0, poly.predict(1).doubleValue(), 0.1)
    assertEquals(27.0, poly.predict(3).doubleValue(), 0.1)
    assertEquals(216.0, poly.predict(6).doubleValue(), 1.0)

    assertEquals(3, poly.degree)
  }

  @Test
  void testBatchPrediction() {
    def x = [1, 2, 3, 4, 5]
    def y = [1, 4, 9, 16, 25]

    def poly = new PolynomialRegression(x, y, 2)

    def predictions = poly.predict([1, 2, 3])
    assertEquals(3, predictions.size())
    assertEquals(1.0, predictions[0].doubleValue(), 0.01)
    assertEquals(4.0, predictions[1].doubleValue(), 0.01)
    assertEquals(9.0, predictions[2].doubleValue(), 0.01)
  }

  @Test
  void testToString() {
    def x = [1, 2, 3, 4, 5]
    def y = [1, 4, 9, 16, 25]

    def poly = new PolynomialRegression(x, y, 2)
    def str = poly.toString()

    assertTrue(str.contains('Y ='), "toString should start with 'Y ='")
    assertTrue(str.contains('X^2'), "toString should contain 'X^2' for quadratic")
  }

  @Test
  void testSummary() {
    def x = [1, 2, 3, 4, 5]
    def y = [1, 4, 9, 16, 25]

    def poly = new PolynomialRegression(x, y, 2)
    def summary = poly.summary()

    assertTrue(summary.contains('Polynomial Regression'), "Summary should contain title")
    assertTrue(summary.contains('R-squared'), "Summary should contain R-squared")
    assertTrue(summary.contains('degree 2'), "Summary should mention degree")
  }

  @Test
  void testInvalidDegree() {
    def x = [1, 2, 3]
    def y = [1, 4, 9]

    // Degree must be at least 1
    assertThrows(IllegalArgumentException) {
      new PolynomialRegression(x, y, 0)
    }
  }

  @Test
  void testInsufficientData() {
    def x = [1, 2]
    def y = [1, 4]

    // Need more points than degree
    assertThrows(IllegalArgumentException) {
      new PolynomialRegression(x, y, 2)
    }
  }
}
