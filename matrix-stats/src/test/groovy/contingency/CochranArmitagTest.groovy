package contingency

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.contingency.CochranArmitage

/**
 * Tests for the Cochran-Armitage trend test.
 */
class CochranArmitagTest {

  @Test
  void testIncreasingTrend() {
    // Clear increasing trend: more cases with higher exposure
    List<Integer> cases = [10, 20, 30]
    List<Integer> controls = [90, 80, 70]

    def result = CochranArmitage.test(cases, controls)

    assertNotNull(result, 'Result should not be null')
    assertEquals(3, result.categories, 'Should have 3 categories')
    assertEquals(60, result.cases, 'Total cases')
    assertEquals(240, result.controls, 'Total controls')
    assertEquals(300, result.sampleSize, 'Total sample size')

    // Should detect significant increasing trend
    assertTrue(result.statistic > 0, 'Z-statistic should be positive for increasing trend')
    assertTrue(result.pValue < 0.05, 'Should be significant at 5% level')
  }

  @Test
  void testDecreasingTrend() {
    // Clear decreasing trend: fewer cases with higher exposure
    List<Integer> cases = [30, 20, 10]
    List<Integer> controls = [70, 80, 90]

    def result = CochranArmitage.test(cases, controls)

    assertNotNull(result, 'Result should not be null')

    // Should detect significant decreasing trend
    assertTrue(result.statistic < 0, 'Z-statistic should be negative for decreasing trend')
    assertTrue(result.pValue < 0.05, 'Should be significant at 5% level')
  }

  @Test
  void testNoTrend() {
    // No trend: similar proportions across categories
    List<Integer> cases = [20, 20, 20]
    List<Integer> controls = [80, 80, 80]

    def result = CochranArmitage.test(cases, controls)

    assertNotNull(result, 'Result should not be null')

    // Should not detect trend
    assertTrue(result.statistic.abs() < 0.5, 'Z-statistic should be close to 0 for no trend')
    assertTrue(result.pValue > 0.05, 'Should not be significant')
  }

  @Test
  void testCustomScores() {
    // Test with custom ordinal scores
    List<Integer> cases = [10, 15, 25]
    List<Integer> controls = [90, 85, 75]
    List<BigDecimal> scores = [1.0, 2.0, 3.0]  // Custom scores instead of default 0, 1, 2

    def result = CochranArmitage.test(cases, controls, scores)

    assertNotNull(result, 'Result should not be null')
    assertEquals(scores, result.scores, 'Should use provided scores')

    // Should still detect increasing trend
    assertTrue(result.statistic > 0, 'Should detect increasing trend with custom scores')
  }

  @Test
  void testListInput() {
    // Test with List inputs instead of arrays
    List<Integer> cases = [10, 20, 30]
    List<Integer> controls = [90, 80, 70]
    List<BigDecimal> scores = [0.0, 1.0, 2.0]

    def result = CochranArmitage.test(cases, controls, scores)

    assertNotNull(result, 'Result should not be null')
    assertEquals(3, result.categories, 'Should have 3 categories')
  }

  @Test
  void testTwoCategories() {
    // Minimum case: 2 categories
    List<Integer> cases = [10, 30]
    List<Integer> controls = [90, 70]

    def result = CochranArmitage.test(cases, controls)

    assertNotNull(result, 'Result should not be null')
    assertEquals(2, result.categories, 'Should have 2 categories')
    assertTrue(result.statistic > 0, 'Should detect increasing trend')
  }

  @Test
  void testManyCategories() {
    // Test with many categories (5)
    List<Integer> cases = [5, 10, 15, 20, 25]
    List<Integer> controls = [95, 90, 85, 80, 75]

    def result = CochranArmitage.test(cases, controls)

    assertNotNull(result, 'Result should not be null')
    assertEquals(5, result.categories, 'Should have 5 categories')
    assertTrue(result.statistic > 0, 'Should detect increasing trend')
  }

  @Test
  void testValidation() {
    List<Integer> validCases = [10, 20, 30]
    List<Integer> validControls = [90, 80, 70]

    // Null cases
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test(null, validControls)
    }

