package se.alipsa.matrix.xchart

import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.XYStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

abstract class AbstractXYChart<T extends AbstractXYChart> extends AbstractChart {

  AbstractXYChart(Matrix matrix, int width, int height,
                  XYSeries.XYSeriesRenderStyle chartType) {
    this.matrix = matrix
    xchart = new XYChartBuilder()
        .width(width)
        .height(height)
        .build()
    style.theme = new MatrixTheme()
    def matrixName = matrix.matrixName
    if (matrixName != null && !matrixName.isBlank()) {
      title = matrix.matrixName
    }
    style.defaultSeriesRenderStyle = chartType
  }

  XYStyler getStyle() {
    xchart.getStyler()
  }

  XYChart getXchart() {
    return super.xchart as XYChart
  }

  T setTitle(String title) {
    xchart.setTitle(title)
    return this as T
  }

  T addSeries(String xValueCol, String yValueCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(String seriesName, String xValueCol, String yValueCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(seriesName, matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(Column xCol, Column yCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(xCol.name, xCol, yCol, renderStyle)
  }

  T addSeries(String name, Column xCol, Column yCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    XYSeries xySeries = xchart.addSeries(name,
        ListConverter.toDoubleArray(xCol),
        ListConverter.toDoubleArray(yCol)
    )
    if (renderStyle != null) {
      xySeries.setXYSeriesRenderStyle(renderStyle)
    }
    this as T
  }

  XYSeries getSeries(String name) {
    xchart.getSeriesMap().get(name)
  }

  Map<String, XYSeries> getSeries() {
    xchart.getSeriesMap()
  }
}
