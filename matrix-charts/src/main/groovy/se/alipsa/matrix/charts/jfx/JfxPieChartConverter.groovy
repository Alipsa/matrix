package se.alipsa.matrix.charts.jfx;

import javafx.scene.chart.*
import se.alipsa.matrix.core.ValueConverter;

class JfxPieChartConverter {

  static PieChart convert(se.alipsa.matrix.charts.PieChart chart) {

    PieChart fxChart = new PieChart();

    var categories = chart.getCategorySeries()
    var values = chart.getValueSeries()[0]
    var data = fxChart.getData()
    for (int i = 0; i < categories.size(); i++) {
      data.add(new PieChart.Data(String.valueOf(categories[i]), ValueConverter.asDouble(values[i])))
    }
    fxChart.setTitle(chart.getTitle())

    // Note: This must occur last in this method as some things require manipulation of the jfx chart itself,
    // not just the style part.
    JfxStyler.style(fxChart, chart)
    return fxChart
  }
}
