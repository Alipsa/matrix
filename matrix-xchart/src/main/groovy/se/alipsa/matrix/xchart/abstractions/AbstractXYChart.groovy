package se.alipsa.matrix.xchart.abstractions

import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.XYStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.MatrixTheme

abstract class AbstractXYChart<T extends AbstractXYChart> extends AbstractChart<T> {

  AbstractXYChart(Matrix matrix, Integer width = null, Integer height = null,
                  XYSeries.XYSeriesRenderStyle chartType) {
    this.matrix = matrix
    XYChartBuilder builder = new XYChartBuilder()
    if (width != null) {
      builder.width = width
    }
    if (height != null) {
      builder.height = height
    }
    xchart = builder.build()
    style.theme = new MatrixTheme()
    def matrixName = matrix.matrixName
    if (matrixName != null && !matrixName.isBlank()) {
      title = matrix.matrixName
    }
    style.defaultSeriesRenderStyle = chartType
  }

  @Override
  XYChart getXchart() {
    return super.xchart as XYChart
  }

  T addSeries(String xValueCol, String yValueCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(String seriesName, String xValueCol, String yValueCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(seriesName, matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(String seriesName, String xValueCol, String yValueCol, String errorCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(seriesName, matrix.column(xValueCol), matrix.column(yValueCol), matrix.column(errorCol), renderStyle)
  }

  T addSeries(Column xCol, Column yCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(xCol.name, xCol, yCol, renderStyle)
  }

  T addSeries(Column xCol, Column yCol, Column errorCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    addSeries(xCol.name, xCol, yCol, errorCol, renderStyle)
  }

  T addSeries(String name, Column xCol, Column yCol, Column errorCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    XYSeries xySeries = xchart.addSeries(name, xCol, yCol, errorCol)
    if (renderStyle != null) {
      xySeries.setXYSeriesRenderStyle(renderStyle)
    }
    this as T
  }

  T addSeries(String name, Column xCol, Column yCol, XYSeries.XYSeriesRenderStyle renderStyle = null) {
    XYSeries xySeries = xchart.addSeries(name, xCol, yCol)
    if (renderStyle != null) {
      xySeries.setXYSeriesRenderStyle(renderStyle)
    }
    this as T
  }

  @Override
  XYSeries getSeries(String name) {
    xchart.getSeriesMap().get(name)
  }

  @Override
  Map<String, XYSeries> getSeries() {
    xchart.getSeriesMap()
  }

  @Override
  XYStyler getStyle() {
    xchart.getStyler()
  }
}
