package se.alipsa.matrix.xchart.abstractions

/**
 * Shared implementation details for heatmap chart types.
 */
final class HeatmapSupport {

  /** Zero-length array used as the type token when converting heat cells to {@code Number[]}. */
  static final Number[] HEAT_ARRAY_TYPE = new Number[0]

  private HeatmapSupport() { }

  /**
   * Verify that heatmap axis labels match the grid dimensions. XChart silently skips cells whose
   * index is outside the label lists, so a mismatch must fail early.
   *
   * @param columnLabels labels for the X-axis (one per column)
   * @param rowLabels labels for the Y-axis (one per row)
   * @param nCols number of data columns
   * @param nRows number of data rows
   * @throws IllegalArgumentException if either label list is null or has the wrong size
   */
  static void validateLabels(List<?> columnLabels, List<?> rowLabels, int nCols, int nRows) {
    if (columnLabels == null || rowLabels == null || columnLabels.size() != nCols || rowLabels.size() != nRows) {
      throw new IllegalArgumentException("Heatmap has $nCols columns and $nRows rows but got ${columnLabels?.size()} column labels and ${rowLabels?.size()} row labels")
    }
  }
}
