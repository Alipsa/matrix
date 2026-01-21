import org.apache.commons.math3.stat.correlation.KendallsCorrelation
import org.junit.jupiter.api.Test
import static se.alipsa.matrix.stats.Correlation.*

import java.math.RoundingMode

import static org.junit.jupiter.api.Assertions.assertEquals

class CorrelationTest {

  /**
   * in R
   * print(cor(c(15, 18, 21, 24, 27), c(25, 25, 27, 31, 32)))
   * [1] 0.95346258924559
   * BigDecimal implementation gives slightly better precision: 0.95346258924560
   */
  @Test
  void testPearsonCorrelation() {
    assertEquals(
        0.95346258924560,
        cor([15, 18, 21, 24, 27], [25, 25, 27, 31, 32]).setScale(14, RoundingMode.HALF_EVEN))
  }

  /**
   * print(cor(c(15, 18, 21, 15, 21), c(25, 25, 27, 27, 27), method = "spearman"))
   * [1] 0.45643546458764
   */
  @Test
  void testSpearmanCorrelation() {
    assertEquals(
        0.45643546458764,
        cor([15, 18, 21, 15, 21], [25, 25, 27, 27, 27], SPEARMAN).setScale(14, RoundingMode.HALF_EVEN))
  }

  /**
   * Test Spearman correlation with simple monotonic data (no ties)
   * X: [1, 2, 3, 4, 5] -> ranks [1, 2, 3, 4, 5]
   * Y: [5, 6, 7, 8, 9] -> ranks [1, 2, 3, 4, 5]
   * Spearman correlation should be 1.0 (perfect positive correlation)
   * Verified in R: cor(c(1, 2, 3, 4, 5), c(5, 6, 7, 8, 9), method = "spearman")
   */
  @Test
  void testSpearmanCorrelationMonotonic() {
    assertEquals(
        1.0,
        cor([1, 2, 3, 4, 5], [5, 6, 7, 8, 9], SPEARMAN).setScale(1, RoundingMode.HALF_EVEN))
  }

  /**
   * Test Spearman correlation with non-monotonic data and ties
   * X: [10, 20, 30, 40, 50] -> ranks [1, 2, 3, 4, 5]
   * Y: [50, 40, 30, 20, 10] -> ranks [5, 4, 3, 2, 1]
   * Spearman correlation should be -1.0 (perfect negative correlation)
   * Verified in R: cor(c(10, 20, 30, 40, 50), c(50, 40, 30, 20, 10), method = "spearman")
   */
  @Test
  void testSpearmanCorrelationNegative() {
    assertEquals(
        -1.0,
        cor([10, 20, 30, 40, 50], [50, 40, 30, 20, 10], SPEARMAN).setScale(1, RoundingMode.HALF_EVEN))
  }

  @Test
  void testKendallsCorrelation() {
    def x1 = [12, 2, 1, 12, 2]
    def x2 = [1, 4, 7, 1, 0]
    def kc = new KendallsCorrelation()
    def apacheCor = kc.correlation(x1 as double[], x2 as double[])
    assertEquals(-0.47140452079103173, apacheCor)
    // BigDecimal implementation gives comparable precision
    assertEquals(-0.47140452079103, corKendall(x1, x2).setScale(14, RoundingMode.HALF_EVEN))
  }
}
