package se.alipsa.matrix.xchart

import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import org.knowm.xchart.HeatMapSeries
import org.knowm.xchart.style.HeatMapStyler

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.xchart.abstractions.AbstractChart
import se.alipsa.matrix.xchart.abstractions.ChartBuilder

/**
 * A heat map (or heatmap) is a 2-dimensional data visualization technique that represents the magnitude of
 * individual values within a dataset as a color.
 * Example usage:
 * <pre><code>
 * def data = [123, 210, 89, 87, 245, 187, 100, 110, 99, 59, 110, 101]
 * Matrix matrix = Matrix.builder().data( 'c': data ).types(Number).build()
 * def hc = HeatmapChart.create(matrix, 1000, 600)
 *   .setTitle(getClass().getSimpleName())
 *   .addSeries('Basic HeatMap','c', 4)
 *
 * File file = new File('build/testHeatmap.png')
 * hc.exportPng(file)
 * </code></pre>
 */
class HeatmapChart extends AbstractChart<HeatmapChart, HeatMapChart, HeatMapStyler, HeatMapSeries> {

  final Number[] numberArray = new Number[]{}
  Matrix heatMapMatrix

  private HeatmapChart(Matrix matrix, Integer width = null, Integer height = null) {
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
   * Create a new heatmap chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new HeatmapChart instance
   */
  static HeatmapChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new HeatmapChart(matrix, width, height)
  }

  /**
   * Create a new heatmap chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new HeatmapChart instance
   */
  static HeatmapChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new HeatmapChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Add a heatmap series from a single column, reshaping it into a grid.
   *
   * @param seriesName the name for this series
   * @param colName the name of the column containing values
   * @param columns the number of grid columns; if null, auto-detected from the square root of the column size
   * @return this chart for method chaining
   */
  HeatmapChart addSeries(String seriesName, String colName, Integer columns = null) {
    addSeries(seriesName, matrix.column(colName), columns)
  }

  /**
   * Add a heatmap series from a single Column, reshaping it into a grid.
   *
   * @param seriesName the name for this series
   * @param col the Column containing values
   * @param columns the number of grid columns; if null, auto-detected from the square root of the column size
   * @return this chart for method chaining
   */
  HeatmapChart addSeries(String seriesName, Column col, Integer columns = null) {
    int nCols = validateVectorColumn(col, columns)
    int nRows = (col.size() / nCols) as int
    List<Number[]> heatData = []
    List<List> tmpRows = []
    int idx = 0
    (0..<nRows).each { int r ->
      def tmpRow = []
      (0..<nCols).each { int c ->
        heatData << [c, r, col[idx]].toArray(numberArray)
        tmpRow << col[idx]
        idx++
      }
      tmpRows << tmpRow
    }
    heatMapMatrix = Matrix.builder().rows(tmpRows).build()
    xchart.addSeries(seriesName, 1..nCols, 1..nRows, heatData)
    this
  }

  /**
   * Add a heatmap series from a list of Columns with auto-generated labels.
   *
   * @param seriesName the name for this series
   * @param columns the list of Columns, each representing a column in the heatmap grid
   * @return this chart for method chaining
   */
  HeatmapChart addSeries(String seriesName, List<Column> columns) {
    validateColumns(columns)
    int nCols = columns.size()
    int nRows = columns[0].size()
    addSeries(seriesName, 1..nCols, 1..nRows, columns)
  }

  /**
   * Add a heatmap series from a list of Columns with custom axis labels.
   *
   * @param seriesName the name for this series
   * @param columnLabels labels for the X-axis (columns)
   * @param rowLabels labels for the Y-axis (rows)
   * @param columns the list of Columns, each representing a column in the heatmap grid
   * @return this chart for method chaining
   */
  HeatmapChart addSeries(String seriesName, List<?> columnLabels, List<?> rowLabels, List<Column> columns) {
    validateColumns(columns)
    int nCols = columns.size()
    int nRows = columns[0].size()
    List<Number[]> heatData = []

    List<List> tmpRows = []
    (0..<nRows).each { int r ->
      List tmpRow = []
      (0..<nCols).each { int c ->
        heatData << [c, r, columns[c][r]].toArray(numberArray)
        tmpRow << columns[c][r]
      }
      tmpRows << tmpRow
    }
    heatMapMatrix = Matrix.builder().rows(tmpRows).build()
    xchart.addSeries(seriesName, columnLabels, rowLabels, heatData)
    this
  }

