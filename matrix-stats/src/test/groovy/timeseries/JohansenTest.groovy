package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.Johansen

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Johansen cointegration test.
 */
class JohansenTest {

  @Test
  void testCointegratedSeries() {
    // Create two cointegrated series
    // y1 is a random walk, y2 = y1 + noise (should be cointegrated)
    Random rnd = new Random(42)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian() * 0.5
      y2[i] = y1[i] + rnd.nextGaussian() * 0.1  // Cointegrated with y1
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    assertEquals(2, result.numVariables)
    assertEquals(1, result.lags)
    assertEquals('const', result.type)

    // Should have 2 eigenvalues
    assertEquals(2, result.eigenvalues.length)
    assertEquals(2, result.traceStatistics.length)

    // Eigenvalues should be between 0 and 1
    for (double eigen : result.eigenvalues) {
      assertTrue(eigen >= 0 && eigen <= 1, "Eigenvalue should be in [0,1]: ${eigen}")
    }

    // Trace statistics should be positive
    for (double trace : result.traceStatistics) {
      assertTrue(trace >= 0, "Trace statistic should be non-negative: ${trace}")
    }
  }

  @Test
  void testIndependentSeries() {
    // Create two independent random walks (not cointegrated)
    Random rnd = new Random(123)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y2[i-1] + rnd.nextGaussian()  // Independent random walk
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    assertEquals(2, result.numVariables)

    // Should detect no cointegration (typically)
    assertNotNull(result.interpret())
  }

  @Test
  void testThreeVariables() {
    // Test with three variables
    Random rnd = new Random(456)
    int n = 80
    double[] y1 = new double[n]
    double[] y2 = new double[n]
    double[] y3 = new double[n]

    y1[0] = 0
    y2[0] = 0
    y3[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian() * 0.5
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
      y3[i] = y1[i] + y2[i] + rnd.nextGaussian() * 0.3
    }

    def result = Johansen.test([y1, y2, y3], 1)

    assertNotNull(result)
    assertEquals(3, result.numVariables)
    assertEquals(3, result.eigenvalues.length)
    assertEquals(3, result.traceStatistics.length)
  }

  @Test
  void testDifferentLags() {
    Random rnd = new Random(789)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result1 = Johansen.test([y1, y2], 1)
    def result2 = Johansen.test([y1, y2], 2)
    def result3 = Johansen.test([y1, y2], 3)

    assertEquals(1, result1.lags)
    assertEquals(2, result2.lags)
    assertEquals(3, result3.lags)

    // Different lags should give different sample sizes
    assertTrue(result1.sampleSize > result2.sampleSize)
    assertTrue(result2.sampleSize > result3.sampleSize)
  }

  @Test
  void testDifferentTypes() {
    Random rnd = new Random(111)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def resultNone = Johansen.test([y1, y2], 1, 'none')
    def resultConst = Johansen.test([y1, y2], 1, 'const')
    def resultTrend = Johansen.test([y1, y2], 1, 'trend')

    assertEquals('none', resultNone.type)
    assertEquals('const', resultConst.type)
    assertEquals('trend', resultTrend.type)
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      Johansen.test(null, 1)
    }

    // Empty list
    assertThrows(IllegalArgumentException) {
      Johansen.test([], 1)
    }

    // Invalid lags
    double[] y1 = new double[100]
    double[] y2 = new double[100]
    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y2], 0)
    }

    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y2], -1)
    }

    // Invalid type
    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y2], 1, 'invalid')
    }

    // Mismatched lengths
    double[] y3 = new double[50]
    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y3], 1)
    }

    // Too few observations
    double[] ySmall1 = new double[15]
    double[] ySmall2 = new double[15]
    assertThrows(IllegalArgumentException) {
      Johansen.test([ySmall1, ySmall2], 1)
    }
  }

  @Test
  void testResultInterpret() {
    Random rnd = new Random(222)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)
    String interpretation = result.interpret()

    assertNotNull(interpretation)
    assertTrue(interpretation.contains('Johansen'))
    assertTrue(interpretation.contains('H0'))
    assertTrue(interpretation.contains('Conclusion'))
  }

  @Test
  void testResultToString() {
    Random rnd = new Random(333)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)
    String str = result.toString()

    assertTrue(str.contains('Johansen'))
    assertTrue(str.contains('Number of variables'))
    assertTrue(str.contains('Sample size'))
    assertTrue(str.contains('Lags'))
    assertTrue(str.contains('Eigenvalues'))
  }

  @Test
  void testEigenvalueOrder() {
    Random rnd = new Random(444)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    // Eigenvalues should be sorted in descending order
    for (int i = 0; i < result.eigenvalues.length - 1; i++) {
      assertTrue(result.eigenvalues[i] >= result.eigenvalues[i+1],
                 "Eigenvalues should be in descending order")
    }
  }

  @Test
  void testTraceStatisticOrder() {
    Random rnd = new Random(555)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    // Trace statistics should decrease as r increases
    for (int i = 0; i < result.traceStatistics.length - 1; i++) {
      assertTrue(result.traceStatistics[i] >= result.traceStatistics[i+1],
                 "Trace statistics should be non-increasing")
    }
  }

  @Test
  void testCriticalValues() {
    Random rnd = new Random(666)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    // Critical values should be available
    assertNotNull(result.criticalValues5pct)
    assertTrue(result.criticalValues5pct.length > 0)
  }

  @Test
  void testFourVariables() {
    // Test with four variables
    Random rnd = new Random(777)
    int n = 80
    double[] y1 = new double[n]
    double[] y2 = new double[n]
    double[] y3 = new double[n]
    double[] y4 = new double[n]

    y1[0] = 0
    y2[0] = 0
    y3[0] = 0
    y4[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian() * 0.5
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
      y3[i] = y1[i] + y2[i] + rnd.nextGaussian() * 0.3
      y4[i] = y2[i] + rnd.nextGaussian() * 0.25
    }

    def result = Johansen.test([y1, y2, y3, y4], 1)

    assertNotNull(result)
    assertEquals(4, result.numVariables)
    assertEquals(4, result.eigenvalues.length)
    assertEquals(4, result.traceStatistics.length)
  }

  @Test
  void testStationarySeries() {
    // Stationary series should not be cointegrated (already stationary)
    Random rnd = new Random(888)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    for (int i = 0; i < n; i++) {
      y1[i] = rnd.nextGaussian()
      y2[i] = rnd.nextGaussian()
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    // Test completes without error
    assertNotNull(result.interpret())
  }

  @Test
  void testLongerLags() {
    Random rnd = new Random(999)
    int n = 150
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 5)

    assertEquals(5, result.lags)
    assertNotNull(result.eigenvalues)
    assertTrue(result.sampleSize < n, "Effective sample size should be less than n")
  }

  @Test
  void testMinimumSampleSize() {
    Random rnd = new Random(1010)
    int n = 25  // Minimum: lags + 20 = 1 + 20 = 21
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    assertEquals(n - 1, result.sampleSize)
  }
}
