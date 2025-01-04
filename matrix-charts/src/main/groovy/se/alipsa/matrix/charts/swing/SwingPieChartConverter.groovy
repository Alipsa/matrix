package se.alipsa.matrix.charts.swing

import org.knowm.xchart.PieChartBuilder
import org.knowm.xchart.style.PieStyler
import se.alipsa.matrix.charts.PieChart

class SwingPieChartConverter {

  static org.knowm.xchart.PieChart convert(PieChart chart) {
    org.knowm.xchart.PieChart pieChart =
        new PieChartBuilder()
        .title(chart.getTitle() ?: '')
        .build()
    pieChart.getStyler().setLegendVisible(false)
    pieChart.getStyler().setLabelType(PieStyler.LabelType.NameAndValue)
    def valueSerie = chart.getValueSeries()[0]
    chart.getCategorySeries().eachWithIndex { serie, idx ->
      pieChart.addSeries(String.valueOf(serie), valueSerie[idx] as Number)
    }
    SwingStyler.style(pieChart, chart)
    pieChart
  }
}
