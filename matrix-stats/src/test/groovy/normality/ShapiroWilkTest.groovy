package normality

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.normality.ShapiroWilk

/**
 * Regression tests for the Shapiro-Wilk normality test.
 */
class ShapiroWilkTest {

  @Test
  void testPerfectWilkStatisticReturnsFinitePValue() {
    def method = ShapiroWilk.getDeclaredMethod('calculatePValue', Double.TYPE, Integer.TYPE)
    method.accessible = true

    double pValue = method.invoke(null, 1.0d, 10) as double

    assertEquals(1.0d, pValue, 0.0d)
    assertTrue(Double.isFinite(pValue))
  }
}
