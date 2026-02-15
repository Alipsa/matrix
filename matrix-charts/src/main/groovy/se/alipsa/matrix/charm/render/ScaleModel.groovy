package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.ScaleType
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Trained axis scale model used by Charm renderer.
 */
@CompileStatic
class ScaleModel {

  private static final List<String> DEFAULT_COLORS = [
      '#1f77b4', '#d62728', '#2ca02c', '#ff7f0e', '#9467bd',
      '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'
  ]

  final boolean discrete
  final List<String> levels
  final BigDecimal domainMin
  final BigDecimal domainMax
  final BigDecimal rangeStart
  final BigDecimal rangeEnd
  final Scale scaleSpec
  final Map<String, String> colorMap

  /**
   * Creates a trained scale model.
   *
   * @param discrete whether this is discrete
   * @param levels discrete levels
   * @param domainMin numeric domain min
   * @param domainMax numeric domain max
   * @param rangeStart rendered range start
   * @param rangeEnd rendered range end
   * @param scaleSpec scale spec
   * @param colorMap optional color map
   */
  ScaleModel(
      boolean discrete,
      List<String> levels,
      BigDecimal domainMin,
      BigDecimal domainMax,
      BigDecimal rangeStart,
      BigDecimal rangeEnd,
      Scale scaleSpec,
      Map<String, String> colorMap = [:]
  ) {
    this.discrete = discrete
    this.levels = levels == null ? [] : new ArrayList<>(levels)
    this.domainMin = domainMin
    this.domainMax = domainMax
    this.rangeStart = rangeStart
    this.rangeEnd = rangeEnd
    this.scaleSpec = scaleSpec
    this.colorMap = colorMap == null ? [:] : new LinkedHashMap<>(colorMap)
  }

  /**
   * Trains a model from values and axis scale spec.
   *
   * @param values mapped values
   * @param spec scale spec
   * @param rangeStart range start
   * @param rangeEnd range end
   * @param forceDiscrete force discrete mode
   * @return trained model
   */
  static ScaleModel train(
      List<Object> values,
      Scale spec,
      Number rangeStart,
      Number rangeEnd,
      boolean forceDiscrete = false
  ) {
    Scale scaleSpec = spec ?: Scale.continuous()
    boolean discrete = resolveDiscrete(values, scaleSpec, forceDiscrete)
    BigDecimal rangeStartValue = toBigDecimal(rangeStart)
    BigDecimal rangeEndValue = toBigDecimal(rangeEnd)
    if (discrete) {
      LinkedHashSet<String> unique = new LinkedHashSet<>()
      values.each { Object value ->
        if (value != null) {
          unique << value.toString()
        }
      }
      List<String> levels = new ArrayList<>(unique)
      return new ScaleModel(true, levels, null, null, rangeStartValue, rangeEndValue, scaleSpec)
    }

    List<BigDecimal> numeric = values.collect { Object value ->
      NumberCoercionUtil.coerceToBigDecimal(value)
    }.findAll { BigDecimal value -> value != null } as List<BigDecimal>
    if (scaleSpec.transformStrategy != null) {
      numeric = numeric.collect { BigDecimal value ->
        scaleSpec.transformStrategy.apply(value)
      }.findAll { BigDecimal value -> value != null } as List<BigDecimal>
    }
    if (numeric.isEmpty()) {
      return new ScaleModel(false, [], 0.0, 1.0, rangeStartValue, rangeEndValue, scaleSpec)
    }
    BigDecimal min = numeric.min()
    BigDecimal max = numeric.max()
    if (min == max) {
      max = max + 1
    }
    new ScaleModel(false, [], min, max, rangeStartValue, rangeEndValue, scaleSpec)
  }

  /**
   * Trains a color mapping model.
   *
   * @param values mapped values
   * @param spec scale spec
   * @return color mapping model
   */
  static ScaleModel trainColor(List<Object> values, Scale spec) {
    ScaleModel model = train(values, spec, 0.0, 1.0, true)
    Map<String, String> colors = [:]
    model.levels.eachWithIndex { String level, int idx ->
      colors[level] = DEFAULT_COLORS[idx % DEFAULT_COLORS.size()]
    }
    new ScaleModel(true, model.levels, null, null, 0.0, 1.0, spec ?: Scale.discrete(), colors)
  }

  /**
   * Maps a value to rendered coordinate.
   *
   * @param value mapped value
   * @return rendered coordinate
   */
  BigDecimal transform(Object value) {
    if (value == null) {
      return null
    }
    if (discrete) {
      if (levels.isEmpty()) {
        return null
      }
      int idx = levels.indexOf(value.toString())
      if (idx < 0) {
        return null
      }
      BigDecimal step = (rangeEnd - rangeStart) / levels.size()
      return rangeStart + step * idx + step / 2
    }
    BigDecimal numeric = NumberCoercionUtil.coerceToBigDecimal(value)
    if (numeric == null) {
      return null
    }
    if (scaleSpec?.transformStrategy != null) {
      numeric = scaleSpec.transformStrategy.apply(numeric)
    }
    if (numeric == null) {
      return null
    }
    if (domainMax == domainMin) {
      return (rangeStart + rangeEnd) / 2
    }
    BigDecimal normalized = (numeric - domainMin) / (domainMax - domainMin)
    rangeStart + normalized * (rangeEnd - rangeStart)
  }

  /**
   * Returns legend color for a mapped value.
   *
   * @param value mapped value
   * @return color string
   */
  String colorFor(Object value) {
    if (value == null) {
      return '#999999'
    }
    if (colorMap.isEmpty()) {
      return '#1f77b4'
    }
    colorMap[value.toString()] ?: '#999999'
  }

  /**
   * Returns axis ticks for rendering.
   *
   * @param count preferred tick count
   * @return ticks in data space
   */
  List<Object> ticks(int count = 5) {
    if (discrete) {
      return new ArrayList<>(levels)
    }
    if (domainMin == null || domainMax == null) {
      return []
    }
    int n = count <= 1 ? 2 : count
    BigDecimal step = (domainMax - domainMin) / (n - 1)
    List<Object> ticks = []
    for (int i = 0; i < n; i++) {
      ticks << (domainMin + step * i)
    }
    ticks
  }

  private static boolean resolveDiscrete(List<Object> values, Scale spec, boolean forceDiscrete) {
    if (forceDiscrete) {
      return true
    }
    if (spec?.type == ScaleType.DISCRETE) {
      return true
    }
    if (spec?.type == ScaleType.CONTINUOUS || spec?.type == ScaleType.TRANSFORM || spec?.type == ScaleType.DATE) {
      return false
    }
    return !values.every { Object value ->
      value == null || NumberCoercionUtil.coerceToBigDecimal(value) != null
    }
  }

  private static BigDecimal toBigDecimal(Number value) {
    value == null ? 0.0 : (value as BigDecimal)
  }
}
