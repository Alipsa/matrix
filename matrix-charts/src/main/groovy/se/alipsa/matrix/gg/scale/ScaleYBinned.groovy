package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned position scale for the y-axis.
 * Divides continuous data into equal-width bins and maps values to bin centers.
 * <p>
 * This scale is useful for creating histogram-like visualizations with discrete geoms
 * or for discretizing continuous position data. Values within the same bin are mapped
 * to the same position (the bin center), creating distinct groupings.
 * <p>
 * Example usage:
 * <pre>
 * def chart = ggplot(data, aes(x: 'category', y: 'value')) +
 *   geom_point() +
 *   scale_y_binned(bins: 10)
 * </pre>
 *
 * @see ScaleXBinned
 */
@CompileStatic
class ScaleYBinned extends ScaleYContinuous {

  /** Number of bins to divide the data range into */
  int bins = 10

  /** Bin boundaries computed from domain (bins+1 values) */
  private List<BigDecimal> binBoundaries = []

  /** Bin centers computed from boundaries (bins values) */
  private List<BigDecimal> binCenters = []

  /** Whether bins are closed on the right (true) or left (false) */
  boolean right = true

  /** Whether to show scale limits as axis breaks */
  boolean showLimits = false

  ScaleYBinned() {
    aesthetic = 'y'
    position = 'left'
  }

  ScaleYBinned(Map params) {
    aesthetic = 'y'
    position = 'left'
    applyParams(params)
  }

  private void applyParams(Map params) {
    // Handle standard scale parameters
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.expand) this.expand = params.expand as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.nBreaks) this.nBreaks = params.nBreaks as int
    if (params.guide) this.guide = params.guide

    // Handle binned-specific parameters
    if (params.bins != null) {
      this.bins = (params.bins as Number).intValue()
    }

    if (params.right != null) {
      this.right = params.right as boolean
    }

    if (params.showLimits != null || params['show.limits'] != null) {
      this.showLimits = (params.showLimits ?: params['show.limits']) as boolean
    }
  }

  /**
   * Train the scale by computing bin boundaries and centers from the data.
   * Bins are equal-width and span the computed domain (after expansion).
   */
  @Override
  void train(List data) {
    // Call parent to compute domain with expansion
    super.train(data)

    // Compute bin boundaries
    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]

    if (dMax == dMin) {
      // Constant domain: create single bin
      binBoundaries = [dMin, dMax]
      binCenters = [dMin]
      return
    }

    // Create equal-width bins
    BigDecimal binWidth = (dMax - dMin) / bins
    binBoundaries = []

    for (int i = 0; i <= bins; i++) {
      binBoundaries << (dMin + i * binWidth)
    }

    // Compute bin centers (midpoint of each bin)
    binCenters = []
    for (int i = 0; i < bins; i++) {
      BigDecimal center = (binBoundaries[i] + binBoundaries[i + 1]) / 2
      binCenters << center
    }
  }

  /**
   * Transform a data value to a position in the output range.
   * Values are mapped to their bin center, then transformed to pixel space.
   *
   * @param value the data value to transform
   * @return the position in the output range, or null if value is invalid/out-of-bounds
   */
  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return null

    // Apply coordinate transformation if present (BEFORE binning)
    if (coordTrans != null && coordTrans.hasYTransformation()) {
      v = coordTrans.transformY(v)
      if (v == null) return null
    }

    // Find which bin this value belongs to
    int binIndex = findBinIndex(v)
    if (binIndex < 0) return null  // Out of bounds

    // Get the bin center in data space
    BigDecimal binCenter = binCenters[binIndex]

    // Transform bin center from data space to pixel space
    return ScaleUtils.linearTransform(
      binCenter,
      computedDomain[0],
      computedDomain[1],
      range[0],
      range[1]
    )
  }

  /**
   * Find the bin index for a given value.
   *
   * @param value the value to find the bin for (must be non-null)
   * @return the bin index [0, bins-1], or -1 if value is out of bounds
   */
  private int findBinIndex(BigDecimal value) {
    if (value == null) return -1

    // Handle empty bins
    if (binBoundaries.isEmpty() || binCenters.isEmpty()) return -1

    // Handle out-of-bounds
    BigDecimal minBoundary = binBoundaries[0]
    BigDecimal maxBoundary = binBoundaries[binBoundaries.size() - 1]
    if (value < minBoundary || value > maxBoundary) {
      return -1
    }

    // Find the bin
    int actualBins = binCenters.size()
    for (int i = 0; i < actualBins; i++) {
      BigDecimal lower = binBoundaries[i]
      BigDecimal upper = binBoundaries[i + 1]

      if (right) {
        // Bins are (lower, upper] - closed on right
        if (value > lower && value <= upper) return i
      } else {
        // Bins are [lower, upper) - closed on left
        if (value >= lower && value < upper) return i
      }
    }

    // Edge case: minimum value with right=true
    if (right && value == binBoundaries[0]) return 0

    return -1
  }

  /**
   * Get computed breaks for the axis.
   * Returns bin boundaries (interior or all, depending on showLimits).
   *
   * @return list of break values
   */
  @Override
  List getComputedBreaks() {
    if (breaks) return breaks  // User-specified breaks take precedence

    List<BigDecimal> result = []

    if (showLimits) {
      // Include all boundaries (including limits)
      result.addAll(binBoundaries)
    } else {
      // Exclude first and last boundary (limits)
      if (binBoundaries.size() > 2) {
        result.addAll(binBoundaries[1..-2])
      }
    }

    return result
  }
}
