package se.alipsa.matrix.xchart.abstractions

import groovy.transform.CompileStatic
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.XYStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.MatrixTheme

/**
 * Base class for XY chart types (Line, Area, Scatter).
 * Provides common functionality for charts with X and Y numeric axes.
 * <p>
 * XY charts display data as a series of points with X and Y coordinates.
 * This abstract class handles chart initialization, series management,
 * and provides methods for adding data series to the chart.
 * </p>
 * <p>
 * Subclasses specify the render style (Line, Area, or Scatter) and
 * can add additional customization for their specific chart type.
 * </p>
 *
 * @param <T> the concrete XY chart type for fluent API chaining
 */
@CompileStatic
abstract class AbstractXYChart<T extends AbstractXYChart> extends AbstractChart<T, XYChart, XYStyler, XYSeries> {

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
}
