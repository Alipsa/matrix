package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.charm.util.ColorUtil

/**
 * Trained color scale for mapping data values to colors.
 *
 * Supports multiple color strategies:
 * <ul>
 *   <li>DEFAULT - 10-color categorical palette</li>
 *   <li>MANUAL - user-provided color list/map or auto-generated HCL hue palette</li>
 *   <li>BREWER - ColorBrewer palettes</li>
 *   <li>GRADIENT - two-color or diverging gradient</li>
 *   <li>GRADIENT_N - multi-stop gradient</li>
 *   <li>VIRIDIS_D - discrete viridis palette family</li>
 *   <li>IDENTITY - pass-through (data values are colors)</li>
 * </ul>
 */
@CompileStatic
class ColorCharmScale extends CharmScale {

  private static final List<String> DEFAULT_COLORS = [
      '#1f77b4', '#d62728', '#2ca02c', '#ff7f0e', '#9467bd',
      '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'
  ]

  /** Color type strategy. */
  String colorType = 'default'

  /** Ordered discrete levels (for discrete strategies). */
  List<String> levels = []

  /** Computed color map: level -> color. */
  Map<String, String> colorMap = [:]

  /** Domain min for continuous color scales (gradient). */
  BigDecimal domainMin

  /** Domain max for continuous color scales (gradient). */
  BigDecimal domainMax

  // Color config params
  /** For MANUAL: user-provided color list. */
  List<String> values = []
  /** For MANUAL: named value -> color mapping. */
  Map<Object, String> namedValues = [:]
  /** For GRADIENT: low color. */
  String low = '#132B43'
  /** For GRADIENT: high color. */
  String high = '#56B1F7'
  /** For GRADIENT: mid color (diverging). */
  String mid
  /** For GRADIENT: midpoint value. */
  Number midpoint
  /** For GRADIENT_N: multi-stop colors. */
  List<String> gradientColors = []
  /** For GRADIENT_N: color stop positions. */
  List<BigDecimal> gradientValues
  /** For BREWER: palette name. */
  String palette = 'Set1'
  /** For BREWER: direction. */
  int direction = 1
  /** For VIRIDIS: option. */
  String viridisOption = 'viridis'
  /** For VIRIDIS: begin. */
  BigDecimal viridisBegin = 0.0
  /** For VIRIDIS: end. */
  BigDecimal viridisEnd = 1.0
  /** For VIRIDIS: alpha. */
  BigDecimal viridisAlpha = 1.0
  /** NA value color. */
  String naValue = '#999999'

  /**
   * Returns the color for a data value.
   *
   * @param value data value
   * @return hex color string
   */
  String colorFor(Object value) {
    if (value == null) return naValue

    if (colorType == 'identity') {
      String colorValue = value.toString()
      String normalized = ColorUtil.normalizeColor(colorValue)
      return normalized ?: colorValue
    }
    if (colorType == 'gradient') {
      return gradientColor(value)
    }
    if (colorType == 'gradientN' || colorType == 'distiller') {
      return gradientNColor(value)
    }
    if (colorType == 'fermenter' || colorType == 'steps' || colorType == 'steps2' || colorType == 'stepsN') {
      return binnedGradientColor(value)
    }

    if (colorMap.isEmpty()) return naValue
    colorMap[value.toString()] ?: naValue
  }

  @Override
  BigDecimal transform(Object value) {
    // Color scales don't map to pixel coordinates
    null
  }

  @Override
  List<Object> ticks(int count) {
    new ArrayList<Object>(levels)
  }

  @Override
  List<String> tickLabels(int count) {
    new ArrayList<>(levels)
  }

  @Override
  boolean isDiscrete() {
    !(colorType in ['gradient', 'gradientN', 'distiller', 'fermenter', 'steps', 'steps2', 'stepsN'])
  }

