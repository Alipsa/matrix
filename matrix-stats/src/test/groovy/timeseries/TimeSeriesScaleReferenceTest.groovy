package timeseries

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.timeseries.Adf
import se.alipsa.matrix.stats.timeseries.AdfGls
import se.alipsa.matrix.stats.timeseries.Granger

class TimeSeriesScaleReferenceTest {

  @Test
  void grangerAndAdfAreInvariantToUnits() {
    List x = (1..60).collect { Math.sin(0.17d * it) + Math.cos(0.31d * it) }
    List y = (0..<60).collect { int i -> (i == 0 ? 0 : x[i - 1]) + 0.2d * Math.sin(0.43d * (i + 1)) }
    def granger = Granger.test(x, y, 2)
    // R 4.6.1 anova(restricted,unrestricted), common lag-2 rows; timeseries.R.
    assertEquals(111.78465d, granger.statistic as double, 1e-5)
    def adf = Adf.test(y, 2, 'drift')
    [1e-8d, 1e8d].each { double scale ->
      assertEquals(granger.statistic as double, Granger.test(x.collect { it * scale }, y.collect { it * scale }, 2).statistic as double, 1e-7)
      assertEquals(adf.statistic as double, Adf.test(y.collect { it * scale }, 2, 'drift').statistic as double, 1e-9)
    }
    assertThrows(IllegalArgumentException) { Granger.test(x[0..<10], y[0..<10], 3) }
  }

  @Test
  void commonSampleMaicMatchesNgPerronReference() {
    List values = getClass().getResource('/r-reference/ar2-200.csv').readLines().collect { new BigDecimal(it.trim()) }
    // R 4.6.1 direct Ng-Perron equation port in timeseries.R, set.seed(73) arima.sim(c(.6,-.25),n=200).
    assertEquals(9, AdfGls.test(values).lags)
  }
}
