package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

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
class CutWidth {
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
    if (width == null || width.doubleValue() <= 0) {
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
  private List<String> computeBins(List<?> values) {
    double w = width.doubleValue()

    List<Double> numericValues = values.findAll { it instanceof Number }
        .collect { ((Number) it).doubleValue() }
    if (numericValues.isEmpty()) {
      return values.collect { null }
    }

    double minVal = numericValues.min()
    double maxVal = numericValues.max()

    // Calculate the starting boundary
    // ggplot2's default: boundary = width/2, which centers bins on multiples of width
    if (boundary != null && center != null) {
      throw new IllegalArgumentException("Only one of boundary and center may be specified")
    }

    double boundaryValue
    if (boundary == null) {
      boundaryValue = center == null ? (w / 2) : (center.doubleValue() - w / 2)
    } else {
      boundaryValue = boundary.doubleValue()
    }

    double minX = boundaryValue + Math.floor((minVal - boundaryValue) / w) * w
    // Ensure maxX includes the bin boundary after maxVal
    double maxX = maxVal + w

    List<Double> breaks = []
    double current = minX
    final int maxBins = 10_000
    int guard = 0
    while (current <= maxX + (w * 1e-9d) && guard < maxBins) {
      breaks << current
      current += w
      guard++
    }
    if (guard >= maxBins && current <= maxX + (w * 1e-9d)) {
      throw new IllegalStateException(
          "Exceeded maximum number of bins (" + maxBins +
          ") while computing cut_width breaks. " +
          "This likely indicates a problem with the binning parameters: " +
          "width=" + w + ", minVal=" + minVal + ", maxVal=" + maxVal +
          ", boundary=" + boundary + ", center=" + center)
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

      double d = ((Number) v).doubleValue()
      // Find which bin this value belongs to using ggplot2's cut_width breaks.
      int binIndex = (int) Math.floor((d - minX) / w)
      if (closedRight) {
        double boundaryPoint = minX + binIndex * w
        double diff = d - boundaryPoint
        double scale = Math.max(1.0d, Math.max(Math.abs(d), Math.abs(boundaryPoint)))
        double epsilon = 1e-10d * scale
        if (Math.abs(diff) < epsilon && Math.abs(d - minX) >= epsilon) {
          binIndex -= 1
        }
      }
      binIndex = Math.max(0, Math.min(binIndex, breaks.size() - 2))
      double binStart = breaks[binIndex]
      double binEnd = breaks[binIndex + 1]

      boolean isFirst = binIndex == 0
      String label = formatBinLabel(binStart, binEnd, isFirst)
      labels.add(label)
    }

    return labels
  }

  /**
   * Format a bin label like "[1,2)" or "(1,2]" depending on closure.
   */
  private String formatBinLabel(double start, double end, boolean isFirst) {
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
  private static String formatNumber(double d) {
    if (d >= Long.MIN_VALUE && d <= Long.MAX_VALUE && d == Math.floor(d)) {
      return String.valueOf((long) d)
    }
    // Round to avoid floating point artifacts
    return String.format("%.2f", d).replaceAll(/\.?0+$/, '')
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
}
