package se.alipsa.matrix.charts.jfx;

import javafx.scene.chart.*

class ConverterUtil {

  static void populateVerticalSeries(XYChart<?,?> fxChart, se.alipsa.matrix.charts.Chart data) {
    populateSeries(fxChart, data, ConverterUtil.&verticalData)
  }

  static void populateHorizontalSeries(XYChart<?,?> fxChart, se.alipsa.matrix.charts.Chart data) {
    populateSeries(fxChart, data, ConverterUtil.&horizontalData)
  }

  private static XYChart.Data verticalData(List<?> categories, int i, List<?> column) {
    new XYChart.Data(categories.get(i), column.get(i))
  }

  private static XYChart.Data horizontalData(List<?> column, int i, List<?> categories) {
    new XYChart.Data(column.get(i), categories.get(i))
  }

  static void populateSeries(XYChart<?,?> fxChart, se.alipsa.matrix.charts.Chart data, Closure<XYChart.Data> dataCreator) {
    def series = data.getValueSeries()
    def categories = data.getCategorySeries()
    int colIdx = 0
    for (column in series) {
      XYChart.Series fxSeries = new XYChart.Series()
      fxSeries.name = data.valueSeriesNames[colIdx++]
      for (int i = 0; i < column.size(); i++) {
        fxSeries.getData().add(dataCreator(categories, i, column))
      }
      fxChart.getData().add(fxSeries)
    }
  }

  static void maybeHideLegend(se.alipsa.matrix.charts.Chart chart, Chart fxChart) {
    if (chart.getValueSeries().size() == 1 && ! chart.style.isLegendVisible()) {
      fxChart.setLegendVisible(false)
    }
  }
}
