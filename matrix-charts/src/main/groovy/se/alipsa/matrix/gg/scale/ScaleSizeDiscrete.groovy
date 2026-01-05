package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete size scale.
 */
@CompileStatic
class ScaleSizeDiscrete extends ScaleDiscrete {

  /** Output range [min, max] for size values. */
  List<Number> range = [1.0, 6.0] as List<Number>

  /** Size value for NA/missing values. */
  Number naValue = 3.0

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
    if (params.naValue != null) this.naValue = params.naValue as Number
  }

  @Override
  void train(List data) {
    super.train(data)
    computedValues = buildValues(levels.size())
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (levels.isEmpty()) return naValue

    int index = levels.indexOf(value)
    if (index < 0) return naValue

    if (computedValues.isEmpty()) {
      computedValues = buildValues(levels.size())
    }
    if (computedValues.isEmpty()) return naValue
    return computedValues[index % computedValues.size()]
  }

  private List<Number> buildValues(int n) {
    if (n <= 0) return []
    double rMin = range[0] as double
    double rMax = range[1] as double
    List<Number> values = []
    if (n == 1) {
      values << ((rMin + rMax) / 2.0d)
      return values
    }
    for (int i = 0; i < n; i++) {
      double t = i / (double) (n - 1)
      values << (rMin + t * (rMax - rMin))
    }
    return values
  }
}
