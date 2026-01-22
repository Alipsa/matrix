package ttest

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.ttest.Student
import se.alipsa.matrix.stats.ttest.TtestResult
import se.alipsa.matrix.stats.ttest.Welch

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Student's t-test with equal variance assumption.
 * Verifies the orthodox implementation of Student's t-test using pooled variance.
 */
class StudentEqualVarianceTest {

  /**
   * Test Student's t-test with equal variance using a classic textbook example.
   *
   * Example from R:
   * <code><pre>
   * group1 <- c(5, 7, 5, 3, 5, 3, 3, 9)
   * group2 <- c(8, 1, 4, 6, 6, 4, 1, 2)
   * t.test(group1, group2, var.equal = TRUE)
   *
   * Results:
   * t = 0.84732, df = 14, p-value = 0.4111
   * mean of x = 5.000, mean of y = 4.000
   * </pre></code>
   */
  @Test
  void testStudentTTestWithEqualVariance() {
    def group1 = [5, 7, 5, 3, 5, 3, 3, 9]
    def group2 = [8, 1, 4, 6, 6, 4, 1, 2]

    def result = Student.tTest(group1, group2, true)

    // Verify description indicates Student's t-test
    assertEquals("Student's two sample t-test", result.description)

    // Verify degrees of freedom (n1 + n2 - 2 = 8 + 8 - 2 = 14)
    assertEquals(14, result.df as int, "Degrees of freedom")

    // Verify t-statistic (from R reference)
    assertEquals(0.84731855, result.getT(8), "t-statistic")

    // Verify p-value (from R reference: 0.4111, allowing small tolerance for numerical precision)
    assertEquals(0.4111, result.pVal, 0.001, "p-value")

    // Verify means
    assertEquals(5.0, result.getMean1(1), "mean1")
    assertEquals(4.0, result.getMean2(1), "mean2")
  }

  /**
   * Test that pooled variance is calculated correctly.
   *
   * Manual calculation:
   * group1: n1=4, mean=10, var=5
   * group2: n2=4, mean=15, var=5
   *
   * Pooled variance: s_p^2 = [(n1-1)var1 + (n2-1)var2] / (n1+n2-2)
   *                        = [(3*5) + (3*5)] / 6
   *                        = 30 / 6 = 5
   *
   * SE = s_p * sqrt(1/n1 + 1/n2) = sqrt(5) * sqrt(1/4 + 1/4)
   *    = sqrt(5) * sqrt(0.5) = sqrt(2.5) = 1.58113883
   *
   * t = (mean1 - mean2) / SE = (10 - 15) / 1.58113883 = -3.16227766
   */
  @Test
  void testPooledVarianceCalculation() {
    def group1 = [8, 10, 12, 10]  // mean=10, var=2.666667 (sample)
    def group2 = [13, 15, 17, 15]  // mean=15, var=2.666667 (sample)

    def result = Student.tTest(group1, group2, true)

    // Verify it uses Student's t-test
    assertEquals("Student's two sample t-test", result.description)

    // Verify degrees of freedom
    assertEquals(6, result.df as int, "df should be n1+n2-2 = 4+4-2 = 6")

    // Both groups have equal variance (approximately 2.666667)
    assertEquals(2.66666667, result.getVar1(8), "var1")
    assertEquals(2.66666667, result.getVar2(8), "var2")

    // Verify t-statistic is negative (group1 mean < group2 mean)
    assertTrue(result.t < 0, "t should be negative")

    // With equal variances and equal sample sizes, the pooled variance
    // should equal the individual variances
    // t = (10-15) / sqrt(2.666667 * (1/4 + 1/4)) = -5 / sqrt(1.333333) = -4.33
    assertEquals(-4.33012702, result.getT(8), "t-statistic")
  }

