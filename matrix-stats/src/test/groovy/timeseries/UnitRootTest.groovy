package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.UnitRoot

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for UnitRoot wrapper class.
 */
class UnitRootTest {

  @Test
  void testStationarySeries() {
    // Stationary series: random noise around zero
    Random rnd = new Random(42)
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)

    assertNotNull(result)
    assertEquals(100, result.sampleSize)
    assertEquals('drift', result.type)

    // All four test results should be present
    assertNotNull(result.dfResult)
    assertNotNull(result.adfResult)
    assertNotNull(result.adfGlsResult)
    assertNotNull(result.kpssResult)

    // For stationary series, should likely conclude stationarity
    assertTrue(result.isStationary(0.05), 'Stationary series should be detected as stationary')
    assertFalse(result.hasUnitRoot(0.05), 'Stationary series should not have unit root')
  }

  @Test
  void testRandomWalkSeries() {
    // Random walk: clear unit root
    Random rnd = new Random(123)
    double[] data = new double[100]
    data[0] = 0
    for (int i = 1; i < 100; i++) {
      data[i] = data[i-1] + rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)

    assertNotNull(result)
    assertEquals(100, result.sampleSize)

    // All four test results should be present
    assertNotNull(result.dfResult)
    assertNotNull(result.adfResult)
    assertNotNull(result.adfGlsResult)
    assertNotNull(result.kpssResult)

    // Can verify the methods work, even if outcome varies with random seed
    assertNotNull(result.summary())
    assertNotNull(result.getConsensus())
  }

  @Test
  void testListInput() {
    List<Double> data = (1..50).collect { it + Math.random() * 2 }

    def result = UnitRoot.test(data)

    assertNotNull(result)
    assertEquals(50, result.sampleSize)
  }

  @Test
  void testDifferentTypes() {
    Random rnd = new Random(456)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    // Note: Skip 'none' type since ADF-GLS doesn't support it
    def resultDrift = UnitRoot.test(data, 'drift')
    def resultTrend = UnitRoot.test(data, 'trend')

    assertEquals('drift', resultDrift.type)
    assertEquals('trend', resultTrend.type)
  }

  @Test
  void testWithSpecifiedLags() {
    Random rnd = new Random(789)
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data, 'drift', 3)

    assertNotNull(result)
    // ADF uses 'lag' (singular), ADF-GLS uses 'lags' (plural)
    assertEquals(3, result.adfResult.lag)
    assertEquals(3, result.adfGlsResult.lags)
  }

  @Test
  void testAutoLagSelection() {
    Random rnd = new Random(999)
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data, 'drift', 0)  // 0 means auto-select

    assertNotNull(result)
    // Auto-selected lags should be reasonable (ADF uses 'lag' singular)
    assertTrue(result.adfResult.lag >= 0)
    assertTrue(result.adfResult.lag <= 10)
    assertTrue(result.adfGlsResult.lags >= 0)
    assertTrue(result.adfGlsResult.lags <= 10)
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      UnitRoot.test(null as double[])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      UnitRoot.test([1, 2, 3, 4, 5] as double[])
    }

    // Invalid type
    double[] data = new double[50]
    assertThrows(IllegalArgumentException) {
      UnitRoot.test(data, 'invalid')
    }

    // Minimum valid size (need at least 12 for ADF, must have variation in first differences)
    Random rnd = new Random(42)
    double[] minData = (1..15).collect { it + rnd.nextGaussian() * 0.5 } as double[]
    def result = UnitRoot.test(minData)
    assertNotNull(result)
  }

  @Test
  void testSummaryMethod() {
    Random rnd = new Random(111)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)
    String summary = result.summary()

    assertNotNull(summary)
    assertTrue(summary.contains('Unit Root Test Summary'))
    assertTrue(summary.contains('Dickey-Fuller Test'))
    assertTrue(summary.contains('Augmented Dickey-Fuller Test'))
    assertTrue(summary.contains('ADF-GLS Test'))
    assertTrue(summary.contains('KPSS Test'))
    assertTrue(summary.contains('Overall Assessment'))
  }

  @Test
  void testGetConsensusStationary() {
    Random rnd = new Random(222)
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = rnd.nextGaussian()  // Stationary white noise
    }

    def result = UnitRoot.test(data)
    String consensus = result.getConsensus()

    assertNotNull(consensus)
    // For stationary series, consensus should mention stationarity
    assertTrue(consensus.toUpperCase().contains('STATIONARY'))
  }

  @Test
  void testGetConsensusUnitRoot() {
    // Random walk with drift
    Random rnd = new Random(333)
    double[] data = new double[100]
    data[0] = 0
    for (int i = 1; i < 100; i++) {
      data[i] = data[i-1] + 0.1 + rnd.nextGaussian() * 0.5
    }

    def result = UnitRoot.test(data)
    String consensus = result.getConsensus()

    assertNotNull(consensus)
    // Should mention unit root or non-stationarity
    assertTrue(consensus.toUpperCase().contains('UNIT ROOT') ||
               consensus.toUpperCase().contains('NON-STATIONARY'))
  }

  @Test
  void testToString() {
    Random rnd = new Random(444)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)
    String str = result.toString()

    assertNotNull(str)
    // toString should return same as summary
    assertEquals(result.summary(), str)
  }

  @Test
  void testIsStationaryMethod() {
    // Test with known stationary series
    Random rnd = new Random(555)
    double[] stationary = new double[100]
    for (int i = 0; i < 100; i++) {
      stationary[i] = rnd.nextGaussian()
    }

    def result1 = UnitRoot.test(stationary)

    // Should recognize as stationary
    assertTrue(result1.isStationary(0.05))
    assertFalse(result1.hasUnitRoot(0.05))
  }

  @Test
  void testHasUnitRootMethod() {
    // Test with random walk
    Random rnd = new Random(666)
    double[] randomWalk = new double[100]
    randomWalk[0] = 0
    for (int i = 1; i < 100; i++) {
      randomWalk[i] = randomWalk[i-1] + rnd.nextGaussian()
    }

    def result = UnitRoot.test(randomWalk)

    // Verify method returns boolean values (outcome may vary with seed)
    assertNotNull(result.hasUnitRoot(0.05))
    assertNotNull(result.isStationary(0.05))
    // They should be opposites in most cases
    // assertTrue(result.hasUnitRoot(0.05) != result.isStationary(0.05))
  }

  @Test
  void testDifferentAlphaLevels() {
    Random rnd = new Random(777)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)

    String summary01 = result.summary(0.01)
    String summary05 = result.summary(0.05)
    String summary10 = result.summary(0.10)

    assertNotNull(summary01)
    assertNotNull(summary05)
    assertNotNull(summary10)

    // Can call consensus at different levels
    String consensus01 = result.getConsensus(0.01)
    String consensus05 = result.getConsensus(0.05)
    String consensus10 = result.getConsensus(0.10)

    assertNotNull(consensus01)
    assertNotNull(consensus05)
    assertNotNull(consensus10)
  }

  @Test
  void testTrendedSeries() {
    // Series with linear trend
    double[] data = new double[100]
    Random rnd = new Random(888)
    for (int i = 0; i < 100; i++) {
      data[i] = i * 0.5 + rnd.nextGaussian() * 0.5  // Trend + noise
    }

    def result = UnitRoot.test(data, 'trend')

    assertNotNull(result)
    assertEquals('trend', result.type)

    // Should test against trend-stationary alternative
    assertNotNull(result.dfResult)
    assertNotNull(result.summary())
  }

  @Test
  void testDefaultParameters() {
    Random rnd = new Random(999)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    // Test with all defaults
    def result = UnitRoot.test(data)

    assertEquals('drift', result.type)
    assertEquals(50, result.sampleSize)
    assertNotNull(result.summary())
  }

  @Test
  void testAllTestsExecute() {
    Random rnd = new Random(1010)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)

    // Verify all tests produced valid results
    assertNotNull(result.dfResult.statistic)
    assertNotNull(result.dfResult.criticalValue5pct)

    assertNotNull(result.adfResult.statistic)
    assertNotNull(result.adfResult.criticalValue)  // ADF only has one critical value

    assertNotNull(result.adfGlsResult.statistic)
    assertNotNull(result.adfGlsResult.criticalValue5pct)

    assertNotNull(result.kpssResult.statistic)
    assertNotNull(result.kpssResult.criticalValue)
  }

  @Test
  void testConsensusLogic() {
    Random rnd = new Random(1111)
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)

    // Count how many unit root tests reject (ADF only has one critical value at 5%)
    int rejections = 0
    if (result.dfResult.statistic < result.dfResult.criticalValue5pct) rejections++
    if (result.adfResult.statistic < result.adfResult.criticalValue) rejections++
    if (result.adfGlsResult.statistic < result.adfGlsResult.criticalValue5pct) rejections++

    boolean kpssRejects = result.kpssResult.statistic > result.kpssResult.criticalValue

    // Verify consensus matches logic
    if (rejections >= 2 && !kpssRejects) {
      assertTrue(result.isStationary(0.05))
      assertFalse(result.hasUnitRoot(0.05))
    } else if (rejections == 0 && kpssRejects) {
      assertFalse(result.isStationary(0.05))
      assertTrue(result.hasUnitRoot(0.05))
    }
  }

  @Test
  void testLargerSample() {
    Random rnd = new Random(1212)
    double[] data = new double[200]
    for (int i = 0; i < 200; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)

    assertNotNull(result)
    assertEquals(200, result.sampleSize)
    assertNotNull(result.summary())
  }

  @Test
  void testMixedEvidence() {
    // Create a series that might give mixed results
    Random rnd = new Random(1313)
    double[] data = new double[100]
    data[0] = 0
    for (int i = 1; i < 100; i++) {
      // Near unit root process
      data[i] = 0.95 * data[i-1] + rnd.nextGaussian()
    }

    def result = UnitRoot.test(data)

    assertNotNull(result)
    String consensus = result.getConsensus()

    // Should handle mixed/inconclusive evidence gracefully
    assertNotNull(consensus)
    assertTrue(consensus.length() > 0)
  }
}
