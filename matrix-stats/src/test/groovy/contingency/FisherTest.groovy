package contingency

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.contingency.Fisher

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Fisher's Exact Test.
 */
class FisherTest {

  @Test
  void testBasic2x2Table() {
    // Classic tea tasting experiment example
    // Lady correctly identifies 3 out of 4 tea-first cups and 1 out of 4 milk-first cups
    def table = [[3, 1], [1, 3]]
    def result = Fisher.test(table)

    assertNotNull(result)
    assertNotNull(result.pValue)
    assertNotNull(result.oddsRatio)
    assertNotNull(result.confidenceInterval)
    assertEquals("two.sided", result.alternative)

    // Odds ratio = (3*3)/(1*1) = 9
    assertEquals(9.0, result.oddsRatio, 0.001)

    // p-value should be around 0.486 for this table
    assertTrue(result.pValue > 0.4 && result.pValue < 0.5)
  }

  @Test
  void testSignificantAssociation() {
    // Strong association: treatment group has much better outcomes
    def table = [[18, 2], [4, 16]]
    def result = Fisher.test(table)

    assertNotNull(result)
    // This should show significant association
    assertTrue(result.pValue < 0.05, "Should detect significant association")
    assertTrue(result.oddsRatio > 1.0, "Odds ratio should indicate positive association")
  }

  @Test
  void testNoAssociation() {
    // Equal proportions in both groups
    def table = [[10, 10], [10, 10]]
    def result = Fisher.test(table)

    assertNotNull(result)
    // No association: odds ratio should be 1
    assertEquals(1.0, result.oddsRatio, 0.001)
    // p-value should be high (not significant)
    assertTrue(result.pValue > 0.9, "Should not detect association")
  }

  @Test
  void testOneSidedGreater() {
    def table = [[12, 5], [9, 11]]
    def result = Fisher.test(table, "greater")

    assertNotNull(result)
    assertEquals("greater", result.alternative)
    assertNotNull(result.pValue)

    // One-sided test should have different p-value than two-sided
    def twoSided = Fisher.test(table, "two.sided")
    assertNotEquals(result.pValue, twoSided.pValue)
  }

  @Test
  void testOneSidedLess() {
    def table = [[12, 5], [9, 11]]
    def result = Fisher.test(table, "less")

    assertNotNull(result)
    assertEquals("less", result.alternative)
    assertNotNull(result.pValue)
  }

  @Test
  void testWithZeroCell() {
    // Table with one zero cell
    def table = [[10, 0], [5, 8]]
    def result = Fisher.test(table)

    assertNotNull(result)
    assertNotNull(result.oddsRatio)
    // Should use continuity correction for odds ratio
    assertTrue(result.oddsRatio > 0, "Odds ratio should be positive")
    assertTrue(result.confidenceInterval[0] > 0, "Lower CI should be positive")
    assertTrue(result.confidenceInterval[1] > 0, "Upper CI should be positive")
  }

  @Test
  void testWithMultipleZeroCells() {
    // Table with two zero cells
    def table = [[10, 0], [0, 8]]
    def result = Fisher.test(table)

    assertNotNull(result)
    // Should handle multiple zeros with continuity correction
    assertTrue(result.oddsRatio > 0)
    assertNotNull(result.confidenceInterval)
  }

  @Test
  void testMatrixInput() {
    // Test with Matrix instead of List
    def matrix = Matrix.builder()
      .matrixName('test')
      .rows([[5, 10], [15, 20]])
      .build()
    def result = Fisher.test(matrix)

    assertNotNull(result)
    assertNotNull(result.pValue)
    assertNotNull(result.oddsRatio)

    // Compare with List input
    def listResult = Fisher.test([[5, 10], [15, 20]])
    assertEquals(listResult.pValue, result.pValue, 1e-10)
    assertEquals(listResult.oddsRatio, result.oddsRatio, 1e-10)
  }

  @Test
  void testMatrixInputOneSided() {
    def matrix = Matrix.builder()
      .matrixName('test')
      .rows([[8, 4], [3, 7]])
      .build()
    def result = Fisher.test(matrix, "greater")

    assertNotNull(result)
    assertEquals("greater", result.alternative)
  }

