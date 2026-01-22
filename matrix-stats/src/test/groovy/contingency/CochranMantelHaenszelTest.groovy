package contingency

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.contingency.CochranMantelHaenszel

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Cochran-Mantel-Haenszel test.
 */
class CochranMantelHaenszelTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testSingleStratum() {
    // Single stratum (equivalent to Fisher's exact or chi-squared test)
    int[][] stratum1 = [[12, 8], [6, 14]]

    List<int[][]> strata = [stratum1]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Result should not be null')
    assertEquals(1, result.strata, 'Should have 1 stratum')
    assertTrue(result.statistic >= 0, 'Chi-squared statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')
    assertTrue(result.commonOddsRatio > 0, 'Odds ratio should be positive')
  }

  @Test
  void testTwoStrata_CommonAssociation() {
    // Two strata with similar positive association
    int[][] stratum1 = [[20, 10], [10, 20]]  // Positive association
    int[][] stratum2 = [[30, 15], [15, 30]]  // Positive association

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Result should not be null')
    assertEquals(2, result.strata, 'Should have 2 strata')

    // Should detect significant common association
    assertTrue(result.commonOddsRatio > 1, 'Odds ratio should indicate positive association')
    assertTrue(result.pValue < 0.05, 'Should be significant at 5% level')
  }

  @Test
  void testNoAssociation() {
    // Multiple strata with no association (odds ratio ≈ 1)
    int[][] stratum1 = [[20, 20], [20, 20]]  // OR = 1
    int[][] stratum2 = [[30, 30], [30, 30]]  // OR = 1

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Result should not be null')

    // Should not detect significant association
    assertEquals(1.0, result.commonOddsRatio, 0.01, 'Odds ratio should be close to 1')
    assertTrue(result.pValue > 0.05, 'Should not be significant')
  }

  @Test
  void testNegativeAssociation() {
    // Strata with negative association (protective effect)
    int[][] stratum1 = [[10, 20], [20, 10]]  // OR < 1
    int[][] stratum2 = [[15, 30], [30, 15]]  // OR < 1

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Result should not be null')

    // Should detect negative association
    assertTrue(result.commonOddsRatio < 1, 'Odds ratio should indicate negative association')
    assertTrue(result.pValue < 0.05, 'Should be significant')
  }

  @Test
  void testMultipleStrata() {
    // Three strata with common association
    int[][] stratum1 = [[15, 10], [8, 12]]
    int[][] stratum2 = [[18, 12], [10, 15]]
    int[][] stratum3 = [[22, 15], [12, 18]]

    List<int[][]> strata = [stratum1, stratum2, stratum3]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Result should not be null')
    assertEquals(3, result.strata, 'Should have 3 strata')
    assertTrue(result.commonOddsRatio > 1, 'Should show positive association')
  }

  @Test
  void testContinuityCorrection() {
    int[][] stratum1 = [[12, 8], [6, 14]]

    List<int[][]> strata = [stratum1]

    // With continuity correction (default)
    def resultWith = CochranMantelHaenszel.test(strata, true)

    // Without continuity correction
    def resultWithout = CochranMantelHaenszel.test(strata, false)

    assertNotNull(resultWith, 'Result with correction should not be null')
    assertNotNull(resultWithout, 'Result without correction should not be null')

    assertTrue(resultWith.continuityCorrection, 'Should indicate correction was applied')
    assertFalse(resultWithout.continuityCorrection, 'Should indicate no correction')

    // Statistic without correction should be larger or equal
    assertTrue(resultWithout.statistic >= resultWith.statistic,
               'Statistic without correction should be >= with correction')
  }

  @Test
  void testValidation() {
    int[][] validStratum = [[12, 8], [6, 14]]

    // Null strata list
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test(null)
    }

    // Empty strata list
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test([])
    }

    // Null stratum
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test([null])
    }

    // Wrong dimensions (3×2 instead of 2×2)
    int[][] wrong3x2 = [[1, 2], [3, 4], [5, 6]]
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test([wrong3x2])
    }

    // Wrong dimensions (2×3 instead of 2×2)
    int[][] wrong2x3 = [[1, 2, 3], [4, 5, 6]]
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test([wrong2x3])
    }

    // Null row
    int[][] nullRow = [null, [6, 14]]
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test([nullRow])
    }

    // Negative value
    int[][] negative = [[12, -8], [6, 14]]
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test([negative])
    }

    // All zeros in one stratum
    int[][] allZeros = [[0, 0], [0, 0]]
    assertThrows(IllegalArgumentException) {
      CochranMantelHaenszel.test([allZeros])
    }
  }

  @Test
  void testResultToString() {
    int[][] stratum1 = [[12, 8], [6, 14]]

    List<int[][]> strata = [stratum1]
    def result = CochranMantelHaenszel.test(strata)

    String str = result.toString()
    assertTrue(str.contains('Cochran-Mantel-Haenszel'), 'Should contain test name')
    assertTrue(str.contains('Strata'), 'Should contain number of strata')
    assertTrue(str.contains('χ²'), 'Should contain chi-squared symbol')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
    assertTrue(str.contains('odds ratio'), 'Should contain odds ratio')
  }

  @Test
  void testInterpret() {
    int[][] stratum1 = [[20, 10], [10, 20]]
    int[][] stratum2 = [[30, 15], [15, 30]]

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention null hypothesis')
    assertTrue(interpretation.contains('association'), 'Should mention association')
  }

  @Test
  void testEvaluate() {
    int[][] stratum1 = [[20, 10], [10, 20]]
    int[][] stratum2 = [[30, 15], [15, 30]]

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('χ² statistic'), 'Should contain statistic info')
    assertTrue(evaluation.contains('Common odds ratio'), 'Should contain odds ratio')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
    assertTrue(evaluation.contains('positive association'), 'Should identify association direction')
  }

  @Test
  void testZeroMargins() {
    // Edge case: zero margin in one cell (but not all zeros)
    int[][] stratum1 = [[0, 10], [10, 20]]
    int[][] stratum2 = [[5, 15], [15, 25]]

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Should handle zero margins')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testOddsRatioCalculation() {
    // Known case: all strata have same structure [[a, b], [c, d]]
    // Odds ratio = (a*d)/(b*c) for each stratum
    int[][] stratum1 = [[10, 5], [5, 10]]  // OR = (10*10)/(5*5) = 4
    int[][] stratum2 = [[20, 10], [10, 20]]  // OR = (20*20)/(10*10) = 4

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    // Common odds ratio should be close to 4
    assertEquals(4.0, result.commonOddsRatio, 0.5, 'Common OR should be approximately 4')
  }

  @Test
  void testPValueRange() {
    // Test multiple configurations to ensure p-values are always valid
    List<List<int[][]>> testCases = [
      [[[20, 10], [10, 20]]],  // Single stratum, positive association
      [[[10, 20], [20, 10]]],  // Single stratum, negative association
      [[[20, 20], [20, 20]]],  // Single stratum, no association
      [[[15, 10], [8, 12]], [[18, 12], [10, 15]]],  // Two strata
      [[[5, 5], [5, 5]], [[10, 10], [10, 10]], [[15, 15], [15, 15]]]  // Three strata, no association
    ]

    for (List<int[][]> testCase : testCases) {
      def result = CochranMantelHaenszel.test(testCase)

      assertTrue(result.pValue >= 0, "p-value should be >= 0, got ${result.pValue}")
      assertTrue(result.pValue <= 1, "p-value should be <= 1, got ${result.pValue}")
    }
  }

  @Test
  void testLargeNumbers() {
    // Test with larger counts - varied structures
    int[][] stratum1 = [[150, 50], [40, 160]]
    int[][] stratum2 = [[200, 80], [70, 210]]

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Should handle large numbers')
    assertTrue(result.commonOddsRatio > 0, 'Odds ratio should be positive')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testSymmetricTables() {
    // Tables with perfect symmetry should have OR ≈ 1
    int[][] stratum1 = [[25, 25], [25, 25]]
    int[][] stratum2 = [[30, 30], [30, 30]]

    List<int[][]> strata = [stratum1, stratum2]
    def result = CochranMantelHaenszel.test(strata)

    assertEquals(1.0, result.commonOddsRatio, 0.01, 'Symmetric tables should have OR ≈ 1')
    assertTrue(result.pValue > 0.9, 'Should have very high p-value for no association')
  }

  @Test
  void testMixedSizes() {
    // Strata with different sample sizes
    int[][] small = [[5, 3], [3, 5]]
    int[][] medium = [[15, 10], [10, 15]]
    int[][] large = [[30, 20], [20, 30]]

    List<int[][]> strata = [small, medium, large]
    def result = CochranMantelHaenszel.test(strata)

    assertNotNull(result, 'Should handle strata of different sizes')
    assertEquals(3, result.strata, 'Should have 3 strata')
  }

  @Test
  void testOddsRatioDirection() {
    // Positive association
    int[][] pos1 = [[20, 10], [10, 20]]
    List<int[][]> posStrata = [pos1]
    def posResult = CochranMantelHaenszel.test(posStrata)

    String posEval = posResult.evaluate()
    assertTrue(posEval.contains('positive association'), 'Should identify positive association')

    // Negative association
    int[][] neg1 = [[10, 20], [20, 10]]
    List<int[][]> negStrata = [neg1]
    def negResult = CochranMantelHaenszel.test(negStrata)

    String negEval = negResult.evaluate()
    assertTrue(negEval.contains('negative association'), 'Should identify negative association')
  }
}
