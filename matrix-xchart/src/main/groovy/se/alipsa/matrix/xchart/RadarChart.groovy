package se.alipsa.matrix.xchart

import org.knowm.xchart.RadarChartBuilder
import org.knowm.xchart.RadarSeries
import org.knowm.xchart.style.RadarStyler

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.xchart.abstractions.AbstractChart
import se.alipsa.matrix.xchart.abstractions.ChartBuilder

/**
 * A RadarChart is a type of chart that displays multivariate data in a circular layout.
 * Each variable is represented as a spoke, and the data points are connected to form a polygon.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   'Category': ['A', 'B', 'C'],
 *   'Value1': [0.10, 0.20, 0.30],
 *   'Value2': [0.15, 0.25, 0.35],
 *   'Value3': [0.20, 0.30, 0.40]
 * ).types([String, Number, Number, Number]).build()
 *
 * def radarChart = RadarChart.create(matrix)
 *   .addSeries('Category')
 *
 * File file = new File('radarChart.png')
 * radarChart.exportPng(file)
 * </code></pre>
 */
class RadarChart extends AbstractChart<RadarChart, org.knowm.xchart.RadarChart, RadarStyler, RadarSeries> {

  private int numSeries = 0

  private RadarChart(Matrix matrix, Integer width = null, Integer height = null) {
    def builder = new RadarChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    initChart(builder.build(), matrix)
  }

  /**
   * Create a new radar chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new RadarChart instance
   */
  static RadarChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new RadarChart(matrix, width, height)
  }

  /**
   * Create a new radar chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new RadarChart instance
   */
  static RadarChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new RadarChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Add all rows of the Matrix as radar series, using the specified column for series labels.
   * All other columns become radar axes (radii). Values should be normalized to [0, 1].
   *
   * @param seriesNameColumn the name of the column containing series labels (one series per row)
   * @param transparency the fill alpha value (0 = fully transparent, 255 = fully opaque); defaults to 150
   * @return this chart for method chaining
   */
  RadarChart addSeries(String seriesNameColumn, Integer transparency = 150) {
    def labels = matrix.columnNames() - seriesNameColumn
    addSeries(seriesNameColumn, labels, transparency)
  }

  /** Add all rows using explicitly selected numeric radius columns. */
  RadarChart addSeries(String seriesNameColumn, List<String> radiusColumns, Integer transparency = 150) {
    if (radiusColumns == null || radiusColumns.isEmpty()) {
      throw new IllegalArgumentException('Radar chart requires at least one radius column')
    }
    radiusColumns.each { String column ->
      if (matrix.columnIndex(column) < 0 || !Number.isAssignableFrom(matrix.type(column))) {
        throw new IllegalArgumentException("Radar radius column '$column' must be numeric")
      }
    }
    if (matrix.columnIndex(seriesNameColumn) < 0) {
      throw new IllegalArgumentException("Radar label column '$seriesNameColumn' does not exist")
    }
    xchart.radiiLabels = radiusColumns as String[]
    matrix.rows().each { Row row ->
      def label = row[seriesNameColumn].toString()
      double[] radii = radiusColumns.collect { String column -> (row[column] as Number).doubleValue() } as double[]
      def s = xchart.addSeries(label, radii)
      makeFillTransparent(s, numSeries, transparency)
      numSeries++
    }
    this
  }

  /** Creates a deferred convenience builder. */
  static Builder builder(Matrix data) { new Builder(data) }

  static class Builder extends ChartBuilder<Builder> {
    private String labelColumn
    private List<String> radiusColumns
    Builder(Matrix data) { super(data) }
    Builder label(String column) { labelColumn = column; this }
    Builder values(String... columns) {
      if (columns == null || columns.length == 0) {
        throw new IllegalArgumentException('values requires at least one column')
      }
      radiusColumns = columns.toList(); this
    }
    @Override Builder x(String columnName) { throw new IllegalArgumentException('RadarChart does not support x(...)') }
    @Override Builder y(String... columnNames) { throw new IllegalArgumentException('RadarChart does not support y(...)') }
    @Override Builder xAxisTitle(String title) { throw new IllegalArgumentException('RadarChart does not support xAxisTitle(...)') }
    @Override Builder yAxisTitle(String title) { throw new IllegalArgumentException('RadarChart does not support yAxisTitle(...)') }
    RadarChart build() {
      if (labelColumn == null || radiusColumns == null) {
        throw new IllegalStateException('label(...) and values(...) must be called before build()')
      }
      requireColumn(labelColumn)
      radiusColumns.each { String column -> requireNumeric(column) }
      RadarChart chart = RadarChart.create(data, chartWidth, chartHeight)
      if (chartTitle != null) {
        chart.title = chartTitle
      }
      chart.addSeries(labelColumn, radiusColumns)
      chart
    }
  }

}
