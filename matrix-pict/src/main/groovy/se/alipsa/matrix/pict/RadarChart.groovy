package se.alipsa.matrix.pict

import se.alipsa.matrix.core.Matrix

/** A radar chart with one polygon per source-matrix row. */
@SuppressWarnings(['DuplicateStringLiteral', 'UnnecessaryCollectCall', 'UnnecessaryObjectReferences'])
class RadarChart extends Chart<RadarChart> {

  /** Minimum number of polygon spokes. */
  static final int MIN_AXES = 3
  /** Default polygon fill opacity. */
  static final BigDecimal DEFAULT_FILL_ALPHA = 0.4
  /** Default number of radial rings. */
  static final int DEFAULT_RINGS = 4

  /** Spoke labels in clockwise order. */
  List<String> axisLabels = []
  /** Polygon labels, one per matrix row. */
  List<String> seriesLabels = []
  /** Values in series-major order. */
  List<List<BigDecimal>> seriesValues = []
  /** Whether source columns were min-max normalized. */
  boolean normalized = false
  /** Polygon fill opacity in [0, 1]; the outline keeps full opacity. */
  BigDecimal fillAlpha = DEFAULT_FILL_ALPHA

  /**
   * Creates a radar chart.
   *
   * @param title chart title
   * @param data source matrix
   * @param labelColumn series-label column
   * @param valueColumns numeric spoke columns
   * @param normalize whether to normalize columns to [0, 1]
   * @return populated radar chart
   */
  static RadarChart create(String title, Matrix data, String labelColumn, List<String> valueColumns, boolean normalize = false) {
    RadarChart chart = new RadarChart()
    populate(chart, title, data, labelColumn, valueColumns, normalize)
    chart
  }

  /** Returns the largest plotted value, with one as a safe non-zero minimum. */
  BigDecimal maxValue() {
    BigDecimal max = seriesValues.collect { List<BigDecimal> values -> values.max() }.max()
    max == null || max <= 0 ? 1 : max
  }

  /** Returns the outer radial value. */
  BigDecimal outerRadius() {
    validateRadialScale()
    yAxisScale != null ? yAxisScale.end : maxValue()
  }

  /** Returns the grid-ring values. */
  List<BigDecimal> ringValues() {
    BigDecimal outer = outerRadius()
    if (yAxisScale == null) {
      return (1..DEFAULT_RINGS).collect { int ring -> outer * ring / DEFAULT_RINGS }
    }
    List<BigDecimal> rings = []
    BigDecimal current = yAxisScale.step
    while (current < outer) {
      rings << current
      current += yAxisScale.step
    }
    rings << outer
    rings
  }

  /** Validates a fixed radial scale against the data. */
  void validateRadialScale() {
    if (yAxisScale == null) {
      return
    }
    if (yAxisScale.start != 0) {
      throw new IllegalArgumentException("Radar yAxisScale start must be 0, got ${yAxisScale.start}")
    }
    BigDecimal max = maxValue()
    if (max > yAxisScale.end) {
      throw new IllegalArgumentException("Radar value ${max} exceeds yAxisScale end ${yAxisScale.end}")
    }
  }

