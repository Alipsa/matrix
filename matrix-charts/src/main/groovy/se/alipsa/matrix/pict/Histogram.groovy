package se.alipsa.matrix.pict

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

/** Histogram chart for visualizing the frequency distribution of numerical data. */
@SuppressWarnings('DuplicateNumberLiteral')
@SuppressWarnings('ExplicitCallToCompareToMethod')
@SuppressWarnings('Instanceof')
@SuppressWarnings('NestedForLoop')
class Histogram extends Chart<Histogram> {

  List<? extends Number> originalData = []
  Map<MinMax, Integer> ranges
  Integer numberOfBins = 9

  /**
   * Creates a histogram from named parameters.
   *
   * @param params named parameters: title, data, columnName, bins, binDecimals
   * @return histogram
   * @deprecated Use {@link #builder(Matrix)} with {@code title(...)}, {@code x(...)},
   * {@code bins(...)}, and {@code binDecimals(...)} for new code.
   */
  @Deprecated
  static Histogram create(Map params) {
    String title = params.title as String
    Matrix data = params.data as Matrix
    String columnName = params.columnName as String
    Integer bins = params.getOrDefault('bins', 9) as Integer
    int binDecimals = params.getOrDefault('binDecimals', 1) as int
    return create(title, data, columnName, bins, binDecimals)
  }

  /**
   * Creates a histogram from a matrix column.
   *
   * @param title chart title
   * @param data chart data
   * @param columnName numeric column name
   * @param bins number of bins
   * @param binDecimals number of decimals for bin boundaries
   * @return histogram
   * @deprecated Use {@link #builder(Matrix)} for new code.
   */
  @Deprecated
  static Histogram create(String title, Matrix data, String columnName, Integer bins = 9, int binDecimals = 1) {
    Histogram chart = new Histogram()
    chart.title = title
    chart.numberOfBins = bins
    if (Number.isAssignableFrom(data.type(columnName))) {
      chart.originalData = data.column(columnName) as List<? extends Number>
      chart.ranges = createRanges(chart.originalData, bins, binDecimals)
    } else {
      throw new IllegalArgumentException('Column must be numeric in a histogram (hint: you can Barplot a Frequency)')
    }
    return chart
  }

  /**
   * Creates a histogram from numeric values.
   *
   * @param column numeric values
   * @param bins number of bins
   * @return histogram
   * @deprecated Use {@link #builder(Matrix)} for new code.
   */
  @Deprecated
  static Histogram create(List<? extends Number> column, Integer bins = 9) {
    Histogram chart = new Histogram()
    chart.originalData = column
    chart.numberOfBins = bins
    chart.ranges = createRanges(column, bins)
    return chart
  }

  private static Map<MinMax, Integer> createRanges(List<? extends Number> column, int bins, int binDecimals = 1) {
    List<MinMax> ranges = []
    BigDecimal minValue = Stat.min(column) as BigDecimal
    BigDecimal maxValue = Stat.max(column) as BigDecimal
    BigDecimal chunk = (maxValue - minValue) / bins
    BigDecimal chunkMin = minValue
    BigDecimal chunkMax
    for (int i = 0; i < bins; i++) {
      chunkMax = chunkMin + chunk
      ranges.add(new MinMax(chunkMin, chunkMax, binDecimals))
      chunkMin = chunkMax
    }
    Map<MinMax, Integer> dist = [:]
    for (MinMax group in ranges) {
      dist.put(group, 0)
    }
    for (Number value in column) {
      BigDecimal bdValue = (value instanceof BigDecimal) ? value : new BigDecimal(value.toString())
      for (MinMax group in ranges) {
        if (bdValue.compareTo(group.maxValue) <= 0) {
          Integer num = dist[group]
          dist[group] = num + 1
          break
        }
      }
    }
    return dist
  }

  Map<MinMax, Integer> getRanges() {
    return ranges
  }

  List<? extends Number> getOriginalData() {
    return originalData
  }

  Integer getNumberOfBins() {
    return numberOfBins
  }

  /**
   * Creates a new fluent builder for constructing a {@link Histogram}.
   *
   * <p>Example:
   * <pre>
   * Histogram chart = Histogram.builder(data)
   *     .title('MPG Distribution')
   *     .x('mpg')
   *     .bins(5)
   *     .build()
   * </pre>
   *
   * @param data the Matrix containing chart data
   * @return a new Builder instance
   */
  static Builder builder(Matrix data) { new Builder(data) }

  /**
   * Fluent builder for {@link Histogram}.
   */
  static class Builder extends Chart.ChartBuilder<Builder, Histogram> {

    private Integer bins = 9
    private int binDecimals = 1

    Builder(Matrix data) { super(data) }

    /**
     * Sets the number of bins for the histogram.
     *
     * @param n the number of bins (default 9)
     * @return this builder
     */
    Builder bins(Integer n) { this.bins = n; this }

    /**
     * Sets the number of decimal places for bin boundaries.
     *
     * @param n the number of decimals (default 1)
     * @return this builder
     */
    Builder binDecimals(int n) { this.binDecimals = n; this }

    /**
     * Builds the configured {@link Histogram}.
     *
     * @return the histogram
     */
    Histogram build() {
      if (xCol == null) {
        throw new IllegalStateException('x(...) must be called before build()')
      }
      Histogram chart = Histogram.create(this.@title, data, xCol, bins, binDecimals)
      applyTo(chart)
      chart
    }

  }

}

class MinMax {

  BigDecimal minValue
  BigDecimal maxValue
  private final int binDecimals

  MinMax(BigDecimal minValue, BigDecimal maxValue, int binDecimals = 1) {
    this.minValue = minValue
    this.maxValue = maxValue
    this.binDecimals = binDecimals
  }

  @Override
  String toString() {
    return "${format(minValue)}-${format(maxValue)}"
  }

  private String format(BigDecimal value) {
    value.setScale(binDecimals, RoundingMode.HALF_EVEN).toString()
  }

}
