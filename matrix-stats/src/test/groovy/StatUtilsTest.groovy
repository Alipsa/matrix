import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.StatUtils

class StatUtilsTest {

  @Test
  void testMean() {
    double result = StatUtils.mean([1.0d, 2.0d, 3.0d, 4.0d] as double[])
    assertEquals(2.5d, result, 1e-10d)
  }

  @Test
  void testMeanEmptyThrows() {
    assertThrows(IllegalArgumentException) {
      StatUtils.mean([] as double[])
    }
  }

  @Test
  void testVariance() {
    double result = StatUtils.variance([1.0d, 2.0d, 3.0d, 4.0d] as double[])
    assertEquals(1.6666666666666667d, result, 1e-10d)
  }

  @Test
  void testVarianceWithMean() {
    double result = StatUtils.variance([1.0d, 2.0d, 3.0d, 4.0d] as double[], 2.5d)
    assertEquals(1.6666666666666667d, result, 1e-10d)
  }
}