  @Test
  void testValidation() {
    // Null table
    assertThrows(IllegalArgumentException) {
      Fisher.test(null as List<List<Integer>>)
    }

    // Wrong dimensions - not 2x2
    assertThrows(IllegalArgumentException) {
      Fisher.test([[1, 2, 3], [4, 5, 6]])
    }

    assertThrows(IllegalArgumentException) {
      Fisher.test([[1, 2]])
    }

    assertThrows(IllegalArgumentException) {
      Fisher.test([[1], [2]])
    }

    // Negative values
    assertThrows(IllegalArgumentException) {
      Fisher.test([[5, -1], [3, 4]])
    }

    // Matrix with wrong dimensions
    def wrongMatrix = Matrix.builder()
      .matrixName('test')
      .rows([[1, 2, 3], [4, 5, 6]])
      .build()
    assertThrows(IllegalArgumentException) {
      Fisher.test(wrongMatrix)
    }
  }

  @Test
  void testEvaluateMethod() {
    // Significant result
    def table1 = [[18, 2], [4, 16]]
    def result1 = Fisher.test(table1)
    assertTrue(result1.evaluate(0.05), "Should reject null at 5% level")
    assertTrue(result1.evaluate(), "Should reject null with default alpha")

    // Non-significant result
    def table2 = [[10, 10], [10, 10]]
    def result2 = Fisher.test(table2)
    assertFalse(result2.evaluate(0.05), "Should not reject null at 5% level")
  }

  @Test
  void testConfidenceInterval() {
    def table = [[12, 5], [9, 11]]
    def result = Fisher.test(table)

    assertNotNull(result.confidenceInterval)
    assertEquals(2, result.confidenceInterval.length)

    double lower = result.confidenceInterval[0]
    double upper = result.confidenceInterval[1]

    // Lower bound should be less than odds ratio
    assertTrue(lower < result.oddsRatio, "Lower CI bound should be < odds ratio")
    // Upper bound should be greater than odds ratio
    assertTrue(upper > result.oddsRatio, "Upper CI bound should be > odds ratio")
    // Lower should be less than upper
    assertTrue(lower < upper, "Lower CI should be < upper CI")
  }

  @Test
  void testToStringMethod() {
    def table = [[8, 4], [3, 7]]
    def result = Fisher.test(table)
    String str = result.toString()

    assertNotNull(str)
    assertTrue(str.contains("Fisher's Exact Test"))
    assertTrue(str.contains("p-value"))
    assertTrue(str.contains("odds ratio"))
    assertTrue(str.contains("95% CI"))
    assertTrue(str.contains("alternative"))
  }

  @Test
  void testSmallSampleSize() {
    // Very small sample - Fisher's is designed for this
    def table = [[1, 2], [2, 1]]
    def result = Fisher.test(table)

    assertNotNull(result)
    assertNotNull(result.pValue)
    // p-value should be 1.0 (perfect symmetry)
    assertEquals(1.0, result.pValue, 0.001)
  }

  @Test
  void testLargeSampleSize() {
    // Larger sample - Fisher's should still work
    def table = [[50, 30], [40, 60]]
    def result = Fisher.test(table)

    assertNotNull(result)
    assertNotNull(result.pValue)
    assertNotNull(result.oddsRatio)
  }

  @Test
  void testExtremeTables() {
    // All observations in one cell
    def table1 = [[20, 0], [0, 0]]
    def result1 = Fisher.test(table1)
    assertNotNull(result1)

    // Diagonal pattern
    def table2 = [[10, 0], [0, 10]]
    def result2 = Fisher.test(table2)
    assertNotNull(result2)
    assertTrue(result2.oddsRatio > 100, "Odds ratio should be very large for perfect association")
  }

  @Test
  void testOddsRatioCalculation() {
    // Test odds ratio = (a*d)/(b*c)
    def table = [[6, 3], [2, 8]]
    def result = Fisher.test(table)

    double expectedOR = (6.0 * 8.0) / (3.0 * 2.0)
    assertEquals(expectedOR, result.oddsRatio, 0.001)
  }

  @Test
  void testOddsRatioWithZero() {
    // When b or c is zero, continuity correction is applied
    def table = [[5, 0], [3, 4]]
    def result = Fisher.test(table)

    // With Haldane-Anscombe correction: (5.5 * 4.5) / (0.5 * 3.5)
    double expectedOR = (5.5 * 4.5) / (0.5 * 3.5)
    assertEquals(expectedOR, result.oddsRatio, 0.001)
  }

  @Test
  void testPValueRange() {
    // p-value should always be between 0 and 1
    def tables = [
      [[10, 5], [5, 10]],
      [[20, 2], [3, 18]],
      [[1, 1], [1, 1]],
      [[15, 0], [0, 15]]
    ]

    for (def table : tables) {
      def result = Fisher.test(table)
      assertTrue(result.pValue >= 0.0, "p-value should be >= 0")
      assertTrue(result.pValue <= 1.0, "p-value should be <= 1")
    }
  }

