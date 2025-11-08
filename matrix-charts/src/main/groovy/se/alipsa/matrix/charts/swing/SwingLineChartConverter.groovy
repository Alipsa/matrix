package se.alipsa.matrix.charts.swing

import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import se.alipsa.matrix.charts.LineChart

class SwingLineChartConverter {

  static XYChart convert(LineChart chart) {
    XYChart lineChart =
        new XYChartBuilder()
        .title(chart.title ?: 'LineChart')
        .xAxisTitle(chart.xAxisTitle ?: "X")
        .yAxisTitle(chart.yAxisTitle ?: "Y")
        .build()

    if (chart.valueSeriesNames.size() == 1) {
      lineChart.getStyler().setLegendVisible(false)
    }
    int serieIdx = 0
    for (String serieName in chart.valueSeriesNames) {
      lineChart.addSeries(
          serieName,
          chart.getCategorySeries() as List<Number>,
          chart.getValueSeries()[serieIdx++] as List<Number>
          )
    }
    lineChart.getStyler().xAxisTickLabelsFormattingFunction = { Double val ->
      return String.valueOf(val)
    }
    SwingStyler.style(lineChart, chart)
    lineChart
  }
}
