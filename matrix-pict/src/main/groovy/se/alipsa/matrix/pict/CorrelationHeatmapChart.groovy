package se.alipsa.matrix.pict

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Correlation

import java.awt.Color
import java.math.RoundingMode

/** A heatmap of pairwise correlations between selected complete numeric columns. */
@SuppressWarnings(['DuplicateNumberLiteral', 'UnnecessaryObjectReferences'])
class CorrelationHeatmapChart extends HeatmapChart {

  /** Default colour for -1. */
  static final Color DEFAULT_LOW = new Color(0xB2, 0x18, 0x2B)
  /** Default colour for zero. */
  static final Color DEFAULT_MID = new Color(0xF7, 0xF7, 0xF7)
  /** Default colour for +1. */
  static final Color DEFAULT_HIGH = new Color(0x21, 0x66, 0xAC)
  /** Supported correlation methods. */
  static final List<String> METHODS = [Correlation.PEARSON, Correlation.SPEARMAN, Correlation.KENDALL].asImmutable()

  private static final int SCALE = 2
  private static final List<BigDecimal> CORRELATION_LIMITS = [-1.00, 1.00].asImmutable()

  /** The selected correlation method. */
  String method = Correlation.PEARSON

  CorrelationHeatmapChart() {
    lowColor = DEFAULT_LOW
    midColor = DEFAULT_MID
    highColor = DEFAULT_HIGH
    midpoint = 0
    fillLimits = CORRELATION_LIMITS
  }

  /**
   * Creates a correlation heatmap.
   *
   * @param title chart title
   * @param data source matrix
   * @param columns columns to correlate
   * @param method correlation method
   * @return a populated correlation heatmap
   */
  static CorrelationHeatmapChart create(String title, Matrix data, List<String> columns, String method = Correlation.PEARSON) {
    CorrelationHeatmapChart chart = new CorrelationHeatmapChart()
    populateCorrelation(chart, title, data, columns, method)
    chart
  }

  private static void populateCorrelation(CorrelationHeatmapChart chart, String title, Matrix data, List<String> columns, String method) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException('Correlation heatmap requires at least one column')
    }
    if (!METHODS.contains(method)) {
      throw new IllegalArgumentException("Unknown correlation method '${method}', expected one of ${METHODS}")
    }
    List<List<BigDecimal>> series = columns.collect { String column -> completeNumericColumn(data, column) }
    chart.title = title
    chart.method = method
    chart.columnLabels = new ArrayList<String>(columns)
    chart.rowLabels = new ArrayList<String>(columns)
    chart.values = correlationMatrix(series, method)
    chart.categorySeries = chart.rowLabels
    chart.valueSeries = chart.values as List<List<?>>
    chart.valueSeriesNames = chart.columnLabels
  }

  private static List<List<BigDecimal>> correlationMatrix(List<List<BigDecimal>> series, String method) {
    int n = series.size()
    List<List<BigDecimal>> corr = []
    (0..<n).each { int ignored -> corr << new ArrayList<BigDecimal>(Collections.nCopies(n, (BigDecimal) null)) }
    for (int c = 0; c < n; c++) {
      corr[c][c] = roundCorrelation(Correlation.cor(series[c], series[c], method))
      for (int r = c + 1; r < n; r++) {
        BigDecimal value = Correlation.cor(series[c], series[r], method)
        BigDecimal rounded = roundCorrelation(value)
        corr[c][r] = rounded
        corr[r][c] = rounded
      }
    }
    corr
  }

  private static BigDecimal roundCorrelation(BigDecimal value) {
    value?.setScale(SCALE, RoundingMode.HALF_UP)
  }

  /**
   * Creates a builder for a correlation heatmap.
   *
   * @param data source matrix
   * @return a builder
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /** Fluent builder for {@link CorrelationHeatmapChart}. */
  static class Builder extends HeatmapChart.HeatmapBuilder<Builder, CorrelationHeatmapChart> {

    private String method = Correlation.PEARSON

    Builder(Matrix data) { super(data) }

    /** Sets the correlation method. */
    Builder method(String method) {
      if (!METHODS.contains(method)) {
        throw new IllegalArgumentException("Unknown correlation method '${method}', expected one of ${METHODS}")
      }
      this.method = method
      this
    }

    /** Correlation axes always use the selected columns. */
    @Override
    Builder rowLabels(String columnName) {
      throw new IllegalArgumentException('CorrelationHeatmapChart does not support rowLabels(...)')
    }

    /** Builds the correlation heatmap. */
    @Override
    CorrelationHeatmapChart build() {
      if (selectedColumns == null) {
        throw new IllegalStateException('columns(...) must be called before build()')
      }
      CorrelationHeatmapChart chart = new CorrelationHeatmapChart()
      CorrelationHeatmapChart.populateCorrelation(chart, this.@title, data, selectedColumns, this.@method)
      applyTo(chart)
      chart.showValues = this.@showValues
      chart.valueDecimals = this.@valueDecimals
      if (this.@lowColor != null || this.@midColor != null || this.@highColor != null) {
        chart.lowColor = this.@lowColor ?: DEFAULT_LOW
        chart.midColor = this.@midColor
        chart.highColor = this.@highColor ?: DEFAULT_HIGH
        chart.midpoint = this.@midpoint
      }
      chart
    }
  }
}
