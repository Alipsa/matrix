package se.alipsa.groovy.charts.jfx;

import javafx.scene.chart.*
import se.alipsa.groovy.matrix.ValueConverter;

class JfxPieChartConverter {

  static PieChart convert(se.alipsa.groovy.charts.PieChart chart) {

    PieChart fxChart = new PieChart();

    var categories = chart.getCategorySeries()
    var values = chart.getValueSeries()[0]
    var data = fxChart.getData()
    for (int i = 0; i < categories.size(); i++) {
      data.add(new PieChart.Data(String.valueOf(categories[i]), ValueConverter.asDouble(values[i])))
    }

    fxChart.setTitle(chart.getTitle())
    JfxStyler.style(fxChart, chart)
    return fxChart
  }
}
