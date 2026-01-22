package contingency

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.contingency.Barnard

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Barnard's exact test.
 */
class BarnardTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testBasicAssociation() {
    // Simple case with clear association
    int[][] table = [[12, 8], [6, 14]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
    assertTrue(result.nuisanceParameter >= 0 && result.nuisanceParameter <= 1, 'π should be in [0, 1]')
    assertEquals(40, result.sampleSize, 'Sample size should be 40')
  }

  @Test
  void testStrongAssociation() {
    // Strong positive association
    int[][] table = [[20, 5], [5, 20]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.pValue < 0.05, 'Should detect strong association')
    assertTrue(Math.abs(result.statistic) > 1, 'Test statistic should be large for strong association')
  }

  @Test
  void testNoAssociation() {
    // No association: similar proportions
    int[][] table = [[10, 10], [10, 10]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.pValue > 0.05, 'Should not detect association')
    assertTrue(Math.abs(result.statistic) < 1, 'Test statistic should be small for no association')
  }

  @Test
  void testSymmetricTable() {
    // Perfectly symmetric table
    int[][] table = [[15, 15], [15, 15]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals(0.0, Math.abs(result.statistic), 0.01, 'Statistic should be zero for symmetric table')
    assertTrue(result.pValue > 0.9, 'p-value should be very high for no association')
  }

  @Test
  void testSmallSample() {
    // Very small sample where Barnard's test is appropriate
    int[][] table = [[3, 1], [1, 3]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals(8, result.sampleSize, 'Sample size should be 8')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testExtremeCounts() {
    // Extreme case: very unbalanced
    int[][] table = [[15, 1], [1, 15]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.pValue < 0.001, 'Should be highly significant for extreme association')
  }

  @Test
  void testZeroCell() {
    // Table with a zero cell
    int[][] table = [[10, 0], [5, 15]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testMultipleZeros() {
    // Table with multiple zeros
    int[][] table = [[10, 0], [0, 10]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.pValue < 0.01, 'Perfect separation should be significant')
  }

  @Test
  void testValidation() {
    int[][] validTable = [[10, 5], [5, 10]]

    // Null table
    assertThrows(IllegalArgumentException) {
      Barnard.test(null)
    }

    // Wrong dimensions (3×2)
    int[][] wrong3x2 = [[1, 2], [3, 4], [5, 6]]
    assertThrows(IllegalArgumentException) {
      Barnard.test(wrong3x2)
    }

    // Wrong dimensions (2×3)
    int[][] wrong2x3 = [[1, 2, 3], [4, 5, 6]]
    assertThrows(IllegalArgumentException) {
      Barnard.test(wrong2x3)
    }

    // Null row
    int[][] nullRow = [null, [5, 10]]
    assertThrows(IllegalArgumentException) {
      Barnard.test(nullRow)
    }

    // Negative value
    int[][] negative = [[10, -5], [5, 10]]
    assertThrows(IllegalArgumentException) {
      Barnard.test(negative)
    }

    // All zeros
    int[][] allZeros = [[0, 0], [0, 0]]
    assertThrows(IllegalArgumentException) {
      Barnard.test(allZeros)
    }
  }

  @Test
  void testResultToString() {
    int[][] table = [[12, 8], [6, 14]]

    def result = Barnard.test(table)

    String str = result.toString()
    assertTrue(str.contains('Barnard'), 'Should contain test name')
    assertTrue(str.contains('Wald score'), 'Should contain test statistic name')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
    assertTrue(str.contains('Nuisance parameter'), 'Should contain nuisance parameter')
  }

  @Test
  void testInterpret() {
    int[][] table = [[20, 5], [5, 20]]

    def result = Barnard.test(table)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention null hypothesis')
    assertTrue(interpretation.contains('association'), 'Should mention association')
  }

  @Test
  void testEvaluate() {
    int[][] table = [[20, 5], [5, 20]]

    def result = Barnard.test(table)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('Wald score statistic'), 'Should contain statistic info')
    assertTrue(evaluation.contains('Nuisance parameter'), 'Should contain nuisance parameter')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testPValueRange() {
    // Test multiple scenarios to ensure p-values are always valid
    int[][][] testCases = [
      [[10, 5], [5, 10]],
      [[15, 15], [15, 15]],
      [[20, 10], [10, 20]],
      [[5, 5], [5, 5]],
      [[1, 9], [9, 1]]
    ]

    for (int[][] testCase : testCases) {
      def result = Barnard.test(testCase)

      assertTrue(result.pValue >= 0, "p-value should be >= 0, got ${result.pValue}")
      assertTrue(result.pValue <= 1, "p-value should be <= 1, got ${result.pValue}")
      assertTrue(result.nuisanceParameter >= 0, "π should be >= 0, got ${result.nuisanceParameter}")
      assertTrue(result.nuisanceParameter <= 1, "π should be <= 1, got ${result.nuisanceParameter}")
    }
  }

  @Test
  void testDifferentAlphaLevels() {
    int[][] table = [[15, 5], [5, 15]]

    def result = Barnard.test(table)

    // Test interpret with different alpha levels
    String interp01 = result.interpret(0.01)
    String interp05 = result.interpret(0.05)
    String interp10 = result.interpret(0.10)

    assertNotNull(interp01, 'Interpretation at α=0.01 should not be null')
    assertNotNull(interp05, 'Interpretation at α=0.05 should not be null')
    assertNotNull(interp10, 'Interpretation at α=0.10 should not be null')
  }

  @Test
  void testLargerSample() {
    // Moderate sample size
    int[][] table = [[30, 20], [15, 35]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Result should not be null')
    assertEquals(100, result.sampleSize, 'Sample size should be 100')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testOneRowZero() {
    // One row has all zeros in one cell
    int[][] table = [[0, 20], [10, 10]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Should handle zero row margin')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testStatisticSign() {
    // Positive difference
    int[][] table1 = [[20, 10], [10, 20]]
    def result1 = Barnard.test(table1)

    // Negative difference (reversed)
    int[][] table2 = [[10, 20], [20, 10]]
    def result2 = Barnard.test(table2)

    // Statistics should have opposite signs
    assertTrue(result1.statistic * result2.statistic < 0,
               'Reversed tables should have opposite-sign statistics')

    // p-values should be similar (test is two-sided)
    assertEquals(result1.pValue, result2.pValue, 0.1,
                 'Reversed tables should have similar p-values')
  }

  @Test
  void testMinimalTable() {
    // Minimal non-zero table
    int[][] table = [[1, 1], [1, 1]]

    def result = Barnard.test(table)

    assertNotNull(result, 'Should handle minimal table')
    assertEquals(4, result.sampleSize, 'Sample size should be 4')
  }

  @Test
  void testNuisanceParameterValid() {
    // The nuisance parameter should be in valid range [0, 1]
    int[][] table = [[15, 10], [5, 10]]

    def result = Barnard.test(table)

    assertTrue(result.nuisanceParameter >= 0.0,
               'Nuisance parameter should be >= 0')
    assertTrue(result.nuisanceParameter <= 1.0,
               'Nuisance parameter should be <= 1')
  }
}
