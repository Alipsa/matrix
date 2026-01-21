package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.TurningPoint

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Turning Point test for randomness.
 */
class TurningPointTest {

  @Test
  void testRandomSeries() {
    // Random series should have turning points close to expected
    Random rnd = new Random(42)
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(100, result.sampleSize)
    assertEquals(98, result.possibleTurningPoints)

    // Expected turning points for n=100: 2(100-2)/3 = 65.33
    assertTrue(Math.abs(result.expectedTurningPoints - 65.33) < 0.01)

    // For random data, should typically not reject H0
    assertTrue(result.pValue > 0.01, 'Random series should typically appear random')
  }

  @Test
  void testMonotonicIncreasing() {
    // Monotonic series should have ZERO turning points (strong trend)
    double[] data = (1..50).collect { it as double } as double[]

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(0, result.turningPoints)
    assertEquals(0, result.peaks)
    assertEquals(0, result.troughs)

    // Expected: 2(50-2)/3 = 32
    assertTrue(Math.abs(result.expectedTurningPoints - 32.0) < 0.01)

    // Should detect as non-random (much fewer turning points than expected)
    assertTrue(result.pValue < 0.05, 'Monotonic series should be detected as non-random')
    assertTrue(result.statistic < 0, 'Statistic should be negative for fewer turning points')
  }

  @Test
  void testMonotonicDecreasing() {
    // Monotonic decreasing series should have zero turning points
    double[] data = (1..50).collect { 51 - it as double } as double[]

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(0, result.turningPoints)
    assertEquals(0, result.peaks)
    assertEquals(0, result.troughs)

    // Should detect as non-random
    assertTrue(result.pValue < 0.05, 'Monotonic series should be detected as non-random')
  }

  @Test
  void testAlternatingPattern() {
    // Alternating pattern: 1, 2, 1, 2, 1, 2, ... has maximum turning points
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = (i % 2 == 0) ? 1.0 : 2.0
    }

    def result = TurningPoint.test(data)

    assertNotNull(result)
    // For alternating pattern, every interior point is a turning point
    assertEquals(48, result.turningPoints)
    assertEquals(24, result.peaks)
    assertEquals(24, result.troughs)

    // Expected: 2(50-2)/3 = 32
    assertTrue(Math.abs(result.expectedTurningPoints - 32.0) < 0.01)

