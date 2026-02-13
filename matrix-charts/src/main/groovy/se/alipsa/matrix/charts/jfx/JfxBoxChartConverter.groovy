package se.alipsa.matrix.charts.jfx

import javafx.scene.chart.Chart
import se.alipsa.matrix.charts.BoxChart

/**
 * Converts a {@link BoxChart} model into a {@link JfxBoxChart} for JavaFX rendering.
 */
class JfxBoxChartConverter {

  static Chart convert(BoxChart chart) {
    List<String> categories = chart.categorySeries.collect { String.valueOf(it) }
    def jfxChart = new JfxBoxChart(
        categories,
        chart.valueSeries,
        chart.title,
        chart.xAxisTitle,
        chart.yAxisTitle
    )
    JfxStyler.style(jfxChart, chart)
    return jfxChart
  }
}
