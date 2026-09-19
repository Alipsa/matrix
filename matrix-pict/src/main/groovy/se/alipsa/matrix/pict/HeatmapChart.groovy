package se.alipsa.matrix.pict

import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

import java.awt.Color

/**
 * A grid chart whose selected numeric matrix columns form coloured tiles.
 * Values are stored column-major, so {@code values[column][row]} addresses a cell.
 */
@SuppressWarnings(['DuplicateStringLiteral', 'UnnecessaryObjectReferences'])
class HeatmapChart extends Chart<HeatmapChart> {

  /** Selected numeric column names, in x-axis order. */
  List<String> columnLabels = []
  /** Row labels, either from the selected label column or one-based row numbers. */
  List<?> rowLabels = []
  /** Column-major tile values. */
  List<List<BigDecimal>> values = []
  /** Whether cell values are rendered as labels. */
  boolean showValues = true
  /** Optional number of decimal places used for rendered cell-value labels. */
  Integer valueDecimals
  /** Optional colour for all rendered cell-value labels. */
  Color labelColor
  /** Gradient low colour, or null for Charm's default. */
  Color lowColor
  /** Optional gradient midpoint colour. */
  Color midColor
  /** Gradient high colour, or null for Charm's default. */
  Color highColor
  /** Value at which the midpoint colour is used. */
  BigDecimal midpoint
  /** Optional fixed fill-domain limits. */
  List<BigDecimal> fillLimits

  /**
   * Creates a heatmap using a column for row labels.
   *
   * @param title chart title
   * @param data source matrix
   * @param rowLabelColumn row label column
   * @param columns numeric value columns
   * @return a populated heatmap
   */
  static HeatmapChart create(String title, Matrix data, String rowLabelColumn, List<String> columns) {
    HeatmapChart chart = new HeatmapChart()
    populate(chart, title, data, rowLabelColumn, columns)
    chart
  }

  /**
   * Creates a heatmap with one-based row labels.
   *
   * @param title chart title
   * @param data source matrix
   * @param columns numeric value columns
   * @return a populated heatmap
   */
  static HeatmapChart create(String title, Matrix data, List<String> columns) {
    create(title, data, null, columns)
  }