    // Should detect as non-random (too many turning points)
    assertTrue(result.pValue < 0.05, 'Alternating pattern should be detected as non-random')
    assertTrue(result.statistic > 0, 'Statistic should be positive for more turning points')
  }

  @Test
  void testCyclicPattern() {
    // Regular cyclic pattern: repeating [1, 2, 3, 2] creates regular peaks
    // This creates a sawtooth wave with consistent turning points
    double[] data = new double[100]
    double[] pattern = [1, 2, 3, 2] as double[]
    for (int i = 0; i < 100; i++) {
      data[i] = pattern[i % 4]
    }

    def result = TurningPoint.test(data)

    assertNotNull(result)
    // This pattern should have many turning points (peak at every 3rd position)
    // Expected: about 25 peaks in 100 points (every 4th point cycle, peak at position 2)
    assertTrue(result.turningPoints > 20, 'Cyclic pattern should have many turning points')

    // Should detect as non-random (cyclicity)
    assertTrue(result.pValue < 0.05, 'Cyclic pattern should be detected as non-random')
  }

  @Test
  void testListInput() {
    List<Double> data = (1..30).collect { it + Math.random() * 2 }

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(30, result.sampleSize)
    assertEquals(28, result.possibleTurningPoints)
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      TurningPoint.test(null as double[])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      TurningPoint.test([1, 2] as double[])
    }

    // Exactly 3 observations should work
    def result = TurningPoint.test([1, 2, 1] as double[])
    assertNotNull(result)
    assertEquals(3, result.sampleSize)
    assertEquals(1, result.possibleTurningPoints)
  }

  @Test
  void testPeaksAndTroughs() {
    // Test correct identification of peaks and troughs
    // Pattern: 1, 3, 2, 4, 1, 5, 2
    // Peaks at index 1 (3) and 3 (4) and 5 (5)
    // Troughs at index 2 (2) and 4 (1) and 6 (2)
    double[] data = [1, 3, 2, 4, 1, 5, 2] as double[]

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(5, result.turningPoints)  // 5 turning points total
    assertEquals(3, result.peaks)          // 3 peaks
    assertEquals(2, result.troughs)        // 2 troughs
  }

  @Test
  void testConstantSeries() {
    // Constant series has no turning points (all values equal)
    double[] data = new double[30]
    Arrays.fill(data, 5.0)

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(0, result.turningPoints)
    assertEquals(0, result.peaks)
    assertEquals(0, result.troughs)

    // Should be detected as non-random (trend-like)
    assertTrue(result.pValue < 0.05)
  }

  @Test
  void testSmallSample() {
    // Small sample with known turning points
    double[] data = [1, 3, 2, 4, 3, 5] as double[]

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(6, result.sampleSize)
    assertEquals(4, result.possibleTurningPoints)
    assertEquals(4, result.turningPoints)  // All interior points are turning points
    assertEquals(2, result.peaks)
    assertEquals(2, result.troughs)
  }

  @Test
  void testExpectedValue() {
    // Test expected value calculation
    double[] data = new double[50]
    Random rnd = new Random(123)
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = TurningPoint.test(data)

    // E[T] = 2(n-2)/3 = 2(48)/3 = 32
    assertEquals(32.0, result.expectedTurningPoints, 0.001)
  }

  @Test
  void testVariance() {
    // Test variance calculation
    double[] data = new double[50]
    Random rnd = new Random(456)
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = TurningPoint.test(data)

    // Var[T] = (16n - 29)/90 = (16*50 - 29)/90 = (800-29)/90 = 771/90 = 8.5667
    assertEquals(8.5667, result.variance, 0.001)
  }

  @Test
  void testResultInterpret() {
    double[] data = (1..30).collect { it + Math.random() * 2 } as double[]

    def result = TurningPoint.test(data)

    String interpretation = result.interpret(0.05)
    assertNotNull(interpretation)
    assertTrue(interpretation.contains('H0'))
    assertTrue(interpretation.contains('Z ='))
    assertTrue(interpretation.contains('p ='))
  }

  @Test
  void testResultEvaluate() {
    double[] data = (1..30).collect { it + Math.random() * 2 } as double[]

    def result = TurningPoint.test(data)

    String evaluation = result.evaluate()
    assertNotNull(evaluation)
    assertTrue(evaluation.contains('Turning Point test'))
    assertTrue(evaluation.contains('Sample size'))
    assertTrue(evaluation.contains('Z-statistic'))
    assertTrue(evaluation.contains('p-value'))
    assertTrue(evaluation.contains('Conclusion'))
  }

  @Test
  void testResultToString() {
    double[] data = [1, 3, 2, 4, 3, 5, 4, 6, 5] as double[]

    def result = TurningPoint.test(data)

    String str = result.toString()
    assertTrue(str.contains('Turning Point Test'))
    assertTrue(str.contains('Sample size'))
    assertTrue(str.contains('Turning points'))
    assertTrue(str.contains('Expected'))
    assertTrue(str.contains('Z-statistic'))
    assertTrue(str.contains('p-value'))
  }

  @Test
  void testPValueRange() {
    Random rnd = new Random(789)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = TurningPoint.test(data)

    // p-value should be in [0, 1]
    assertTrue(result.pValue >= 0, 'p-value should be >= 0')
    assertTrue(result.pValue <= 1, 'p-value should be <= 1')
  }

  @Test
  void testDifferentAlphaLevels() {
    double[] data = (1..30).collect { it + Math.random() * 2 } as double[]

    def result = TurningPoint.test(data)

    String interp01 = result.interpret(0.01)
    String interp05 = result.interpret(0.05)
    String interp10 = result.interpret(0.10)

    assertNotNull(interp01)
    assertNotNull(interp05)
    assertNotNull(interp10)
  }

  @Test
  void testNegativeValues() {
    // Test with negative values
    double[] data = [-5, -2, -8, -1, -9, -3, -7, -4] as double[]

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(8, result.sampleSize)
    assertTrue(result.turningPoints >= 0)
    assertTrue(result.peaks >= 0)
    assertTrue(result.troughs >= 0)
  }

  @Test
  void testMixedPositiveNegative() {
    // Test with mixed positive and negative values
    double[] data = [1, -2, 3, -4, 5, -6, 7, -8, 9] as double[]

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(9, result.sampleSize)
    // Every interior point is a turning point
    assertEquals(7, result.turningPoints)
  }

  @Test
  void testLargeSample() {
    // Test with larger sample size
    Random rnd = new Random(999)
    double[] data = new double[200]
    for (int i = 0; i < 200; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = TurningPoint.test(data)

    assertNotNull(result)
    assertEquals(200, result.sampleSize)
    assertEquals(198, result.possibleTurningPoints)

    // Expected: 2(198)/3 = 132
    assertEquals(132.0, result.expectedTurningPoints, 0.001)

    // Variance: (16*200 - 29)/90 = (3200-29)/90 = 3171/90 = 35.2333
    assertEquals(35.2333, result.variance, 0.001)
  }

  @Test
  void testStatisticSign() {
    // More turning points than expected → positive statistic
    double[] alternating = new double[30]
    for (int i = 0; i < 30; i++) {
      alternating[i] = (i % 2 == 0) ? 1.0 : 2.0
    }

    def result1 = TurningPoint.test(alternating)
    assertTrue(result1.statistic > 0, 'More turning points should give positive statistic')

    // Fewer turning points than expected → negative statistic
    double[] monotonic = (1..30).collect { it as double } as double[]

    def result2 = TurningPoint.test(monotonic)
    assertTrue(result2.statistic < 0, 'Fewer turning points should give negative statistic')
  }
}