  /**
   * Trains this color scale from data values and a Scale spec.
   *
   * @param dataValues collected data values for the color/fill aesthetic
   * @param spec user-facing Scale config
   * @return this scale (for chaining)
   */
  ColorCharmScale trainFromValues(List<Object> dataValues, Scale spec) {
    this.scaleSpec = spec

    String configuredType = spec?.params?.get('colorType') as String ?: 'default'
    String type = configuredType.toLowerCase(Locale.ROOT)

    switch (type) {
      case 'manual' -> {
        this.colorType = 'manual'
        trainManual(dataValues, spec)
      }
      case 'brewer' -> {
        this.colorType = 'brewer'
        trainBrewer(dataValues, spec)
      }
      case 'gradient' -> {
        this.colorType = 'gradient'
        trainGradient(dataValues, spec)
      }
      case 'gradientn' -> {
        this.colorType = 'gradientN'
        trainGradientN(dataValues, spec)
      }
      case 'viridis_d' -> {
        this.colorType = 'viridis_d'
        trainViridis(dataValues, spec)
      }
      case 'identity' -> {
        this.colorType = 'identity'
        trainIdentity(dataValues, spec)
      }
      case 'distiller' -> {
        this.colorType = 'distiller'
        trainDistiller(dataValues, spec)
      }
      case 'fermenter' -> {
        this.colorType = 'fermenter'
        trainFermenter(dataValues, spec)
      }
      case 'grey' -> {
        this.colorType = 'grey'
        trainGrey(dataValues, spec)
      }
      case 'hue' -> {
        this.colorType = 'hue'
        trainHue(dataValues, spec)
      }
      case 'steps' -> {
        this.colorType = 'steps'
        trainSteps(dataValues, spec)
      }
      case 'steps2' -> {
        this.colorType = 'steps2'
        trainSteps2(dataValues, spec)
      }
      case 'stepsn' -> {
        this.colorType = 'stepsN'
        trainStepsN(dataValues, spec)
      }
      default -> {
        this.colorType = 'default'
        trainDefault(dataValues)
      }
    }
    this
  }

  private void trainDefault(List<Object> dataValues) {
    collectLevels(dataValues)
    colorMap = [:]
    levels.eachWithIndex { String level, int idx ->
      colorMap[level] = DEFAULT_COLORS[idx % DEFAULT_COLORS.size()]
    }
  }

  private void trainManual(List<Object> dataValues, Scale spec) {
    collectLevels(dataValues)

    Object userValues = spec.params['values']
    Object userNamedValues = spec.params['namedValues']
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'

    if (userNamedValues instanceof Map) {
      this.namedValues = (userNamedValues as Map).collectEntries { k, v ->
        [k, v?.toString()]
      } as Map<Object, String>
    }

    if (userValues instanceof List) {
      this.values = (userValues as List).collect { it?.toString() }
    }

    colorMap = [:]
    if (!namedValues.isEmpty()) {
      levels.each { String level ->
        String color = namedValues[level] as String
        colorMap[level] = color ?: naValue
      }
    } else if (!values.isEmpty()) {
      levels.eachWithIndex { String level, int idx ->
        colorMap[level] = values[idx % values.size()]
      }
    } else {
      // Auto-generate HCL hue palette
      List<String> hueColors = generateHuePalette(levels.size())
      levels.eachWithIndex { String level, int idx ->
        colorMap[level] = hueColors[idx]
      }
    }
  }

  private void trainBrewer(List<Object> dataValues, Scale spec) {
    collectLevels(dataValues)
    this.palette = (spec.params['palette'] as String) ?: 'Set1'
    this.direction = spec.params['direction'] != null ? spec.params['direction'] as int : 1
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'

    List<String> colors = BrewerPalettes.selectPalette(palette, levels.size(), direction)
    colorMap = [:]
    if (colors.isEmpty()) {
      levels.eachWithIndex { String level, int idx ->
        colorMap[level] = DEFAULT_COLORS[idx % DEFAULT_COLORS.size()]
      }
    } else {
      levels.eachWithIndex { String level, int idx ->
        colorMap[level] = colors[idx % colors.size()]
      }
    }
  }

