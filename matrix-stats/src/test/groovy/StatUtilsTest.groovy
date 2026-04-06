import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.StatUtils

class StatUtilsTest {

  @Test
  void testMeanAcceptsListInput() {
    assertEquals(2.5d, StatUtils.mean([1.0G, 2.0G, 3.0G, 4.0G]), 1e-10d)
  }

  @Test
  void testVarianceAcceptsListAndNumberMean() {
    List<BigDecimal> values = [1.0, 2.0, 3.0, 4.0]

    assertEquals(1.6666666666666667d, StatUtils.variance(values), 1e-10d)
    assertEquals(1.6666666666666667d, StatUtils.variance(values, 2.5G), 1e-10d)
  }
}
