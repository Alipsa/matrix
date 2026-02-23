package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmExpression
import se.alipsa.matrix.core.Matrix
import java.util.Locale

/**
 * Wrapper for binning continuous data into fixed-width intervals.
 * Similar to ggplot2's cut_width function.
 *
 * Creates a categorical variable from a continuous column by dividing it
 * into bins of the specified width. Useful for grouping continuous data
 * in boxplots, histograms, and other visualizations.
 *
 * Example usage:
 * <pre>
 * ggplot(mpg, aes('cty', 'hwy')) +
 *     geom_boxplot(aes(group: cut_width('displ', 1)))
 * </pre>
 */
@CompileStatic
class CutWidth implements CharmExpression {
  private static final String DEFAULT_PREFIX = 'cut_width'

  /** Column name containing the values to bin */
  final String column

  /** Width of each bin */
  final Number width

  /** Center point for bins (null means auto-detect based on data) */
  final Number center

  /** Boundary point for bins (alternative to center) */
  final Number boundary

  /** Whether bins are closed on the right (true) or left (false) */
  final boolean closedRight

  /**
   * Create a cut_width wrapper with default settings.
   *
   * @param column the column name containing continuous values
   * @param width the width of each bin
   */
  CutWidth(String column, Number width) {
    this(column, width, null, null, true)
  }

  /**
   * Create a cut_width wrapper with full configuration.
   *
   * @param column the column name containing continuous values
   * @param width the width of each bin
   * @param center center point for bins (mutually exclusive with boundary)
   * @param boundary boundary point for bins (mutually exclusive with center)
   * @param closedRight if true, bins are (a, b]; if false, bins are [a, b)
   */
  CutWidth(String column, Number width, Number center, Number boundary, boolean closedRight) {
    if (column == null || column.isBlank()) {
      throw new IllegalArgumentException("Column name cannot be null or empty")
    }
    if (width == null || width <= 0) {
      throw new IllegalArgumentException("Width must be a positive number")
    }
    this.column = column
    this.width = width
    this.center = center
    this.boundary = boundary
    this.closedRight = closedRight
  }

  /**
   * Add a binned column to the matrix based on the continuous values.
   *
   * @param data matrix to append the binned column to
   * @return the column name used in the matrix
   */
  String addToMatrix(Matrix data) {
    if (!data.columnNames().contains(column)) {
      throw new IllegalArgumentException("Column '${column}' not found in data")
    }

    List<?> values = data[column] as List<?>
    List<String> binLabels = computeBins(values)

    String colName = generateColumnName(data)
    data.addColumn(colName, binLabels)
    return colName
  }