  private void trainGradient(List<Object> dataValues, Scale spec) {
    this.low = (spec.params['low'] as String) ?: '#132B43'
    this.high = (spec.params['high'] as String) ?: '#56B1F7'
    this.mid = spec.params['mid'] as String
    this.midpoint = spec.params['midpoint'] as Number
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'

    trainContinuousDomain(dataValues)
  }

  private void trainGradientN(List<Object> dataValues, Scale spec) {
    this.gradientColors = (spec.params['colors'] as List)?.collect { it?.toString() } ?: ['#132B43', '#56B1F7']
    this.gradientValues = spec.params['gradientValues'] as List<BigDecimal>
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'

    trainContinuousDomain(dataValues)
  }

  private void trainViridis(List<Object> dataValues, Scale spec) {
    collectLevels(dataValues)
    this.viridisOption = (spec.params['option'] as String) ?: 'viridis'
    this.viridisBegin = spec.params['begin'] != null ? (spec.params['begin'] as BigDecimal) : 0.0
    this.viridisEnd = spec.params['end'] != null ? (spec.params['end'] as BigDecimal) : 1.0
    this.direction = spec.params['direction'] != null ? spec.params['direction'] as int : 1
    this.viridisAlpha = spec.params['alpha'] != null ? (spec.params['alpha'] as BigDecimal) : 1.0
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'

    List<String> colors = ViridisProvider.getColors(
        levels.size(), viridisOption, viridisBegin, viridisEnd, direction, viridisAlpha)
    colorMap = [:]
    levels.eachWithIndex { String level, int idx ->
      colorMap[level] = colors[idx % colors.size()]
    }
  }

  private void trainIdentity(List<Object> dataValues, Scale spec) {
    collectLevels(dataValues)
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    // Identity scale: each level maps to itself as a color
    colorMap = [:]
    levels.each { String level ->
      String normalized = ColorUtil.normalizeColor(level)
      colorMap[level] = normalized ?: level
    }
  }

  private void trainDistiller(List<Object> dataValues, Scale spec) {
    this.palette = (spec.params['palette'] as String) ?: 'RdYlBu'
    this.direction = spec.params['direction'] != null ? spec.params['direction'] as int : -1
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    List<String> colors = BrewerPalettes.selectPalette(palette, 9, direction)
    this.gradientColors = colors.isEmpty() ? new ArrayList<>(DEFAULT_COLORS) : colors
    this.gradientValues = null
    trainContinuousDomain(dataValues)
  }

  private void trainFermenter(List<Object> dataValues, Scale spec) {
    this.palette = (spec.params['palette'] as String) ?: 'YlOrRd'
    this.direction = spec.params['direction'] != null ? spec.params['direction'] as int : 1
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    int breaks = ValueConverter.asBigDecimal(spec.params['nBreaks'])?.intValue() ?:
        ValueConverter.asBigDecimal(spec.params['n.breaks'])?.intValue() ?: 6
    if (breaks < 2) {
      breaks = 6
    }
    List<String> colors = BrewerPalettes.selectPalette(palette, breaks, direction)
    this.gradientColors = colors.isEmpty() ? (0..<breaks).collect { int i ->
      DEFAULT_COLORS[i % DEFAULT_COLORS.size()]
    } as List<String> : colors
    this.gradientValues = null
    trainContinuousDomain(dataValues)
  }

  private void trainGrey(List<Object> dataValues, Scale spec) {
    collectLevels(dataValues)
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    String start = (spec.params['start'] as String) ?: '#EBEBEB'
    String end = (spec.params['end'] as String) ?: '#4D4D4D'
    colorMap = [:]
    if (levels.isEmpty()) {
      return
    }
    if (levels.size() == 1) {
      colorMap[levels[0]] = start
      return
    }
    levels.eachWithIndex { String level, int idx ->
      BigDecimal t = idx / (levels.size() - 1)
      colorMap[level] = ColorScaleUtil.interpolateColor(start, end, t)
    }
  }

