package timeseries

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.timeseries.Kpss

/**
 * Tests for the KPSS stationarity test.
 */
class KpssTest {

  @Test
  void testLevelStationary() {
    // Stationary AR(1) process around a constant mean
    List<Double> data = []
    Random rnd = new Random(42)
    double y = 0.0
    for (int i = 0; i < 100; i++) {
      y = 0.3 * y + rnd.nextGaussian()
      data.add(y + 10.0)  // Add constant level
    }

    def result = Kpss.test(data, 'level')

    assertNotNull(result, 'Result should not be null')
    assertEquals('level', result.type, 'Type should be level')
    assertEquals(100, result.sampleSize, 'Sample size')
    assertTrue(result.statistic >= 0, 'KPSS statistic should be non-negative')
  }

  @Test
  void testTrendStationary() {
    // Trend-stationary series
    List<Double> data = []
    Random rnd = new Random(123)
    for (int i = 0; i < 100; i++) {
      data.add(i * 0.5 + rnd.nextGaussian())  // Linear trend + noise
    }

    def result = Kpss.test(data, 'trend')

    assertNotNull(result, 'Result should not be null')
    assertEquals('trend', result.type, 'Type should be trend')
    assertTrue(result.statistic >= 0, 'KPSS statistic should be non-negative')
  }

  @Test
  void testRandomWalk() {
    // Random walk (non-stationary) - should reject H0
    List<Double> data = []
    Random rnd = new Random(456)
    double value = 100.0
    for (int i = 0; i < 100; i++) {
      value += rnd.nextGaussian()
      data.add(value)
    }

    def result = Kpss.test(data, 'level')

    assertNotNull(result, 'Result should not be null')
    // For random walk, we expect higher KPSS statistic
    assertTrue(result.statistic >= 0, 'KPSS statistic should be non-negative')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      Kpss.test(null)
    }

    // Empty data
    assertThrows(IllegalArgumentException) {
      Kpss.test([])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      Kpss.test([1.0, 2.0, 3.0, 4.0])
    }

