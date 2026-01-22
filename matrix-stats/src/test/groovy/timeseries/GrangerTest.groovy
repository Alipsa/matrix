package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.Granger

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Granger causality test.
 */
class GrangerTest {

  @Test
  void testNoCausality() {
    // Independent random series - X should not Granger-cause Y
    double[] x = new double[50]
    double[] y = new double[50]

    Random rnd = new Random(42)
    for (int i = 0; i < 50; i++) {
      x[i] = rnd.nextGaussian()
      y[i] = rnd.nextGaussian()
    }

    def result = Granger.test(x, y, 2)

    assertNotNull(result)
    assertEquals(2, result.lags)
    assertEquals(50, result.sampleSize)

    // Should not detect causality
    assertTrue(result.pValue > 0.05, 'Should not detect causality in independent series')
  }

  @Test
  void testCausalityPresent() {
    // Y depends on lagged X - X should Granger-cause Y
    double[] x = new double[50]
    double[] y = new double[50]

    Random rnd = new Random(42)
    x[0] = rnd.nextGaussian()
    x[1] = rnd.nextGaussian()
    y[0] = rnd.nextGaussian()
    y[1] = rnd.nextGaussian()

    for (int i = 2; i < 50; i++) {
      x[i] = 0.5 * x[i-1] + rnd.nextGaussian() * 0.1
      y[i] = 0.3 * y[i-1] + 0.7 * x[i-1] + rnd.nextGaussian() * 0.1  // Y depends on X_{t-1}
    }

    def result = Granger.test(x, y, 2)

    assertNotNull(result)
    // Should detect causality
    assertTrue(result.pValue < 0.05, 'Should detect causality when X causes Y')
    assertTrue(result.statistic > 0, 'F-statistic should be positive')
  }

  @Test
  void testListInput() {
    List<Double> x = (1..30).collect { it + Math.random() * 0.1 }
    List<Double> y = (1..30).collect { it * 1.5 + Math.random() * 0.1 }

    def result = Granger.test(x, y, 2)

    assertNotNull(result)
    assertEquals(30, result.sampleSize)
  }

  @Test
  void testValidation() {
    double[] x = (1..30).collect { it as double } as double[]
    double[] y = (1..30).collect { it as double } as double[]

    // Null data
    assertThrows(IllegalArgumentException) {
      Granger.test(null as double[], y, 2)
    }

    assertThrows(IllegalArgumentException) {
      Granger.test(x, null as double[], 2)
    }

    // Mismatched lengths
    double[] yShort = [1, 2, 3] as double[]
    assertThrows(IllegalArgumentException) {
      Granger.test(x, yShort, 2)
    }

    // Too few observations
    double[] xSmall = [1, 2, 3, 4, 5] as double[]
    double[] ySmall = [1, 2, 3, 4, 5] as double[]
    assertThrows(IllegalArgumentException) {
      Granger.test(xSmall, ySmall, 2)
    }

    // Lag too large
    assertThrows(IllegalArgumentException) {
      Granger.test(x, y, 15)
    }
  }

  @Test
  void testDifferentLags() {
    double[] x = new double[50]
    double[] y = new double[50]

    Random rnd = new Random(42)
    for (int i = 0; i < 50; i++) {
      x[i] = i + Math.sin(i * 0.2) * 5 + rnd.nextGaussian() * 0.5
      y[i] = i * 1.5 + Math.cos(i * 0.3) * 3 + rnd.nextGaussian() * 0.5
    }

    def result1 = Granger.test(x, y, 1)
    def result2 = Granger.test(x, y, 2)
    def result3 = Granger.test(x, y, 3)

    assertEquals(1, result1.lags)
    assertEquals(2, result2.lags)
    assertEquals(3, result3.lags)

    // Effective sample sizes should decrease with more lags
    assertTrue(result1.effectiveSampleSize > result2.effectiveSampleSize)
    assertTrue(result2.effectiveSampleSize > result3.effectiveSampleSize)
  }

  @Test
  void testAutoLagSelection() {
    double[] x = new double[50]
    double[] y = new double[50]

    for (int i = 0; i < 50; i++) {
      x[i] = i + Math.random() * 2
      y[i] = i * 1.2 + Math.random() * 2
    }

    def result = Granger.test(x, y, null as Integer)

    assertNotNull(result)
    assertTrue(result.lags >= 1, 'Auto-selected lag should be at least 1')
    assertTrue(result.lags <= 10, 'Auto-selected lag should be reasonable')
  }

  @Test
  void testResultInterpret() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    String interpretation = result.interpret(0.05)
    assertNotNull(interpretation)
    assertTrue(interpretation.contains('H0'))
    assertTrue(interpretation.contains('F ='))
    assertTrue(interpretation.contains('p ='))
  }

  @Test
  void testResultEvaluate() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    String evaluation = result.evaluate()
    assertNotNull(evaluation)
    assertTrue(evaluation.contains('Granger causality'))
    assertTrue(evaluation.contains('F-statistic'))
    assertTrue(evaluation.contains('p-value'))
    assertTrue(evaluation.contains('RSS'))
    assertTrue(evaluation.contains('Conclusion'))
  }

  @Test
  void testResultToString() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    String str = result.toString()
    assertTrue(str.contains('Granger Causality'))
    assertTrue(str.contains('Lags'))
    assertTrue(str.contains('F-statistic'))
    assertTrue(str.contains('p-value'))
  }

  @Test
  void testRSSValues() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    // RSS values should be non-negative
    assertTrue(result.rssRestricted >= 0, 'RSS restricted should be non-negative')
    assertTrue(result.rssUnrestricted >= 0, 'RSS unrestricted should be non-negative')

    // Unrestricted should fit at least as well as restricted
    assertTrue(result.rssUnrestricted <= result.rssRestricted,
               'Unrestricted model should fit at least as well as restricted')
  }

  @Test
  void testDegreesOfFreedom() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    // df1 should equal number of lags
    assertEquals(2, result.df1)

    // df2 should equal effective sample size - 2*lags - 1
    assertEquals(30 - 2 - 2*2 - 1, result.df2)
  }

  @Test
  void testPValueRange() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    // p-value should be in [0, 1]
    assertTrue(result.pValue >= 0, 'p-value should be >= 0')
    assertTrue(result.pValue <= 1, 'p-value should be <= 1')
  }

  @Test
  void testFStatisticPositive() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    // F-statistic should be non-negative
    assertTrue(result.statistic >= 0, 'F-statistic should be non-negative')
  }

  @Test
  void testEffectiveSampleSize() {
    double[] x = (1..50).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..50).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 3)

    // Effective sample size should be original size minus lags
    assertEquals(50 - 3, result.effectiveSampleSize)
    assertEquals(50, result.sampleSize)
  }

  @Test
  void testDifferentAlphaLevels() {
    double[] x = (1..30).collect { it + Math.random() * 0.5 } as double[]
    double[] y = (1..30).collect { it * 1.5 + Math.random() * 0.5 } as double[]

    def result = Granger.test(x, y, 2)

    String interp01 = result.interpret(0.01)
    String interp05 = result.interpret(0.05)
    String interp10 = result.interpret(0.10)

    assertNotNull(interp01)
    assertNotNull(interp05)
    assertNotNull(interp10)
  }
}
