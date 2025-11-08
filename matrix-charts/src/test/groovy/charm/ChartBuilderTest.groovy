package charm

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.ChartBuilder
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.core.*

class ChartBuilderTest {

  @Test
  void testBarChart() {
    def mtcars = Dataset.mtcars()
    def gearsAndVs = Stat.frequency(mtcars, 'gear', 'vs', false)
    //ChartBuilder.create(gearsAndVs)
    //.barChart(ChartDirection.VERTICAL)
    //.build()
  }

  @Test
  void testBarChartWithLine() {
  }

  @Test
  void testScatterPlot() {
  }

  @Test
  void testScatterPlotWithRegressionLine() {
  }
}
