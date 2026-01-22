package ttest

import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.stats.ttest.Welch
import se.alipsa.matrix.stats.ttest.TtestResult

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Welch's t-test implementation.
 */
class WelchTest {

  @Test
  void testWelchTwoSample() {
    // Same test as StudentTest but using Welch class with simpler API
    def iris = Dataset.iris()
    def speciesIdx = iris.columnIndex("Species")
    def setosa = iris.subset {
      it[speciesIdx] == 'setosa'
    }
    def virginica = iris.subset {
      it[speciesIdx] == 'virginica'
    }

    // Welch.tTest() always uses Welch's t-test (no equalVariance parameter)
    def result = Welch.tTest(setosa['Petal Length'], virginica['Petal Length'])

    // Verify same results as Student.tTest with equalVariance=false
    assertEquals(-49.98618626, result.getT(8), "t value")
    assertEquals(58.60939455, result.getDf(8), "Degrees of freedom")
    assertEquals(0.17366400, result.getSd1(8), "sd1")
    assertEquals(0.55189470, result.getSd2(8), "sd2")
    assertEquals(9.269628E-50, result.p, 0.0000001)
    assertEquals("Welch's two sample t-test", result.description)
  }

  @Test
  void testWelchWithDifferentSampleSizes() {
    def sample1 = [10, 12, 11, 13, 9]
    def sample2 = [14, 15, 13, 16, 14, 15, 16]

    def result = Welch.tTest(sample1, sample2)

    // Should handle different sample sizes correctly
    assertEquals(5, result.n1)
    assertEquals(7, result.n2)
    assertTrue(result.pVal < 0.05, "Should detect difference")
    assertTrue(result.df > 0, "df should be positive")
    // Welch-Satterthwaite df should be fractional
    assertNotEquals(result.df as int, result.df, "df should be fractional for Welch's test")
  }

  @Test
  void testWelchInputValidation() {
    def sample = [10, 12, 11]

    // Null inputs
    assertThrows(IllegalArgumentException) {
      Welch.tTest(null, sample)
    }

    assertThrows(IllegalArgumentException) {
      Welch.tTest(sample, null)
    }

    // Empty inputs
    assertThrows(IllegalArgumentException) {
      Welch.tTest([], sample)
    }

    // Insufficient observations
    assertThrows(IllegalArgumentException) {
      Welch.tTest([10], sample)
    }

    assertThrows(IllegalArgumentException) {
      Welch.tTest(sample, [10])
    }
  }

  @Test
  void testWelchVsStudentWithEqualVariance() {
    // When variances are very similar, Welch and Student should give similar results
    def sample1 = [10.0, 10.2, 9.8, 10.1, 9.9] // var ≈ 0.025
    def sample2 = [12.0, 12.2, 11.8, 12.1, 11.9] // var ≈ 0.025

    def welchResult = Welch.tTest(sample1, sample2)

    // With equal variance and equal sample sizes, results should be very close
    assertTrue(welchResult.pVal < 0.001, "Should detect clear difference")
    assertTrue(Math.abs(welchResult.t) > 10, "t-statistic should be large")
  }

  @Test
  void testWelchDescriptionIsCorrect() {
    def sample1 = [1, 2, 3, 4]
    def sample2 = [5, 6, 7, 8]

    def result = Welch.tTest(sample1, sample2)

    assertEquals("Welch's two sample t-test", result.description)
  }

  @Test
  void testWelchReturnsTtestResult() {
    def sample1 = [1, 2, 3, 4, 5]
    def sample2 = [6, 7, 8, 9, 10]

    def result = Welch.tTest(sample1, sample2)

    // Verify result is TtestResult type
    assertTrue(result instanceof TtestResult, "Welch.tTest() should return TtestResult")

    // Verify all expected properties are accessible
    assertNotNull(result.tVal, "tVal should be set")
    assertNotNull(result.pVal, "pVal should be set")
    assertNotNull(result.df, "df should be set")
    assertNotNull(result.mean1, "mean1 should be set")
    assertNotNull(result.mean2, "mean2 should be set")
    assertNotNull(result.description, "description should be set")
  }
}
