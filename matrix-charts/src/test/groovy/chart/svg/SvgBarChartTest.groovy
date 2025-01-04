package chart.svg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charts.BarChart
import se.alipsa.matrix.charts.ChartType
import se.alipsa.matrix.charts.svg.SvgBarChart
import se.alipsa.matrix.charts.util.DateUtil
import se.alipsa.matrix.core.Matrix

import java.text.DecimalFormat

class SvgBarChartTest {

  @Test
  void testBarChart() {
    def data = Matrix.builder()
        .data([
            month: DateUtil.monthNames,
            sales: [123, 142, 100, 141, 165, 98, 75, 99, 143, 150, 100, 112]
        ])
        .types(String, int)
        .build()

    BarChart barChart = BarChart.createVertical(
        "Sales Per Month",
        data,
        'month',
        ChartType.BASIC,
        'sales'
    )
    barChart.style.css = '''
      .chart {
       fill: #fff6a7
      }
      .chart-title {
        font-weight: bold;
        font-size: 18px;
      }
      .chart-content .bar {
        fill: red
      }
      #bar-0-4 {
        fill: navy
      }
      #xaxis {
        stroke: green;
        stroke-width: 5px;
      }
      #yaxis {
        stroke: blue;
        stroke-width: 5px;
      }
    '''
    def format = DecimalFormat.instance
    format.maximumFractionDigits = 0
    barChart.style.yLabels.format = format
    // todo add a style for title
    // barChart.style.title.fontSize = "12px"
    // barChart.style.title.color = "navy"

    SvgBarChart svgBarChart = new SvgBarChart(barChart, 640, 480)
    File file = new File("build/svgBarChart.svg")
    file.write(svgBarChart.asString(true))
    println "wrote $file.absolutePath"
  }
}
