package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous color scale using multiple colors.
 */
@CompileStatic
class ScaleColorGradientN extends ScaleContinuous {

  /** Colors to interpolate between. */
  List<String> colors = ['#132B43', '#56B1F7']

  /** Optional positions for each color in 0-1 range. */
  List<Number> values

  /** Color for NA values. */
  String naValue = 'grey50'

  /** Guide type: 'colorbar' or 'legend'. */
  String guideType = 'colorbar'

  /**
   * Create a multi-color gradient scale with defaults.
   */
  ScaleColorGradientN() {
    aesthetic = 'color'
    expand = [0, 0] as List<Number>
  }

  /**
   * Create a multi-color gradient scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleColorGradientN(Map params) {
    aesthetic = 'color'
    expand = [0, 0] as List<Number>
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.colors) this.colors = params.colors as List<String>
    if (params.colours) this.colors = params.colours as List<String>
    if (params.values) this.values = params.values as List<Number>
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
    if (params.guide) this.guideType = params.guide as String
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (!(value instanceof Number)) return naValue
    if (colors == null || colors.isEmpty()) return naValue

    double v = (value as Number).doubleValue()
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    if (dMax == dMin) {
      return colors.size() == 1 ? colors[0] : colors[(int) Math.floor(colors.size() / 2.0d)]
    }

    double normalized = (v - dMin) / (dMax - dMin)
    normalized = Math.max(0.0d, Math.min(1.0d, normalized))

    if (colors.size() == 1) {
      return colors[0]
    }

    List<Double> stops = resolveStops()
    int idx = 0
    while (idx < stops.size() - 1 && normalized > stops[idx + 1]) {
      idx++
    }
    if (idx >= stops.size() - 1) {
      return colors[colors.size() - 1]
    }
    double start = stops[idx]
    double end = stops[idx + 1]
    double localT = end > start ? (normalized - start) / (end - start) : 0.0d
    return ColorScaleUtil.interpolateColor(colors[idx], colors[idx + 1], localT)
  }

  private List<Double> resolveStops() {
    if (values != null && values.size() == colors.size()) {
      return values.collect { Number n ->
        double v = n != null ? (n as Number).doubleValue() : 0.0d
        Math.max(0.0d, Math.min(1.0d, v))
      } as List<Double>
    }
    int n = colors.size()
    List<Double> stops = new ArrayList<>(n)
    if (n == 1) {
      stops << 0.0d
      return stops
    }
    for (int i = 0; i < n; i++) {
      stops << (i / (double) (n - 1)) as double
    }
    return stops
  }
}
