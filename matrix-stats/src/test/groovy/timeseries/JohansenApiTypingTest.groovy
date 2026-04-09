package timeseries

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.timeseries.Johansen

@CompileStatic
class JohansenApiTypingTest {

  @Test
  void testPrimitiveSeriesTypedOverload() {
    Random rnd = new Random(42)
    int n = 80
    double[] y1 = new double[n]
    double[] y2 = new double[n]
    for (int i = 1; i < n; i++) {
      y1[i] = y1[i - 1] + rnd.nextGaussian() * 0.4d
      y2[i] = y1[i] + rnd.nextGaussian() * 0.1d
    }
    List<double[]> data = [y1, y2]

    Johansen.JohansenResult result = Johansen.testArrays(data, 1)

    assertNotNull(result)
    assertEquals(2, result.numVariables)
  }

  @Test
  void testNumericListTypedOverload() {
    Random rnd = new Random(84)
    int n = 80
    List<BigDecimal> y1 = [0.0]
    List<BigDecimal> y2 = [0.0]
    for (int i = 1; i < n; i++) {
      BigDecimal nextY1 = y1[i - 1] + rnd.nextGaussian() * 0.4d
      y1 << nextY1
      y2 << nextY1 + rnd.nextGaussian() * 0.1d
    }
    Collection<List<BigDecimal>> data = [y1, y2]

    Johansen.JohansenResult result = Johansen.testSeries(data, 1)

    assertNotNull(result)
    assertEquals(2, result.numVariables)
  }
}