  /**
   * Test Student's t-test vs Welch's t-test with equal variance.
   * When variances are truly equal, both tests should give similar results,
   * but Student's t-test will have integer degrees of freedom.
   */
  @Test
  void testStudentVsWelchWithEqualVariance() {
    def group1 = [10.0, 10.2, 9.8, 10.1, 9.9]
    def group2 = [12.0, 12.2, 11.8, 12.1, 11.9]

    def studentResult = Student.tTest(group1, group2, true)
    def welchResult = Welch.tTest(group1, group2)

    // Verify Student's test has integer df
    assertEquals(8, studentResult.df as int, "Student's df should be n1+n2-2 = 8")

    // Verify descriptions
    assertEquals("Student's two sample t-test", studentResult.description)
    assertEquals("Welch's two sample t-test", welchResult.description)

    // With truly equal variances, t-statistics should be very close
    assertEquals(studentResult.t, welchResult.t, 0.01, "t-statistics should be similar")

    // But degrees of freedom will differ
    assertTrue(Math.abs(studentResult.df - welchResult.df) < 0.5,
               "With equal variances, df should be close but Welch's may be fractional")
  }

  /**
   * Test auto-detection of equal variance (null parameter).
   * Should use rule of thumb: |var1 - var2| < 4
   */
  @Test
  void testAutoDetectEqualVariance() {
    // Very similar variances - should auto-detect as equal
    def group1 = [10, 11, 12, 13, 14]
    def group2 = [10, 11, 12, 13, 14]

    def result = Student.tTest(group1, group2, null)

    // Should detect equal variance and use Student's t-test
    assertTrue(result.description.contains("equal variance") ||
               result.df == (group1.size() + group2.size() - 2),
               "Should auto-detect equal variance")
  }

  /**
   * Test that Student's t-test with equal variance handles
   * different sample sizes correctly.
   */
  @Test
  void testUnequalSampleSizes() {
    def group1 = [5, 6, 7]
    def group2 = [8, 9, 10, 11, 12]

    def result = Student.tTest(group1, group2, true)

    // Verify df = n1 + n2 - 2 = 3 + 5 - 2 = 6
    assertEquals(6, result.df as int, "df should be n1+n2-2 = 6")
    assertEquals(3, result.n1, "n1")
    assertEquals(5, result.n2, "n2")
    assertEquals("Student's two sample t-test", result.description)
  }

  /**
   * Test that Student.Result extends TtestResult for backwards compatibility.
   */
  @Test
  void testStudentResultExtendsTtestResult() {
    def group1 = [1, 2, 3, 4, 5]
    def group2 = [6, 7, 8, 9, 10]

    def result = Student.tTest(group1, group2, true)

    // Verify result is both Student.Result and TtestResult
    assertTrue(result instanceof Student.Result, "Should be Student.Result")
    assertTrue(result instanceof TtestResult, "Should also be TtestResult (via inheritance)")

    // Verify backwards compatibility - Student.Result can be assigned to TtestResult
    TtestResult ttestResult = result
    assertNotNull(ttestResult, "Should be assignable to TtestResult")
    assertEquals(result.tVal, ttestResult.tVal, "tVal should match")
  }

  /**
   * Test that Student.tTest throws exception when equalVariance=false.
   * User should use Welch.tTest() instead for unequal variances.
   */
  @Test
  void testStudentRejectsUnequalVariance() {
    def group1 = [1, 2, 3, 4, 5]
    def group2 = [6, 7, 8, 9, 10]

    def exception = assertThrows(IllegalArgumentException) {
      Student.tTest(group1, group2, false)
    }

    assertTrue(exception.message.contains("Welch.tTest()"),
               "Exception should direct user to Welch.tTest()")
    assertTrue(exception.message.contains("equal variances"),
               "Exception should mention equal variance requirement")
  }

  /**
   * Test that Student.tTest throws exception when auto-detect determines unequal variance.
   */
  @Test
  void testStudentRejectsAutoDetectedUnequalVariance() {
    // Create samples with very different variances (> 4)
    def group1 = [1, 1, 1, 1, 1]      // var ≈ 0
    def group2 = [1, 10, 20, 30, 40]  // var ≈ 247

    def exception = assertThrows(IllegalArgumentException) {
      Student.tTest(group1, group2, null)
    }

    assertTrue(exception.message.contains("Welch.tTest()"),
               "Exception should direct user to Welch.tTest()")
    assertTrue(exception.message.contains("differ significantly"),
               "Exception should mention variances differ")
  }
}