  /**
   * Compute bin labels for each value in the list.
   */
  List<String> computeBins(List<?> values) {
    BigDecimal w = width as BigDecimal

    List<BigDecimal> numericValues = values.findAll { it instanceof Number }
        .collect { it as BigDecimal }
    if (numericValues.isEmpty()) {
      return values.collect { null }
    }

    BigDecimal minVal = numericValues.min()
    BigDecimal maxVal = numericValues.max()

    // Calculate the starting boundary
    // ggplot2's default: boundary = width/2, which offsets bin boundaries by half the bin width
    if (boundary != null && center != null) {
      throw new IllegalArgumentException("Only one of boundary and center may be specified")
    }

    BigDecimal boundaryValue
    if (boundary == null) {
      boundaryValue = center == null ? (w / 2) : (center - w / 2)
    } else {
      boundaryValue = boundary as BigDecimal
    }

    BigDecimal minX = boundaryValue + (((minVal - boundaryValue) / w).floor() * w)
    // Compute maxX as the first bin boundary at or after maxVal (avoids creating excess bins)
    BigDecimal maxX = ((maxVal - minX) / w).ceil() * w + minX
    // For closed-left bins [a,b), values equal to a break point need a bin starting there,
    // so we need one more break to close the final bin
    if (!closedRight) {
      maxX += w
    }

    List<BigDecimal> breaks = []
    BigDecimal current = minX
    final int maxBins = 10_000
    int binCount = 0
    while (current <= maxX + (w * 1e-9d) && binCount < maxBins) {
      breaks << current
      current += w
      binCount++
    }
    if (binCount >= maxBins && current <= maxX + (w * 1e-9d)) {
      throw new IllegalStateException(
          """Exceeded maximum number of bins (${maxBins}) while computing cut_width breaks. \
This likely indicates a problem with the binning parameters: \
width=${w}, minVal=${minVal}, maxVal=${maxVal}, boundary=${boundary}, center=${center}""")
    }
    if (breaks.size() < 2) {
      breaks << (minX + w)
    }

    // Compute bin label for each value
    List<String> labels = new ArrayList<>(values.size())
    for (def v : values) {
      if (v == null || !(v instanceof Number)) {
        labels.add(null)
        continue
      }

      BigDecimal d = v as BigDecimal
      // Find which bin this value belongs to using ggplot2's cut_width breaks.
      BigDecimal binIndex = ((d - minX) / w).floor()
      if (closedRight) {
        BigDecimal boundaryPoint = minX + binIndex * w
        BigDecimal diff = d - boundaryPoint
        BigDecimal epsilon = [boundaryPoint.ulp(), d.ulp()].max() * 10.0d
        boolean onBoundary = diff.abs() <= epsilon
        boolean atMinBoundary = (d - minX).abs() <= epsilon
        // For closed-right bins, values exactly on a boundary belong to the previous bin,
        // except for the minimum boundary which stays in the first bin.
        if (onBoundary && !atMinBoundary) {
          binIndex -= 1
        }
      }
      binIndex = 0.max(binIndex.min(breaks.size() - 2))
      BigDecimal binStart = breaks[binIndex]
      BigDecimal binEnd = breaks[binIndex + 1]

      boolean isFirst = binIndex == 0
      String label = formatBinLabel(binStart, binEnd, isFirst)
      labels.add(label)
    }

    return labels
  }

  /**
   * Format a bin label like "[1,2)" or "(1,2]" depending on closure.
   */
  private String formatBinLabel(Number start, Number end, boolean isFirst) {
    String startStr = formatNumber(start)
    String endStr = formatNumber(end)

    if (closedRight) {
      // Closed on right: (start, end], but include the lowest boundary in the first bin.
      if (isFirst) {
        return "[${startStr},${endStr}]"
      }
      return "(${startStr},${endStr}]"
    } else {
      // Closed on left: [start, end)
      return "[${startStr},${endStr})"
    }
  }

  /**
   * Format a number, removing unnecessary decimal places.
   */
  private static String formatNumber(Number n) {
    double d = n as double
    if (d >= Long.MIN_VALUE && d <= Long.MAX_VALUE && d == (d as BigDecimal).floor()) {
      return String.valueOf((long) d)
    }
    // Round to avoid floating point artifacts
    String s = String.format(Locale.US, "%.2f", d)
    if (s.contains('.')) {
      // Remove trailing zeros after the decimal point
      s = s.replaceAll(/0+$/, '')
      // If all decimals were zeros, remove the trailing decimal point
      s = s.replaceAll(/\.$/, '')
    }
    return s
  }

  /**
   * Generate a unique column name for the binned data.
   */
  private String generateColumnName(Matrix data) {
    String baseName = "${DEFAULT_PREFIX}_${column}"
    if (!data.columnNames().contains(baseName)) {
      return baseName
    }
    int suffix = 1
    while (data.columnNames().contains("${baseName}_${suffix}")) {
      suffix++
    }
    return "${baseName}_${suffix}"
  }

  @Override
  String toString() {
    return "cut_width('${column}', ${width})"
  }

  @Override
  String describe() {
    "cut_width(${column}, ${width})"
  }
}