  /**
   * Add all columns (except the specified one) as a heatmap series, using the specified column for row labels.
   * The Matrix must have a name set before calling this method.
   *
   * @param columnName the name of the column to use as row labels (excluded from heatmap values)
   * @return this chart for method chaining
   * @throws IllegalArgumentException if the Matrix has no name
   */
  HeatmapChart addAllToSeriesBy(String columnName) {
    if (matrix.matrixName == null || matrix.matrixName.isBlank()) {
      throw new IllegalArgumentException('Matrix must have a name before charting all columns as a heatmap series')
    }
    int byIdx = matrix.columnIndex(columnName)
    List<Number[]> heatData = []
    List<List> tmpRows = []
    matrix.rows().eachWithIndex { row, r ->
      List tmpRow = []
      List valueRow = (row as Row).minusColumn(byIdx)
      valueRow.eachWithIndex { Object entry, int c ->
        heatData << [c, r, entry as Number].toArray(numberArray)
        tmpRow << entry
      }
      tmpRows << tmpRow
    }
    heatMapMatrix = Matrix.builder()
        .rows(tmpRows)
        .columnNames(matrix.columnNames() - columnName)
        .build()
    List<String> colNames = matrix.columnNames() - columnName
    xchart.addSeries(matrix.matrixName, colNames, matrix[columnName], heatData)
    this
  }

  private static int validateVectorColumn(Column col, Integer columns) {
    if (col == null || col.isEmpty()) {
      throw new IllegalArgumentException('Heatmap column must contain at least one value')
    }
    int nCols = columns == null ? autoColumnCount(col) : columns
    if (nCols <= 0) {
      throw new IllegalArgumentException("Heatmap column count must be positive, got $nCols")
    }
    if (col.size() % nCols != 0) {
      throw new IllegalArgumentException("Heatmap column '${col.name}' with ${col.size()} values cannot be evenly divided into $nCols columns")
    }
    nCols
  }

  private static void validateColumns(List<Column> columns) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException('Heatmap column list must contain at least one column')
    }
    Integer expectedSize = null
    columns.each { Column column ->
      if (column == null) {
        throw new IllegalArgumentException('Heatmap column list must not contain null columns')
      }
      expectedSize = expectedSize ?: column.size()
      if (column.size() != expectedSize) {
        throw new IllegalArgumentException("Heatmap columns must have equal lengths; expected $expectedSize values but column '${column.name}' has ${column.size()}")
      }
    }
  }

  private static int autoColumnCount(Column col) {
    int nCols = col.size().sqrt() as int
    if (nCols * nCols != col.size()) {
      throw new IllegalArgumentException("Heatmap column '${col.name}' with ${col.size()} values has no integer square root; pass an explicit column count instead of relying on auto-detection")
    }
    nCols
  }

  /** Creates a deferred convenience builder. */
  static Builder builder(Matrix data) { new Builder(data) }

  static class Builder extends ChartBuilder<Builder> {
    private String seriesName = 'Heatmap'
    private String vectorColumn
    private Integer vectorColumns
    private List<String> valueColumns
    private List<?> rows
    private List<?> columns

    Builder(Matrix data) { super(data) }
    Builder seriesName(String name) { seriesName = name; this }
    Builder values(String columnName, Integer columns = null) { vectorColumn = columnName; vectorColumns = columns; this }
    Builder values(List<String> columnNames) {
      if (columnNames == null || columnNames.isEmpty()) {
        throw new IllegalArgumentException('values requires at least one column')
      }
      valueColumns = columnNames; this
    }
    Builder rowLabels(List<?> labels) { rows = labels; this }
    Builder columnLabels(List<?> labels) { columns = labels; this }
    @Override Builder xAxisTitle(String title) { throw new IllegalArgumentException('HeatmapChart does not support xAxisTitle(...)') }
    @Override Builder yAxisTitle(String title) { throw new IllegalArgumentException('HeatmapChart does not support yAxisTitle(...)') }
    HeatmapChart build() {
      if (vectorColumn == null && valueColumns == null) {
        throw new IllegalStateException('values(...) must be called before build()')
      }
      if (vectorColumn != null && valueColumns != null) {
        throw new IllegalStateException('Specify either vector or matrix heatmap values, not both')
      }
      HeatmapChart chart = HeatmapChart.create(data, chartWidth, chartHeight)
      if (chartTitle != null) {
        chart.title = chartTitle
      }
      if (vectorColumn != null) {
        if (rows != null || columns != null) {
          throw new IllegalArgumentException('Labels are supported only for matrix-shaped heatmaps')
        }
        requireNumeric(vectorColumn)
        chart.addSeries(seriesName, vectorColumn, vectorColumns)
      } else {
        valueColumns.each { String column -> requireNumeric(column) }
        if ((rows == null) != (columns == null)) {
          throw new IllegalStateException('rowLabels(...) and columnLabels(...) must be provided together')
        }
        List<Column> selected = valueColumns.collect { String column -> data.column(column) }
        rows == null ? chart.addSeries(seriesName, selected) : chart.addSeries(seriesName, columns, rows, selected)
      }
      chart
    }
  }

}