  /** Fills a heatmap from its source data. */
  protected static void populate(HeatmapChart chart, String title, Matrix data, String rowLabelColumn, List<String> columns) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException('Heatmap requires at least one value column')
    }
    if (data.rowCount() == 0) {
      throw new IllegalArgumentException("Heatmap data ${data.matrixName ?: ''} has no rows".trim())
    }
    columns.each { String column -> requireNumericColumn(data, column) }
    if (rowLabelColumn != null) {
      requireColumn(data, rowLabelColumn)
      if (columns.contains(rowLabelColumn)) {
        throw new IllegalArgumentException("The row label column '${rowLabelColumn}' cannot also be a value column")
      }
    }
    chart.title = title
    chart.columnLabels = new ArrayList<String>(columns)
    chart.rowLabels = rowLabelColumn == null ? (1..data.rowCount()).toList() : data.column(rowLabelColumn) as List<?>
    chart.values = columns.collect { String column -> ListConverter.toBigDecimals(data.column(column)) }
    chart.categorySeries = chart.rowLabels
    chart.valueSeries = chart.values as List<List<?>>
    chart.valueSeriesNames = chart.columnLabels
  }

  /**
   * Creates a builder for a heatmap.
   *
   * @param data source matrix
   * @return a builder
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /** Fluent builder for {@link HeatmapChart}. */
  static class Builder extends HeatmapBuilder<Builder, HeatmapChart> {

    Builder(Matrix data) { super(data) }

    /** Builds the heatmap. */
    @Override
    HeatmapChart build() {
      if (selectedColumns == null) {
        throw new IllegalStateException('columns(...) must be called before build()')
      }
      HeatmapChart chart = new HeatmapChart()
      HeatmapChart.populate(chart, this.@title, data, rowLabelColumn, selectedColumns)
      applyTo(chart)
      applyHeatmapOptions(chart)
      chart
    }
  }

  /** Shared fluent configuration for heatmap-like charts. */
  abstract static class HeatmapBuilder<B extends HeatmapBuilder, C extends HeatmapChart> extends Chart.ChartBuilder<B, C> {

    protected String rowLabelColumn
    protected List<String> selectedColumns
    protected boolean showValues = true
    protected Integer valueDecimals
    protected Color labelColor
    protected Color lowColor
    protected Color midColor
    protected Color highColor
    protected BigDecimal midpoint

    protected HeatmapBuilder(Matrix data) { super(data) }

    /** Sets the row-label column. */
    B rowLabels(String columnName) { rowLabelColumn = columnName; this as B }

    /** Sets numeric value columns. */
    B columns(String... columnNames) {
      if (columnNames == null || columnNames.length == 0) {
        throw new IllegalArgumentException('columns(...) requires at least one column')
      }
      selectedColumns = columnNames.toList()
      this as B
    }

    /** Sets numeric value columns. */
    B columns(List<String> columnNames) {
      if (columnNames == null || columnNames.isEmpty()) {
        throw new IllegalArgumentException('columns(...) requires at least one column')
      }
      selectedColumns = new ArrayList<String>(columnNames)
      this as B
    }

    /** Toggles value labels. */
    B showValues(boolean show) { showValues = show; this as B }

    /**
     * Sets the number of decimal places used for cell-value labels.
     *
     * @param decimals non-negative number of decimal places; when omitted, values retain their natural scale
     * @return this builder
     */
    B valueDecimals(int decimals) {
      if (decimals < 0) {
        throw new IllegalArgumentException("valueDecimals must be non-negative, got ${decimals}")
      }
      valueDecimals = decimals
      this as B
    }

    /**
     * Sets the colour used for every cell-value label.
     *
     * <p>When omitted, label colour is selected for contrast with each tile.</p>
     *
     * @param color label colour
     * @return this builder
     */
    B labelColor(Color color) {
      labelColor = color
      this as B
    }

    /** Sets a two-colour gradient. */
    B colors(Color low, Color high) {
      lowColor = low
      midColor = null
      highColor = high
      midpoint = null
      this as B
    }

    /** Sets a three-colour gradient. */
    B colors(Color low, Color mid, Color high, Number midpointValue = null) {
      lowColor = low
      midColor = mid
      highColor = high
      midpoint = midpointValue == null ? null : midpointValue as BigDecimal
      this as B
    }

    /** Applies heatmap-specific builder settings. */
    protected void applyHeatmapOptions(C chart) {
      chart.showValues = showValues
      chart.valueDecimals = valueDecimals
      chart.labelColor = labelColor
      chart.lowColor = lowColor
      chart.midColor = midColor
      chart.highColor = highColor
      chart.midpoint = midpoint
    }

    @Override B x(String columnName) { throw unsupported('x') }
    @Override B y(String columnName) { throw unsupported('y') }
    @Override B y(String... columnNames) { throw unsupported('y') }
    @Override B xAxisTitle(String title) { throw unsupported('xAxisTitle') }
    @Override B yAxisTitle(String title) { throw unsupported('yAxisTitle') }
    @Override B xAxisScale(AxisScale scale) { throw unsupported('xAxisScale') }
    @Override B yAxisScale(AxisScale scale) { throw unsupported('yAxisScale') }
    @Override B xAxisScale(BigDecimal start, BigDecimal end, BigDecimal step) { throw unsupported('xAxisScale') }
    @Override B yAxisScale(BigDecimal start, BigDecimal end, BigDecimal step) { throw unsupported('yAxisScale') }
    @Override B yLabels(Map<String, String> labels) { throw unsupported('yLabels') }
    @Override B seriesColors(Color... colors) { throw unsupported('seriesColors') }
    @Override B seriesColors(List<Color> colors) { throw unsupported('seriesColors') }
    @Override B seriesColors(Map<String, Color> colors) { throw unsupported('seriesColors') }

    private IllegalArgumentException unsupported(String method) {
      new IllegalArgumentException("${this.class.enclosingClass.simpleName} does not support ${method}(...)")
    }
  }
}
