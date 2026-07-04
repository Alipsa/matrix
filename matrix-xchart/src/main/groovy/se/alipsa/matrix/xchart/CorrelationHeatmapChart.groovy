package se.alipsa.matrix.xchart

import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import org.knowm.xchart.HeatMapSeries
import org.knowm.xchart.style.HeatMapStyler

import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Correlation
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A correlation heatmap chart is a graphical representation of the correlation matrix, where the values are
 * represented by colors. It is used to visualize the correlation between different variables in a dataset.
 * Sample usage:
 * <pre><code>
 * Matrix whisky = Matrix.builder().data(this.class.getResource('/ScotchWhisky01.csv')).build()
 * def chart = CorrelationHeatmapChart.create(whisky, 800, 600)
 *   .setTitle('Correlation Heatmap of Scotch Whisky Data')
 *   .addSeries('Correlation', whisky.columnNames() - 'Distillery' as List<String>) // only numeric columns allowed
 * chart.exportSvg(new File('build/correlationHeatmap.svg')
 * </code></pre>
 */
class CorrelationHeatmapChart extends AbstractChart<CorrelationHeatmapChart, HeatMapChart, HeatMapStyler, HeatMapSeries> {

  private static final int CORRELATION_SCALE = 2
  final Number[] numberArray = new Number[]{}

  private CorrelationHeatmapChart(Matrix matrix, Integer width = null, Integer height = null) {
    def builder = new HeatMapChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    initChart(builder.build(), matrix)
    style.setPlotContentSize(1)
    style.setShowValue(true)
  }

  /**
   * Create a new correlation heatmap chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new CorrelationHeatmapChart instance
   */
  static CorrelationHeatmapChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new CorrelationHeatmapChart(matrix, width, height)
  }

  /**
   * Create a new correlation heatmap chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new CorrelationHeatmapChart instance
   */
  static CorrelationHeatmapChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new CorrelationHeatmapChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Add a correlation heatmap series computed from the specified numeric columns.
   * The correlation matrix is calculated using Pearson correlation coefficients. Self-correlations are
   * rendered as 1, and undefined non-diagonal correlations from zero-variance columns are rendered as 0.
   *
   * @param title the name for this series (displayed in legend)
   * @param columnNames the names of the numeric columns to compute correlations for
   * @return this chart for method chaining
   * @throws IllegalArgumentException if columnNames is empty, or any column is missing or non-numeric
   */
  CorrelationHeatmapChart addSeries(String title, List<String> columnNames) {
    if (columnNames.isEmpty()) {
      throw new IllegalArgumentException('At least one column name must be provided for the correlation heatmap series.')
    }
    validateColumns(columnNames)
    Matrix data = matrix.select(columnNames)
    int size = columnNames.size()
    List<List<Number>> corr = (0..<size).collect { int c ->
      (0..<size).collect { int r ->
        List<BigDecimal> xValues = ListConverter.toBigDecimals(data[c])
        List<BigDecimal> yValues = ListConverter.toBigDecimals(data[r])
        roundCorrelation(Correlation.cor(xValues, yValues), c == r) as Number
      }
    }
    addSeries(title, columnNames, columnNames, corr)
  }

  /**
   * Add a correlation heatmap series with pre-computed data and custom labels.
   *
   * @param seriesName the name for this series
   * @param columnLabels labels for the X-axis
   * @param rowLabels labels for the Y-axis
   * @param columns the correlation data organized as a list of column lists
   * @return this chart for method chaining
   */
  CorrelationHeatmapChart addSeries(String seriesName, List<String> columnLabels, List<String> rowLabels, List<List<Number>> columns) {
    int nCols = columns.size()
    int nRows = columns[0].size()
    List<Number[]> heatData = []

    def tmpRows = []
    (0..<nRows).each { int r ->
      def tmpRow = []
      (0..<nCols).each { int c ->
        heatData << [c, r, columns[c][r]].toArray(numberArray)
        tmpRow << columns[c][r]
      }
      tmpRows << tmpRow
    }
    xchart.addSeries(seriesName, columnLabels, rowLabels, heatData)
    this
  }

  private static BigDecimal roundCorrelation(BigDecimal correlation, boolean selfCorrelation) {
    if (selfCorrelation) {
      return BigDecimal.ONE
    }
    correlation == null ? BigDecimal.ZERO : correlation.round(CORRELATION_SCALE)
  }

  private void validateColumns(List<String> columnNames) {
    columnNames.each { String columnName ->
      if (matrix.columnIndex(columnName) == -1) {
        throw new IllegalArgumentException("Correlation heatmap column '$columnName' does not exist")
      }
      Class columnType = matrix.type(columnName)
      if (columnType == null || !Number.isAssignableFrom(columnType)) {
        throw new IllegalArgumentException("Correlation heatmap column '$columnName' must be numeric, got ${columnType?.simpleName ?: 'null'}")
      }
    }
  }

}
