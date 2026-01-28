package se.alipsa.matrix.xchart.abstractions

import groovy.transform.CompileStatic
import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import org.knowm.xchart.CategorySeries
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.CategoryStyler
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.MatrixTheme

/**
 * Base class for category chart types (Bar, Stick, Histogram).
 * Provides common functionality for charts with categorical X-axis and numeric Y-axis.
 * <p>
 * Category charts display data organized by categories or discrete groups.
 * This abstract class handles chart initialization, series management,
 * and provides methods for adding categorical data series to the chart.
 * </p>
 * <p>
 * Subclasses specify the render style (Bar, Stick, etc.) and
 * can add additional customization for their specific chart type.
 * </p>
 *
 * @param <T> the concrete category chart type for fluent API chaining
 */
@CompileStatic
class AbstractCategoryChart<T extends AbstractCategoryChart> extends AbstractChart<T, CategoryChart, CategoryStyler, CategorySeries> {

  AbstractCategoryChart(Matrix matrix, Integer width = null, Integer height = null, CategorySeries.CategorySeriesRenderStyle chartType) {
    this.matrix = matrix
    CategoryChartBuilder builder = new CategoryChartBuilder()
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


  T addSeries(String xValueCol, String yValueCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(String seriesName, String xValueCol, String yValueCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(seriesName, matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(Column xCol, Column yCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(yCol.name, xCol, yCol, renderStyle)
  }

  T addSeries(String name, Column xCol, Column yCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    CategorySeries series = xchart.addSeries(name, xCol, yCol)
    if (renderStyle != null) {
      series.setChartCategorySeriesRenderStyle(renderStyle)
    }
    this as T
  }

}
