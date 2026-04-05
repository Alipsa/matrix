package normality

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.normality.JarqueBera

class JarqueBeraTest {

  @Test
  void testNormalLikeSampleProducesBigDecimalScalars() {
    List<Double> data = [2.3, 3.1, 2.8, 3.5, 2.9, 3.2, 3.0, 2.7, 3.4, 2.6]

    def result = JarqueBera.test(data)

    assertNotNull(result)
    assertTrue(result.jbStatistic instanceof BigDecimal)
    assertTrue(result.skewness instanceof BigDecimal)
    assertTrue(result.kurtosis instanceof BigDecimal)
    assertTrue(result.excessKurtosis instanceof BigDecimal)
    assertTrue(result.pValue instanceof BigDecimal)
    assertEquals(10, result.sampleSize)
    assertTrue(result.jbStatistic >= 0)
    assertTrue(result.pValue >= 0 && result.pValue <= 1)
  }

  @Test
  void testValidationRejectsInvalidSamples() {
    assertThrows(IllegalArgumentException) {
      JarqueBera.test(null)
    }
    assertThrows(IllegalArgumentException) {
      JarqueBera.test([])
    }
    assertThrows(IllegalArgumentException) {
      JarqueBera.test([1.0, 2.0, 3.0])
    }
    assertThrows(IllegalArgumentException) {
      JarqueBera.test([5.0, 5.0, 5.0, 5.0])
    }
  }

  @Test
  void testEvaluateUsesNumericAlphaInput() {
    List<Double> data = [1.0, 1.1, 1.2, 1.3, 1.4, 2.0, 3.0, 5.0, 10.0, 20.0]

    def result = JarqueBera.test(data)

    assertTrue(result.evaluate(0.05) == (result.pValue < 0.05))
    assertTrue(result.isNormal(0.05) == (result.pValue >= 0.05))
  }
}