  private static void populate(RadarChart chart, String title, Matrix data, String labelColumn, List<String> valueColumns, boolean normalize) {
    if (valueColumns == null || valueColumns.size() < MIN_AXES) {
      throw new IllegalArgumentException("Radar chart requires at least ${MIN_AXES} value columns, got ${valueColumns?.size() ?: 0}")
    }
    if (data.rowCount() == 0) {
      throw new IllegalArgumentException("Radar data ${data.matrixName ?: ''} has no rows".trim())
    }
    requireColumn(data, labelColumn)
    List<List<BigDecimal>> columns = valueColumns.collect { String column -> completeNumericColumn(data, column) }
    if (normalize) {
      columns = columns.collect { List<BigDecimal> column -> normalizeColumn(column) }
    } else {
      columns.eachWithIndex { List<BigDecimal> column, int i ->
        if (column.any { BigDecimal value -> value < 0 }) {
          throw new IllegalArgumentException("Column '${valueColumns[i]}' contains negative values; radar values must be >= 0 (use normalize(true) for signed data)")
        }
      }
    }
    int rowCount = data.rowCount()
    chart.title = title
    chart.axisLabels = new ArrayList<String>(valueColumns)
    chart.seriesLabels = data.column(labelColumn).collect { Object label -> String.valueOf(label) }
    chart.seriesValues = (0..<rowCount).collect { int row -> columns.collect { List<BigDecimal> column -> column[row] } }
    chart.normalized = normalize
    chart.categorySeries = chart.axisLabels
    chart.valueSeries = chart.seriesValues as List<List<?>>
    chart.valueSeriesNames = chart.seriesLabels
  }

  private static List<BigDecimal> normalizeColumn(List<BigDecimal> column) {
    BigDecimal min = column.min()
    BigDecimal max = column.max()
    BigDecimal range = max - min
    column.collect { BigDecimal value -> range == 0 ? 0.0 : (value - min) / range }
  }

  /**
   * Creates a radar-chart builder.
   *
   * @param data source matrix
   * @return a builder
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /** Fluent builder for {@link RadarChart}. */
  static class Builder extends Chart.ChartBuilder<Builder, RadarChart> {

    private String labelColumn
    private List<String> valueColumns
    private boolean normalize = false
    private BigDecimal fillAlpha = DEFAULT_FILL_ALPHA

    Builder(Matrix data) { super(data) }

    /** Sets the series label column. */
    Builder label(String columnName) { labelColumn = columnName; this }
    /** Sets the spoke columns. */
    Builder values(String... columnNames) { valueColumns = columnNames == null ? null : columnNames.toList(); this }
    /** Sets the spoke columns. */
    Builder values(List<String> columnNames) { valueColumns = columnNames == null ? null : new ArrayList<String>(columnNames); this }
    /** Enables or disables column normalization. */
    Builder normalize(boolean enabled) { normalize = enabled; this }

    /**
     * Sets the polygon fill opacity. The polygon outline is unaffected.
     *
     * @param alpha opacity between 0 and 1 (default 0.4)
     * @return this builder
     */
    Builder fillAlpha(Number alpha) {
      BigDecimal value = alpha as BigDecimal
      if (value == null || value < 0 || value > 1) {
        throw new IllegalArgumentException("fillAlpha must be between 0 and 1, got ${alpha}")
      }
      fillAlpha = value
      this
    }

    @Override Builder x(String columnName) { throw unsupported('x') }
    @Override Builder y(String columnName) { throw unsupported('y') }
    @Override Builder y(String... columnNames) { throw unsupported('y') }
    @Override Builder xAxisTitle(String title) { throw unsupported('xAxisTitle') }
    @Override Builder yAxisTitle(String title) { throw unsupported('yAxisTitle') }
    @Override Builder xAxisScale(AxisScale scale) { throw unsupported('xAxisScale') }
    @Override Builder xAxisScale(BigDecimal start, BigDecimal end, BigDecimal step) { throw unsupported('xAxisScale') }
    @Override Builder yLabels(Map<String, String> labels) { throw unsupported('yLabels') }

    private static IllegalArgumentException unsupported(String method) {
      new IllegalArgumentException("RadarChart does not support ${method}(...)")
    }

    /** Builds the configured radar chart. */
    @Override
    RadarChart build() {
      if (labelColumn == null) {
        throw new IllegalStateException('label(...) must be called before build()')
      }
      if (valueColumns == null) {
        throw new IllegalStateException('values(...) must be called before build()')
      }
      RadarChart chart = new RadarChart()
      RadarChart.populate(chart, this.@title, data, labelColumn, valueColumns, normalize)
      applyTo(chart)
      chart.fillAlpha = fillAlpha
      chart.validateRadialScale()
      chart
    }
  }
}