  private void trainHue(List<Object> dataValues, Scale spec) {
    collectLevels(dataValues)
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    BigDecimal start = ValueConverter.asBigDecimal(spec.params['hStart'] ?: spec.params['h.start']) ?: 15.0
    BigDecimal end = ValueConverter.asBigDecimal(spec.params['hEnd'] ?: spec.params['h.end']) ?: 375.0
    int dir = spec.params['direction'] != null ? spec.params['direction'] as int : 1
    List<String> colors = generateHuePalette(levels.size(), start, end)
    if (dir < 0) {
      colors = colors.reverse()
    }
    colorMap = [:]
    levels.eachWithIndex { String level, int idx ->
      colorMap[level] = colors[idx % colors.size()]
    }
  }

  private void trainSteps(List<Object> dataValues, Scale spec) {
    String stepLow = (spec.params['low'] as String) ?: '#132B43'
    String stepHigh = (spec.params['high'] as String) ?: '#56B1F7'
    int breaks = ValueConverter.asBigDecimal(spec.params['nBreaks'])?.intValue() ?:
        ValueConverter.asBigDecimal(spec.params['n.breaks'])?.intValue() ?: 6
    if (breaks < 2) {
      breaks = 6
    }
    gradientColors = []
    for (int i = 0; i < breaks; i++) {
      BigDecimal t = i / (breaks - 1)
      gradientColors << ColorScaleUtil.interpolateColor(stepLow, stepHigh, t)
    }
    gradientValues = null
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    trainContinuousDomain(dataValues)
  }

  private void trainSteps2(List<Object> dataValues, Scale spec) {
    String stepLow = (spec.params['low'] as String) ?: '#832424'
    String stepMid = (spec.params['mid'] as String) ?: '#FFFFFF'
    String stepHigh = (spec.params['high'] as String) ?: '#3B4CC0'
    int breaks = ValueConverter.asBigDecimal(spec.params['nBreaks'])?.intValue() ?:
        ValueConverter.asBigDecimal(spec.params['n.breaks'])?.intValue() ?: 7
    if (breaks < 3) {
      breaks = 7
    }
    gradientColors = []
    for (int i = 0; i < breaks; i++) {
      BigDecimal t = i / (breaks - 1)
      String color = t <= 0.5
          ? ColorScaleUtil.interpolateColor(stepLow, stepMid, t * 2)
          : ColorScaleUtil.interpolateColor(stepMid, stepHigh, (t - 0.5) * 2)
      gradientColors << color
    }
    gradientValues = null
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    trainContinuousDomain(dataValues)
  }

  private void trainStepsN(List<Object> dataValues, Scale spec) {
    this.gradientColors = (spec.params['colors'] as List)?.collect { it?.toString() } ?: ['#132B43', '#56B1F7']
    if (gradientColors.size() < 2) {
      gradientColors = ['#132B43', '#56B1F7']
    }
    this.gradientValues = null
    this.naValue = (spec.params['naValue'] as String) ?: '#999999'
    trainContinuousDomain(dataValues)
  }

  private void trainContinuousDomain(List<Object> dataValues) {
    List<BigDecimal> numeric = dataValues
        .collect { ValueConverter.asBigDecimal(it) }
        .findAll { it != null } as List<BigDecimal>
    if (numeric.isEmpty()) {
      domainMin = 0.0
      domainMax = 1.0
      return
    }
    domainMin = numeric.min()
    domainMax = numeric.max()
    if (domainMin == domainMax) {
      domainMax = domainMin + 1
    }
  }

  private void collectLevels(List<Object> dataValues) {
    LinkedHashSet<String> unique = new LinkedHashSet<>()
    dataValues.each { Object value ->
      if (value != null) {
        unique << value.toString()
      }
    }
    this.levels = new ArrayList<>(unique)
  }

