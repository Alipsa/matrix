package se.alipsa.matrix.gg.scale

import se.alipsa.matrix.charm.Scale as CharmScale


/**
 * Size scale where area is proportional to the data.
 * Missing or invalid values map to naValue (BigDecimal, nullable) inherited from ScaleSizeContinuous.
 */
class ScaleSizeArea extends ScaleSizeContinuous {

  /**
   * Create a size-by-area scale with defaults.
   */
  ScaleSizeArea() {
    super()
  }

  /**
   * Create a size-by-area scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleSizeArea(Map params) {
    super(params)
    if (params.max_size != null) {
      range = [0.0G, params.max_size as BigDecimal]
    }
  }

  /**
   * Trains an area scale from zero so radius is proportional to the represented area.
   *
   * @param data input values
   */
  @Override
  void train(List data) {
    super.train(data)
    if (computedDomain.size() >= 2 && computedDomain[0] > 0) {
      computedDomain[0] = 0.0G
    }
  }

  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) {
      return naValue
    }

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (dMax == dMin) {
      BigDecimal midArea = (rMin * rMin + rMax * rMax) / 2
      return midArea.sqrt()
    }

    BigDecimal normalized = (v - dMin) / (dMax - dMin)
    BigDecimal areaMin = rMin * rMin
    BigDecimal areaMax = rMax * rMax
    BigDecimal area = areaMin + normalized * (areaMax - areaMin)
    return area.sqrt()
  }

  /** Converts this area-proportional size scale to Charm's sqrt transform. */
  CharmScale toCharmScale() {
    CharmScale scale = CharmScale.transform('sqrt')
    scale.params['range'] = [range[0], range[1]]
    scale.params['limits'] = [0, null]
    scale
  }
}
