package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.xchart.HistogramChart

import java.math.RoundingMode

import static org.junit.jupiter.api.Assertions.assertTrue

class HistogramChartTest {


  /**
   * Equivalent to the following R code
   * <code><pre>
   * hist(airquality$Temp,
   *   main="Maximum daily temperature at La Guardia Airport",
   *   xlab="Temperature in degrees Fahrenheit",
   *   xlim=c(50,100),
   *   col="darkmagenta",
   *   freq=FALSE
   * )
   * </pre></code>
   */
  @Test
  void testDesityHistogram() {

  }

  /**
   * Equivalent to the following R code
   * <code><pre>
   * hist(airquality$Temp, breaks=9, main="With breaks=9")
   * </pre></code>
   */
  @Test
  void testFrequencyHistogram() {
    def hc = HistogramChart.create(Dataset.airquality())
    hc.addSeries('Temp', 9)
    File file = new File("build/testFrequencyHistogram.png")
    hc.exportPng(file)
    assertTrue(file.exists())
  }

  /**
   * Equivalent to the following R code
   * <code><pre>
   * h <- hist(airquality$Temp,ylim=c(0,40))
   * text(h$mids,h$counts,labels=h$counts, adj=c(0.5, -0.5))
   * </pre></code>
   */
  @Test
  void testFrequencyHistogramCustom() {

  }


  /**
   * Equivalent to the following R code:
   * 3.49 * sd(airquality$Temp) / length(airquality$Temp)^(1/3)
   */
  @Test
  void testScottsRule() {
    def scott = HistogramChart.scottsRule(Dataset.airquality().Temp)
    assert 6.17629378 == scott.setScale(8, RoundingMode.HALF_EVEN)
  }

  @Test
  void TestFreedmanDiaconisRule() {
    def fd = HistogramChart.freedmanDiaconisRule(Dataset.airquality()['Temp'])
    assert 4.86119308 == fd.setScale(8, RoundingMode.HALF_EVEN)
  }

}
