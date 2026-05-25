package se.alipsa.matrix.xchart.abstractions

import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import org.knowm.xchart.CategorySeries
import org.knowm.xchart.style.CategoryStyler

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix

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
class AbstractCategoryChart<T extends AbstractCategoryChart> extends AbstractChart<T, CategoryChart, CategoryStyler, CategorySeries> {

  AbstractCategoryChart(Matrix matrix, Integer width = null, Integer height = null, CategorySeries.CategorySeriesRenderStyle chartType) {
    CategoryChartBuilder builder = new CategoryChartBuilder()
    if (width != null) {
      builder.width = width
    }
    if (height != null) {
      builder.height = height
    }
    initChart(builder.build(), matrix)
    style.defaultSeriesRenderStyle = chartType
  }

  /**
   * Add a data series to the chart using column names from the source Matrix.
   * The series will be named using the Y column name.
   *
   * @param xValueCol the name of the column containing category (X-axis) values
   * @param yValueCol the name of the column containing numeric (Y-axis) values
   * @param renderStyle optional render style override for this series (null to use chart default)
   * @return this chart for method chaining
   */
  T addSeries(String xValueCol, String yValueCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  /**
   * Add a named data series to the chart using column names from the source Matrix.
   *
   * @param seriesName the name for this series (displayed in legend)
   * @param xValueCol the name of the column containing category (X-axis) values
   * @param yValueCol the name of the column containing numeric (Y-axis) values
   * @param renderStyle optional render style override for this series (null to use chart default)
   * @return this chart for method chaining
   */
  T addSeries(String seriesName, String xValueCol, String yValueCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(seriesName, matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  /**
   * Add a data series to the chart using Column objects.
   * The series will be named using the Y column name.
   *
   * @param xCol the column containing category (X-axis) values
   * @param yCol the column containing numeric (Y-axis) values
   * @param renderStyle optional render style override for this series (null to use chart default)
   * @return this chart for method chaining
   */
  T addSeries(Column xCol, Column yCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(yCol.name, xCol, yCol, renderStyle)
  }

  /**
   * Add a named data series to the chart using Column objects.
   * This is the core implementation method that all other addSeries methods delegate to.
   *
   * @param name the name for this series (displayed in legend)
   * @param xCol the column containing category (X-axis) values
   * @param yCol the column containing numeric (Y-axis) values
   * @param renderStyle optional render style override for this series (null to use chart default)
   * @return this chart for method chaining
   */
  T addSeries(String name, Column xCol, Column yCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    CategorySeries series = xchart.addSeries(name, xCol, yCol)
    if (renderStyle != null) {
      series.setChartCategorySeriesRenderStyle(renderStyle)
    }
    this as T
  }

  /**
   * Add multiple Y-axis data series against a shared X-axis (category) column.
   * Each Y column becomes a separate series named after the column.
   *
   * @param xCol the name of the column containing shared category (X-axis) values
   * @param yCols the names of the columns containing numeric (Y-axis) values
   * @param renderStyle optional render style override for all series (null to use chart default)
   * @return this chart for method chaining
   */
  T addAllSeries(String xCol, List<String> yCols, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    yCols.each { String yCol ->
      addSeries(xCol, yCol, renderStyle)
    }
    this as T
  }

}