  private String gradientColor(Object value) {
    if (!(value instanceof Number)) return naValue
    BigDecimal v = value as BigDecimal
    if (domainMax == domainMin) {
      return mid ?: ColorScaleUtil.interpolateColor(low, high, 0.5)
    }

    BigDecimal normalized = ((v - domainMin) / (domainMax - domainMin)).min(1.0).max(0.0)

    if (mid && midpoint != null) {
      BigDecimal midNorm = ((midpoint as BigDecimal) - domainMin) / (domainMax - domainMin)
      if (normalized < midNorm) {
        BigDecimal t = midNorm > 0 ? normalized / midNorm : 0.0
        return ColorScaleUtil.interpolateColor(low, mid, t)
      } else {
        BigDecimal t = midNorm < 1 ? (normalized - midNorm) / (1 - midNorm) : 1.0
        return ColorScaleUtil.interpolateColor(mid, high, t)
      }
    }
    ColorScaleUtil.interpolateColor(low, high, normalized)
  }

  private String gradientNColor(Object value) {
    if (!(value instanceof Number) || gradientColors == null || gradientColors.isEmpty()) return naValue
    BigDecimal v = value as BigDecimal
    if (domainMax == domainMin) {
      return gradientColors.size() == 1 ? gradientColors[0] : gradientColors[(gradientColors.size() / 2.0).floor() as int]
    }

    BigDecimal normalized = ((v - domainMin) / (domainMax - domainMin)).min(1.0).max(0.0)

    if (gradientColors.size() == 1) return gradientColors[0]

    List<BigDecimal> stops = resolveGradientStops()
    int idx = 0
    while (idx < stops.size() - 1 && normalized > stops[idx + 1]) {
      idx++
    }
    if (idx >= stops.size() - 1) return gradientColors[gradientColors.size() - 1]

    BigDecimal start = stops[idx]
    BigDecimal end = stops[idx + 1]
    BigDecimal localT = end > start ? (normalized - start) / (end - start) : 0.0
    ColorScaleUtil.interpolateColor(gradientColors[idx], gradientColors[idx + 1], localT)
  }

  private String binnedGradientColor(Object value) {
    if (!(value instanceof Number) || gradientColors == null || gradientColors.isEmpty()) return naValue
    BigDecimal v = value as BigDecimal
    if (domainMax == domainMin) {
      return gradientColors[gradientColors.size() - 1]
    }
    BigDecimal normalized = ((v - domainMin) / (domainMax - domainMin)).min(1.0).max(0.0)
    int idx = normalized * (gradientColors.size() - 1) as int
    if (idx < 0) {
      idx = 0
    } else if (idx > gradientColors.size() - 1) {
      idx = gradientColors.size() - 1
    }
    gradientColors[idx]
  }

  private List<BigDecimal> resolveGradientStops() {
    if (gradientValues != null && gradientValues.size() == gradientColors.size()) {
      return gradientValues.collect { Number n ->
        BigDecimal v = n != null ? (n as BigDecimal) : 0.0
        v.min(1.0).max(0.0)
      } as List<BigDecimal>
    }
    int n = gradientColors.size()
    List<BigDecimal> stops = new ArrayList<>(n)
    if (n == 1) {
      stops << 0.0
      return stops
    }
    BigDecimal step = 1.0 / (n - 1)
    for (int i = 0; i < n; i++) {
      stops << (i * step)
    }
    stops
  }

  /**
   * Generate a ggplot2-like hue palette in HCL space.
   *
   * @param n number of colors
   * @return list of hex RGB colors
   */
  private static List<String> generateHuePalette(int n) {
    generateHuePalette(n, 15.0, 375.0)
  }

  private static List<String> generateHuePalette(int n, BigDecimal start, BigDecimal end) {
    if (n <= 0) return []
    BigDecimal step = (end - start) / n
    List<String> colors = new ArrayList<>(n)
    for (int i = 0; i < n; i++) {
      BigDecimal hue = (start + step * i) % 360.0
      colors << ColorSpaceUtil.hclToHex(hue, 100, 65)
    }
    colors
  }
}
