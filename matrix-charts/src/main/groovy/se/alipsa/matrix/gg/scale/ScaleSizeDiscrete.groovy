package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete size scale.
 * Missing or invalid values map to naValue (BigDecimal, nullable).
 */
@CompileStatic
class ScaleSizeDiscrete extends ScaleDiscrete {

  /** Output range [min, max] for size values. */
  List<Number> range = [1.0, 6.0] as List<Number>

  /** Size value for NA/missing values (BigDecimal, nullable). */
  BigDecimal naValue = 3.0G

  private List<Number> computedValues = []

  /**
   * Create a discrete size scale with defaults.
   */
  ScaleSizeDiscrete() {
    aesthetic = 'size'
  }

  /**
   * Create a discrete size scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleSizeDiscrete(Map params) {
    aesthetic = 'size'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.range) this.range = params.range as List<Number>
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue != null) this.naValue = ScaleUtils.coerceToNumber(params.naValue)
  }

  @Override
  void train(List data) {
    super.train(data)
    computedValues = ScaleUtils.interpolateRange(levels.size(), range)
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (levels.isEmpty()) return naValue

    int index = levels.indexOf(value)
    if (index < 0) return naValue

    if (computedValues.isEmpty()) {
      computedValues = ScaleUtils.interpolateRange(levels.size(), range)
    }
    if (computedValues.isEmpty()) return naValue
    return computedValues[index % computedValues.size()]
  }
}
