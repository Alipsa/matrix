package se.alipsa.groovy.charts.jfx;

import javafx.scene.chart.*

class ConverterUtil {

  static void populateVerticalSeries(XYChart<?,?> fxChart, se.alipsa.groovy.charts.Chart data) {
    populateSeries(fxChart, data, ConverterUtil.&verticalData)
  }

  static void populateHorizontalSeries(XYChart<?,?> fxChart, se.alipsa.groovy.charts.Chart data) {
    populateSeries(fxChart, data, ConverterUtil.&horizontalData)
  }

  private static XYChart.Data verticalData(List<?> categories, int i, List<?> column) {
    new XYChart.Data(categories.get(i), column.get(i))
  }

  private static XYChart.Data horizontalData(List<?> column, int i, List<?> categories) {
    new XYChart.Data(column.get(i), categories.get(i))
  }

  static void populateSeries(XYChart<?,?> fxChart, se.alipsa.groovy.charts.Chart data, Closure<XYChart.Data> dataCreator) {
    def series = data.getValueSeries()
    def categories = data.getCategorySeries()
    for (column in series) {
      XYChart.Series fxSeries = new XYChart.Series()
      for (int i = 0; i < column.size(); i++) {
        fxSeries.getData().add(dataCreator(categories, i, column))
      }
      fxChart.getData().add(fxSeries)
    }
  }
}