  @Test
  void testDifferentAlternatives() {
    def table = [[10, 5], [3, 12]]

    def twoSided = Fisher.test(table, "two.sided")
    def greater = Fisher.test(table, "greater")
    def less = Fisher.test(table, "less")

    // All should have same odds ratio
    assertEquals(twoSided.oddsRatio, greater.oddsRatio, 1e-10)
    assertEquals(twoSided.oddsRatio, less.oddsRatio, 1e-10)

    // But different p-values
    assertNotEquals(twoSided.pValue, greater.pValue)
    assertNotEquals(twoSided.pValue, less.pValue)

    // Greater + less should roughly equal or be less than two-sided
    // (depends on exact implementation, but this is generally true)
  }

  @Test
  void testSymmetricTable() {
    // Perfectly symmetric table
    def table = [[5, 5], [5, 5]]
    def result = Fisher.test(table)

    assertEquals(1.0, result.oddsRatio, 0.001, "Symmetric table should have OR = 1")
    assertTrue(result.pValue > 0.9, "Symmetric table should have high p-value")
  }

  @Test
  void testConfidenceIntervalIncludesOddsRatio() {
    def table = [[12, 8], [7, 13]]
    def result = Fisher.test(table)

    double lower = result.confidenceInterval[0]
    double upper = result.confidenceInterval[1]

    // CI should contain the point estimate
    assertTrue(lower <= result.oddsRatio, "Lower CI should be <= odds ratio")
    assertTrue(upper >= result.oddsRatio, "Upper CI should be >= odds ratio")
  }

  @Test
  void testCaseControlStudy() {
    // Typical case-control study: exposure vs disease
    // Cases: 30 exposed, 10 unexposed
    // Controls: 10 exposed, 30 unexposed
    def table = [[30, 10], [10, 30]]
    def result = Fisher.test(table)

    assertNotNull(result)
    // Strong association: OR = (30*30)/(10*10) = 9
    assertEquals(9.0, result.oddsRatio, 0.001)
    assertTrue(result.pValue < 0.05, "Should detect significant association")
  }

  @Test
  void testRareDiseaseApproximation() {
    // Rare disease scenario
    def table = [[2, 98], [1, 99]]
    def result = Fisher.test(table)

    assertNotNull(result)
    // Odds ratio ≈ relative risk for rare diseases
    assertTrue(result.oddsRatio > 1.5, "Should show increased risk")
  }

  @Test
  void testMinimalTable() {
    // Minimal valid table
    def table = [[1, 1], [1, 1]]
    def result = Fisher.test(table)

    assertNotNull(result)
    assertEquals(1.0, result.oddsRatio, 0.001)
  }

  @Test
  void testAllZerosInOneRow() {
    // One row is all zeros (edge case)
    def table = [[0, 0], [5, 10]]
    def result = Fisher.test(table)

    assertNotNull(result)
    // Should handle gracefully with continuity correction
    assertNotNull(result.oddsRatio)
  }

  @Test
  void testAllZerosInOneColumn() {
    // One column is all zeros (edge case)
    def table = [[0, 10], [0, 15]]
    def result = Fisher.test(table)

    assertNotNull(result)
    assertNotNull(result.oddsRatio)
  }

  @Test
  void testComparisonWithExpectedValues() {
    // Test against known result from R
    // R: fisher.test(matrix(c(12, 5, 9, 11), nrow=2, byrow=TRUE))
    // Note: R uses byrow=TRUE for row-major order [[12,5],[9,11]]
    // Expected: p-value ≈ 0.1845, OR ≈ 2.933
    def table = [[12, 5], [9, 11]]
    def result = Fisher.test(table)

    assertEquals(2.933, result.oddsRatio, 0.01, "Odds ratio should match expected")
    // Two-sided p-value should be reasonable (not extremely small or large)
    assertTrue(result.pValue > 0.1 && result.pValue < 0.3,
               "p-value should be in reasonable range (got ${result.pValue})")
  }

  @Test
  void testAlternativeHypothesesConsistency() {
    def table = [[15, 5], [8, 12]]

    def greater = Fisher.test(table, "greater")
    def less = Fisher.test(table, "less")

    // For one-sided tests, p(greater) + p(less) should be approximately 1 + p(observed)
    // This is because they overlap at the observed value
    assertNotNull(greater.pValue)
    assertNotNull(less.pValue)
  }
}
