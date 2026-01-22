package contingency

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.contingency.ChiSquared

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Chi-Squared tests (Pearson, G-test, Yates).
 */
class ChiSquaredTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testPearsonBasic2x2() {
    // Simple 2×2 table
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[10, 20], [30, 40]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals('Pearson', result.testType)
    assertEquals(1, result.degreesOfFreedom, 'df for 2×2 table should be 1')
    assertTrue(result.testStatistic >= 0, 'Test statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
  }

  @Test
  void testPearson3x3() {
    // 3×3 contingency table
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[10, 15, 20], [12, 18, 25], [8, 10, 15]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals(4, result.degreesOfFreedom, 'df for 3×3 table should be 4')
    assertTrue(result.testStatistic >= 0, 'Test statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
  }

  @Test
  void testPearsonIndependence() {
    // Table with perfect independence: proportions are the same across rows
    // Row 1: 10/30 = 1/3 in col 1, 20/30 = 2/3 in col 2
    // Row 2: 20/60 = 1/3 in col 1, 40/60 = 2/3 in col 2
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[10, 20], [20, 40]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.testStatistic < 0.01, 'Test statistic should be near 0 for independent data')
    assertTrue(result.pValue > 0.9, 'p-value should be high for independent data')
  }

  @Test
  void testPearsonDependence() {
    // Table with strong dependence
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[50, 5], [5, 50]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.testStatistic > 10, 'Test statistic should be large for dependent data')
    assertTrue(result.pValue < 0.001, 'p-value should be very small for strong dependence')
  }

  @Test
  void testGTestBasic() {
    // Simple 2×2 table for G-test
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[10, 20], [30, 40]])
      .build()

    def result = ChiSquared.gTest(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals('G-test', result.testType)
    assertEquals(1, result.degreesOfFreedom, 'df for 2×2 table should be 1')
    assertTrue(result.testStatistic >= 0, 'G statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
  }

  @Test
  void testGTestWithZeros() {
    // Table with some zero values (should handle gracefully)
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[0, 20], [30, 40]])
      .build()

    def result = ChiSquared.gTest(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.testStatistic >= 0, 'G statistic should be non-negative even with zeros')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
  }

  @Test
  void testGTestIndependence() {
    // Table with perfect independence
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[10, 20], [20, 40]])
      .build()

    def result = ChiSquared.gTest(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.testStatistic < 0.01, 'G statistic should be near 0 for independent data')
    assertTrue(result.pValue > 0.9, 'p-value should be high for independent data')
  }

  @Test
  void testGTestVsPearson() {
    // G-test and Pearson should give similar results
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[15, 25], [35, 45]])
      .build()

    def pearsonResult = ChiSquared.pearsonTest(table)
    def gResult = ChiSquared.gTest(table)

    // Test statistics should be similar (within 20% for moderate sample sizes)
    double diff = Math.abs(pearsonResult.testStatistic - gResult.testStatistic)
    double avg = (pearsonResult.testStatistic + gResult.testStatistic) / 2
    assertTrue(diff / avg < 0.2, 'Pearson and G-test should give similar results')
  }

  @Test
  void testYatesBasic() {
    // Simple 2×2 table for Yates' correction
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[10, 20], [30, 40]])
      .build()

    def result = ChiSquared.yatesTest(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals('Yates', result.testType)
    assertEquals(1, result.degreesOfFreedom, 'df for Yates test should be 1')
    assertTrue(result.testStatistic >= 0, 'Test statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
  }

  @Test
  void testYatesVsPearson() {
    // Yates' correction should give more conservative results (higher p-value)
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[12, 8], [6, 14]])
      .build()

    def pearsonResult = ChiSquared.pearsonTest(table)
    def yatesResult = ChiSquared.yatesTest(table)

    // Yates should have smaller test statistic (more conservative)
    assertTrue(yatesResult.testStatistic <= pearsonResult.testStatistic,
               'Yates correction should give smaller or equal test statistic')

    // Yates should have larger p-value (more conservative)
    assertTrue(yatesResult.pValue >= pearsonResult.pValue - 0.001,
               'Yates correction should give larger or equal p-value')
  }

  @Test
  void testYatesSmallSample() {
    // Yates' correction is particularly important for small samples
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[5, 3], [2, 6]])
      .build()

    def result = ChiSquared.yatesTest(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.testStatistic >= 0, 'Test statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
  }

  @Test
  void testValidation() {
    // Null table
    assertThrows(IllegalArgumentException) {
      ChiSquared.pearsonTest(null)
    }

    // Table too small (1×1)
    def small = Matrix.builder().matrixName('test').rows([[5]]).build()
    assertThrows(IllegalArgumentException) {
      ChiSquared.pearsonTest(small)
    }

    // Table with negative value
    def negative = Matrix.builder().matrixName('test').rows([[10, -5], [5, 10]]).build()
    assertThrows(IllegalArgumentException) {
      ChiSquared.pearsonTest(negative)
    }

    // Yates on non-2×2 table
    def non2x2 = Matrix.builder().matrixName('test').rows([[10, 20, 30], [40, 50, 60]]).build()
    assertThrows(IllegalArgumentException) {
      ChiSquared.yatesTest(non2x2)
    }
  }

  @Test
  void testPearsonExpectedTooSmall() {
    // Very sparse table where expected frequency < 1
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[1, 0, 0, 0, 0], [0, 0, 0, 0, 1]])
      .build()

    assertThrows(IllegalArgumentException) {
      ChiSquared.pearsonTest(table)
    }
  }

  @Test
  void testResultEvaluate() {
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[50, 5], [5, 50]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    // Should reject null hypothesis at 0.05 level
    assertTrue(result.evaluate(0.05), 'Should reject H0 at 0.05 for strong dependence')
    assertTrue(result.evaluate(0.01), 'Should reject H0 at 0.01 for strong dependence')

    // Test with independent data
    def indepTable = Matrix.builder()
      .matrixName('test')
      .rows([[10, 20], [20, 40]])
      .build()

    def indepResult = ChiSquared.pearsonTest(indepTable)
    assertFalse(indepResult.evaluate(0.05), 'Should not reject H0 at 0.05 for independence')
  }

  @Test
  void testResultToString() {
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[10, 20], [30, 40]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    String str = result.toString()
    assertTrue(str.contains('Chi-Squared'), 'Should contain test name')
    assertTrue(str.contains('Pearson'), 'Should contain test type')
    assertTrue(str.contains('test statistic'), 'Should contain test statistic')
    assertTrue(str.contains('degrees of freedom'), 'Should contain df')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
  }

  @Test
  void testLargerTable() {
    // 4×3 table
    def table = Matrix.builder()
      .matrixName('test')
      .rows([
        [10, 15, 20],
        [12, 18, 22],
        [14, 16, 24],
        [8, 12, 18]
      ])
      .build()

    def result = ChiSquared.pearsonTest(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals(6, result.degreesOfFreedom, 'df for 4×3 table should be 6')
    assertTrue(result.testStatistic >= 0, 'Test statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
  }

  @Test
  void testAllTestTypes() {
    // Test all three methods on the same table
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[15, 25], [35, 45]])
      .build()

    def pearson = ChiSquared.pearsonTest(table)
    def gtest = ChiSquared.gTest(table)
    def yates = ChiSquared.yatesTest(table)

    assertNotNull(pearson, 'Pearson result should not be null')
    assertNotNull(gtest, 'G-test result should not be null')
    assertNotNull(yates, 'Yates result should not be null')

    assertEquals('Pearson', pearson.testType)
    assertEquals('G-test', gtest.testType)
    assertEquals('Yates', yates.testType)

    // All should have same df for 2×2 table
    assertEquals(1, pearson.degreesOfFreedom)
    assertEquals(1, gtest.degreesOfFreedom)
    assertEquals(1, yates.degreesOfFreedom)
  }

  @Test
  void testSymmetricTable() {
    // Perfectly symmetric table
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[25, 25], [25, 25]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals(0.0, result.testStatistic, 0.001, 'Test statistic should be 0 for symmetric table')
    assertEquals(1.0, result.pValue, 0.001, 'p-value should be 1.0 for perfect independence')
  }

  @Test
  void testEqualProportions() {
    // Different totals but same proportions (independence)
    def table = Matrix.builder()
      .matrixName('test')
      .rows([[20, 30], [40, 60]])
      .build()

    def result = ChiSquared.pearsonTest(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.testStatistic < 0.001, 'Test statistic should be near 0')
    assertTrue(result.pValue > 0.99, 'p-value should be very high')
  }

  @Test
  void testDegreesOfFreedom() {
    // Test df calculation for various table sizes
    def table2x2 = Matrix.builder().matrixName('test').rows([[10, 20], [30, 40]]).build()
    def table3x3 = Matrix.builder().matrixName('test').rows([[10, 15, 20], [12, 18, 25], [8, 10, 15]]).build()
    def table2x3 = Matrix.builder().matrixName('test').rows([[10, 15, 20], [12, 18, 25]]).build()

    assertEquals(1, ChiSquared.pearsonTest(table2x2).degreesOfFreedom, '2×2 should have df=1')
    assertEquals(4, ChiSquared.pearsonTest(table3x3).degreesOfFreedom, '3×3 should have df=4')
    assertEquals(2, ChiSquared.pearsonTest(table2x3).degreesOfFreedom, '2×3 should have df=2')
  }
}