    // Invalid type
    List<Double> validData = (1..50).collect { it + Math.random() }
    assertThrows(IllegalArgumentException) {
      Kpss.test(validData, 'invalid')
    }
  }

  @Test
  void testDifferentTypes() {
    List<Double> data = []
    Random rnd = new Random(789)
    for (int i = 0; i < 100; i++) {
      data.add(i * 0.3 + rnd.nextGaussian())
    }

    def resultLevel = Kpss.test(data, 'level')
    def resultTrend = Kpss.test(data, 'trend')

    assertEquals('level', resultLevel.type, 'Level type')
    assertEquals('trend', resultTrend.type, 'Trend type')

    // Critical values should be different
    assertTrue(resultLevel.criticalValue > resultTrend.criticalValue,
               'Level critical value should be larger than trend')
  }

  @Test
  void testCustomLags() {
    List<Double> data = []
    Random rnd = new Random(321)
    double y = 0.0
    for (int i = 0; i < 100; i++) {
      y = 0.5 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result1 = Kpss.test(data, 'level', 1)
    def result2 = Kpss.test(data, 'level', 5)

    assertEquals(1, result1.lags, 'Should use 1 lag')
    assertEquals(5, result2.lags, 'Should use 5 lags')
  }

  @Test
  void testExplicitZeroAndLargeSupportedLagAreHonored() {
    List<Double> data = (1..20).collect { int value -> value + (value % 3) * 0.25d }

    def zeroLag = Kpss.test(data, 'level', 0)
    def tenLags = Kpss.test(data, 'level', 10)

    assertEquals(0, zeroLag.lags)
    assertEquals(10, tenLags.lags)

    double mean = data.sum() / data.size()
    List<Double> residuals = data.collect { double value -> value - mean }
    double variance = residuals.sum { double value -> value * value } / data.size()
    double partialSum = 0.0d
    double numerator = 0.0d
    residuals.each { double value ->
      partialSum += value
      numerator += partialSum * partialSum
    }
    BigDecimal expected = BigDecimal.valueOf(numerator / (data.size() * data.size() * variance))
    assertEquals(expected, zeroLag.statistic)
  }

  @Test
  void testInvalidExplicitLagsAreRejected() {
    List<Integer> data = (1..20).toList()

    IllegalArgumentException negative = assertThrows(IllegalArgumentException) {
      Kpss.test(data, 'level', -1)
    }
    assertEquals('Lags must be between 0 and 19, got: -1', negative.message)

    IllegalArgumentException excessive = assertThrows(IllegalArgumentException) {
      Kpss.test(data, 'level', 20)
    }
    assertEquals('Lags must be between 0 and 19, got: 20', excessive.message)

    List<Double> nonFinite = (1..20).collect { int value -> value as double }
    nonFinite[5] = Double.NaN
    IllegalArgumentException lagBeforeFiniteness = assertThrows(IllegalArgumentException) {
      Kpss.test(nonFinite, 'level', -1)
    }
    assertEquals('Lags must be between 0 and 19, got: -1', lagBeforeFiniteness.message)
  }

  @Test
  void testDegenerateAndNonFiniteInputsHaveSpecificDiagnostics() {
    IllegalArgumentException constant = assertThrows(IllegalArgumentException) {
      Kpss.test((1..20).collect { 5.0d }, 'level')
    }
    assertEquals('Data has no variation (constant series)', constant.message)

    IllegalArgumentException trend = assertThrows(IllegalArgumentException) {
      Kpss.test((1..20).toList(), 'trend')
    }
    assertEquals('Detrended residuals have no variation; cannot perform KPSS test', trend.message)

    [Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY].each { double invalid ->
      List<Double> data = (1..20).collect { int value -> value as double }
      data[5] = invalid
      IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
        Kpss.test(data)
      }
      assertEquals('Data contains non-finite values', exception.message)
    }
  }

  @Test
  void testArithmeticOverflowDiagnostics() {
    List<Double> varianceOverflow = (0..<12).collect { int index -> index % 2 == 0 ? -1e200d : 1e200d }
    IllegalArgumentException varianceException = assertThrows(IllegalArgumentException) {
      Kpss.test(varianceOverflow, 'level', 3)
    }
    assertEquals('KPSS calculation produced a non-finite long-run variance', varianceException.message)

    List<Double> statisticOverflow = [*([3e153d] * 6), *([-3e153d] * 6)]
    IllegalArgumentException statisticException = assertThrows(IllegalArgumentException) {
      Kpss.test(statisticOverflow, 'level', 0)
    }
    assertEquals('KPSS calculation produced a non-finite statistic', statisticException.message)
  }

  @Test
  void testLargeSampleStatisticDenominatorDoesNotOverflow() {
    List<Integer> data = (0..<50_000).collect { int index -> index % 11 }

    def result = Kpss.test(data, 'level', 0)

    assertTrue(result.statistic >= 0G)
    assertTrue(result.statistic < 1G)
  }

  @Test
  void testAutoLagSelection() {
    List<Double> data = []
    Random rnd = new Random(654)
    double y = 0.0
    for (int i = 0; i < 100; i++) {
      y = 0.5 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Kpss.test(data, 'level')

    assertTrue(result.lags > 0, 'Auto-selected lags should be positive')
    assertTrue(result.lags < 100, 'Auto-selected lags should be reasonable')
  }

  @Test
  void testResultToString() {
    List<Double> data = []
    Random rnd = new Random(987)
    double y = 0.0
    for (int i = 0; i < 50; i++) {
      y = 0.4 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Kpss.test(data)

    String str = result
    assertTrue(str.contains('KPSS Stationarity Test'), 'Should contain test name')
    assertTrue(str.contains('Sample size'), 'Should contain sample size')
    assertTrue(str.contains('KPSS statistic'), 'Should contain test statistic')
  }

  @Test
  void testInterpret() {
    List<Double> data = []
    Random rnd = new Random(147)
    double y = 0.0
    for (int i = 0; i < 50; i++) {
      y = 0.5 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Kpss.test(data)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention null hypothesis')
  }

  @Test
  void testEvaluate() {
    List<Double> data = []
    Random rnd = new Random(258)
    double y = 0.0
    for (int i = 0; i < 50; i++) {
      y = 0.6 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Kpss.test(data)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('KPSS statistic'), 'Should contain statistic info')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testStatisticProperties() {
    // KPSS statistic should always be non-negative
    List<Double> data = []
    Random rnd = new Random(369)
    double y = 0.0
    for (int i = 0; i < 50; i++) {
      y = 0.7 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Kpss.test(data)

    assertTrue(result.statistic >= 0, 'KPSS statistic should be non-negative')
    assertTrue(result.criticalValue > 0, 'Critical value should be positive')
  }

  @Test
  void testComplementaryToADF() {
    // KPSS and ADF test opposite hypotheses
    // KPSS: H0 = stationary, H1 = unit root
    // ADF: H0 = unit root, H1 = stationary
    // They are complementary tools

    List<Double> data = []
    Random rnd = new Random(741)
    double y = 0.0
    for (int i = 0; i < 50; i++) {
      y = 0.5 * y + rnd.nextGaussian()
      data.add(y)
    }

    def kpssResult = Kpss.test(data, 'level')

    assertNotNull(kpssResult, 'KPSS result should not be null')
    assertEquals('level', kpssResult.type, 'Type should be level')
  }
}