    // Null controls
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test(validCases, null)
    }

    // Mismatched lengths
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test([10, 20], validControls)
    }

    // Too few categories (need at least 2)
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test([10], [90])
    }

    // Negative cases
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test([-10, 20, 30], validControls)
    }

    // Negative controls
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test(validCases, [90, -80, 70])
    }

    // Wrong scores length
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test(validCases, validControls, [0.0, 1.0])
    }

    // Zero total (all zeros)
    assertThrows(IllegalArgumentException) {
      CochranArmitage.test([0, 0, 0], [0, 0, 0])
    }
  }

  @Test
  void testResultToString() {
    List<Integer> cases = [10, 20, 30]
    List<Integer> controls = [90, 80, 70]

    def result = CochranArmitage.test(cases, controls)

    String str = result
    assertTrue(str.contains('Cochran-Armitage'), 'Should contain test name')
    assertTrue(str.contains('Categories'), 'Should contain number of categories')
    assertTrue(str.contains('Z-statistic'), 'Should contain test statistic')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
  }

  @Test
  void testInterpret() {
    List<Integer> cases = [10, 20, 30]
    List<Integer> controls = [90, 80, 70]

    def result = CochranArmitage.test(cases, controls)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention null hypothesis')
    assertTrue(interpretation.contains('trend'), 'Should mention trend')
  }

  @Test
  void testEvaluate() {
    List<Integer> cases = [10, 20, 30]
    List<Integer> controls = [90, 80, 70]

    def result = CochranArmitage.test(cases, controls)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('Z-statistic'), 'Should contain statistic info')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
    assertTrue(evaluation.contains('direction'), 'Should mention direction of trend')
  }

  @Test
  void testZeroVariance() {
    // All same scores should cause zero variance
    List<Integer> cases = [10, 20, 30]
    List<Integer> controls = [90, 80, 70]
    List<BigDecimal> scores = [1.0, 1.0, 1.0]  // All same score

    assertThrows(IllegalArgumentException) {
      CochranArmitage.test(cases, controls, scores)
    }
  }

  @Test
  void testPValueRange() {
    // Test multiple scenarios to ensure p-values are always valid
    List<List<Integer>> testCases = [
      [10, 20, 30],
      [30, 20, 10],
      [20, 20, 20],
      [5, 15, 25],
      [1, 5, 10]
    ]
    List<List<Integer>> testControls = [
      [90, 80, 70],
      [70, 80, 90],
      [80, 80, 80],
      [95, 85, 75],
      [99, 95, 90]
    ]

    for (int i = 0; i < testCases.size(); i++) {
      def result = CochranArmitage.test(testCases[i], testControls[i])

      assertTrue(result.pValue >= 0, "p-value should be >= 0, got ${result.pValue}")
      assertTrue(result.pValue <= 1, "p-value should be <= 1, got ${result.pValue}")
    }
  }

  @Test
  void testSymmetry() {
    // Reversing the trend should give opposite Z-statistic but same p-value
    List<Integer> cases1 = [10, 20, 30]
    List<Integer> controls1 = [90, 80, 70]

    List<Integer> cases2 = [30, 20, 10]
    List<Integer> controls2 = [70, 80, 90]

    def result1 = CochranArmitage.test(cases1, controls1)
    def result2 = CochranArmitage.test(cases2, controls2)

    // Z-statistics should have opposite signs
    assertEquals(result1.statistic as double, (-result2.statistic) as double, 0.0001,
                 'Reversed trend should have opposite Z-statistic')

    // p-values should be approximately equal
    assertEquals(result1.pValue as double, result2.pValue as double, 0.0001,
                 'Reversed trend should have same p-value')
  }

  @Test
  void testLargeNumbers() {
    // Test with larger sample sizes
    List<Integer> cases = [100, 200, 300]
    List<Integer> controls = [900, 800, 700]

    def result = CochranArmitage.test(cases, controls)

    assertNotNull(result, 'Should handle large numbers')
    assertEquals(600, result.cases, 'Total cases')
    assertEquals(2400, result.controls, 'Total controls')
    assertTrue(result.pValue < 0.05, 'Should be significant with large n')
  }

  @Test
  void testSingleCategory_ZeroCounts() {
    // Edge case: one category has zero counts
    List<Integer> cases = [0, 20, 30]
    List<Integer> controls = [100, 80, 70]

    def result = CochranArmitage.test(cases, controls)

    assertNotNull(result, 'Should handle zero counts in one category')
    assertEquals(50, result.cases, 'Total cases')
  }

  @Test
  void testDirection() {
    List<Integer> increasingCases = [10, 20, 30]
    List<Integer> controls = [90, 80, 70]

    def result = CochranArmitage.test(increasingCases, controls)

    String eval = result.evaluate()
    assertTrue(eval.contains('increasing'), 'Should identify increasing trend')

    List<Integer> decreasingCases = [30, 20, 10]
    def result2 = CochranArmitage.test(decreasingCases, controls)

    String eval2 = result2.evaluate()
    assertTrue(eval2.contains('decreasing'), 'Should identify decreasing trend')
  }
}
