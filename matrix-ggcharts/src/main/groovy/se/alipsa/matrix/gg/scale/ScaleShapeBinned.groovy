package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned shape scale for continuous data.
 * Divides continuous data into discrete bins and maps each bin to a shape.
 * Missing or invalid values map to naValue.
 */
@CompileStatic
class ScaleShapeBinned extends ScaleContinuous {

  /** List of shapes to use for bins */
  List<String> shapes = [
      'circle', 'triangle', 'square', 'diamond', 'plus', 'x', 'cross'
  ]

  /** Number of bins */
  int bins = 5

  /** Shape for NA/missing values */
  String naValue = 'circle'

  /**
   * Create a binned shape scale with defaults.
   */
  ScaleShapeBinned() {
    aesthetic = 'shape'
    expand = ScaleContinuous.NO_EXPAND
  }

  /**
   * Create a binned shape scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleShapeBinned(Map params) {
    aesthetic = 'shape'
    expand = ScaleContinuous.NO_EXPAND
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.shapes) this.shapes = params.shapes as List<String>
    if (params.bins != null) this.bins = (params.bins as Number).intValue()
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
  }

  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return naValue

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]

    // When domain is constant, return first shape
    if (dMax == dMin) {
      return shapes.isEmpty() ? naValue : shapes[0]
    }

    // Normalize to [0, 1]
    BigDecimal normalized = (v - dMin) / (dMax - dMin)
    normalized = normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE)

    // Determine which bin this value falls into
    int binsCount = 1.max(bins) as int
    if (binsCount == 1) {
      return shapes.isEmpty() ? naValue : shapes[0]
    }

    // Calculate bin index
    BigDecimal scaled = normalized * binsCount
    int binIndex = scaled.floor() as int

    // Clamp to valid range [0, binsCount-1]
    binIndex = binIndex.min(binsCount - 1) as int
    binIndex = binIndex.max(0) as int

    // Map bin to shape (cycle through shapes if more bins than shapes)
    if (shapes.isEmpty()) return naValue
    return shapes[binIndex % shapes.size()]
  }
}
