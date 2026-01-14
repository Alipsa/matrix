package chart

import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.charts.LineChart
import se.alipsa.matrix.charts.swing.SwingPlot
import se.alipsa.matrix.charts.Plot
import java.time.YearMonth

class LineChartTest {

  List<YearMonth> yearMonthsBetween(YearMonth start, YearMonth end) {
    def result = []
    def current = start
    while (current.isBefore(end)) {
      result << current
      current = current.plusMonths(1)
    }
    result << end
    return result
  }

  @Test
  void testSingleLine() {

    def salesData = Matrix.builder().matrixName('Salesdata').columns(
        [
            yearMonth: [202301, 202302, 202303, 202304, 202305, 202306, 202307, 202308, 202309, 202310, 202311, 202312],
            //yearMonth: [1,2,3,4,5,6,7,8,9,10,11,12],
            sales    : [23, 14, 15, 24, 34, 36, 22, 45, 43, 17, 29, 25]
        ])
    .types([YearMonth, int])
    .build()

    def chart = LineChart.create(salesData, 'yearMonth', 'sales')
    assertIterableEquals(['sales'], chart.valueSeriesNames, "ValueSeries Names")
    assertEquals(1, chart.valueSeries.size(), "Number of valueseries")
    assertEquals(12, chart.categorySeries.size(), "Number of elements in x")
    assertEquals(12, chart.valueSeries[0].size(), "Number of elements in y")
    File file = File.createTempFile("JfxLineChart", ".png")
    Plot.png(chart, file)
    //println("Wrote ${file.getAbsolutePath()}")
    assertTrue(file.exists())
    file.delete()
    file = File.createTempFile("SwingLineChart", ".png")
    SwingPlot.png(chart, file)
    //println("Wrote ${file.getAbsolutePath()}")
    assertTrue(file.exists())
    file.delete()
  }
}