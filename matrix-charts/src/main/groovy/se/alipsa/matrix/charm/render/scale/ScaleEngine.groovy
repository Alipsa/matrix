package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.ScaleType
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.charm.render.RenderConfig

/**
 * Orchestrates scale training for all aesthetic channels.
 *
 * Takes a compiled {@link Chart}, {@link RenderConfig}, and collected layer data,
 * then produces trained {@link CharmScale} instances for each aesthetic.
 */
@CompileStatic
class ScaleEngine {

  /**
   * Trains scales for all aesthetic channels.
   *
   * @param chart compiled chart specification
   * @param config render configuration
   * @param xValues collected x values from all layers
   * @param yValues collected y values from all layers
   * @param colorValues collected color values from all layers
   * @param fillValues collected fill values from all layers
   * @param sizeValues collected size values from all layers
   * @param shapeValues collected shape values from all layers
   * @param alphaValues collected alpha values from all layers
   * @return trained scales container
   */
  static TrainedScales train(Chart chart, RenderConfig config,
                              List<Object> xValues, List<Object> yValues,
                              List<Object> colorValues, List<Object> fillValues,
                              List<Object> sizeValues = [],
                              List<Object> shapeValues = [],
                              List<Object> alphaValues = [],
                              List<Object> linetypeValues = []) {
    TrainedScales trained = new TrainedScales()

    trained.x = trainPositionalScale(xValues, chart.scale.x, 0, config.plotWidth())
    trained.y = trainPositionalScale(yValues, chart.scale.y, config.plotHeight(), 0)
    applyFixedCoordScaling(chart, config, trained)

    boolean hasColor = colorValues.any { it != null }
    if (hasColor) {
      trained.color = trainColorScale(colorValues, chart.scale.color)
    }

    boolean hasFill = fillValues.any { it != null }
    if (hasFill) {
      trained.fill = trainColorScale(fillValues, chart.scale.fill)
    }

    boolean hasSize = sizeValues.any { it != null }
    if (hasSize) {
      trained.size = trainSizeScale(sizeValues, chart.scale.size)
    }

    boolean hasShape = shapeValues.any { it != null }
    if (hasShape) {
      trained.shape = trainShapeScale(shapeValues, chart.scale.shape)
    }

    boolean hasAlpha = alphaValues.any { it != null }
    if (hasAlpha) {
      trained.alpha = trainAlphaScale(alphaValues, chart.scale.alpha)
    }

    boolean hasLinetype = linetypeValues.any { it != null }
    if (hasLinetype) {
      trained.linetype = trainLinetypeScale(linetypeValues, chart.scale.linetype)
    }

    trained
  }

  private static void applyFixedCoordScaling(Chart chart, RenderConfig config, TrainedScales trained) {
    if (chart?.coord?.type != CharmCoordType.FIXED) {
      return
    }
    if (!(trained.x instanceof ContinuousCharmScale) || !(trained.y instanceof ContinuousCharmScale)) {
      return
    }

    ContinuousCharmScale xScale = trained.x as ContinuousCharmScale
    ContinuousCharmScale yScale = trained.y as ContinuousCharmScale

    List<Number> xlim = chart.coord?.xlim
    if (xlim != null && xlim.size() >= 2) {
      BigDecimal xMin = ValueConverter.asBigDecimal(xlim[0])
      BigDecimal xMax = ValueConverter.asBigDecimal(xlim[1])
      if (xMin != null && xMax != null && xMax > xMin) {
        xScale.domainMin = xMin
        xScale.domainMax = xMax
      }
    }
    List<Number> ylim = chart.coord?.ylim
    if (ylim != null && ylim.size() >= 2) {
      BigDecimal yMin = ValueConverter.asBigDecimal(ylim[0])
      BigDecimal yMax = ValueConverter.asBigDecimal(ylim[1])
      if (yMin != null && yMax != null && yMax > yMin) {
        yScale.domainMin = yMin
        yScale.domainMax = yMax
      }
    }

    BigDecimal xDomain = (xScale.domainMax - xScale.domainMin).abs()
    BigDecimal yDomain = (yScale.domainMax - yScale.domainMin).abs()
    if (xDomain <= 0 || yDomain <= 0) {
      return
    }

    BigDecimal ratio = ValueConverter.asBigDecimal(chart.coord?.ratio) ?: 1.0
    if (ratio <= 0) {
      ratio = 1.0
    }
    BigDecimal plotWidth = config.plotWidth()
    BigDecimal plotHeight = config.plotHeight()
    BigDecimal targetWidth = (plotHeight * xDomain) / (ratio * yDomain)

    if (targetWidth < plotWidth) {
      BigDecimal offset = (plotWidth - targetWidth) / 2
      xScale.rangeStart = offset
      xScale.rangeEnd = offset + targetWidth
    } else {
      xScale.rangeStart = 0
      xScale.rangeEnd = plotWidth
    }
  }

  /**
   * Trains a positional (x or y) scale.
   *
   * @param values data values
   * @param spec user-facing scale config
   * @param rangeStart output range start (pixels)
   * @param rangeEnd output range end (pixels)
   * @return trained CharmScale
   */
  static CharmScale trainPositionalScale(List<Object> values, Scale spec,
                                          Number rangeStart, Number rangeEnd) {
    Scale scaleSpec = spec ?: Scale.continuous()
    if (scaleSpec.type == ScaleType.BINNED) {
      return trainBinnedScale(values, scaleSpec, rangeStart as BigDecimal, rangeEnd as BigDecimal)
    }
    boolean discrete = resolveDiscrete(values, spec)
    BigDecimal rStart = rangeStart as BigDecimal
    BigDecimal rEnd = rangeEnd as BigDecimal

    if (discrete) {
      return trainDiscreteScale(values, scaleSpec, rStart, rEnd)
    }
    trainContinuousScale(values, scaleSpec, rStart, rEnd)
  }

  /**
   * Trains a color scale from data values and spec.
   *
   * @param values data values
   * @param spec user-facing scale config
   * @return trained ColorCharmScale
   */
  static ColorCharmScale trainColorScale(List<Object> values, Scale spec) {
    Scale effectiveSpec = spec
    if (effectiveSpec == null) {
      boolean numericValues = values.findAll { it != null }
          .every { ValueConverter.asBigDecimal(it) != null }
      effectiveSpec = numericValues ? Scale.gradient() : Scale.discrete()
    }
    ColorCharmScale colorScale = new ColorCharmScale(
        rangeStart: 0.0,
        rangeEnd: 1.0
    )
    colorScale.trainFromValues(values, effectiveSpec)
    colorScale
  }

  private static ContinuousCharmScale trainContinuousScale(
      List<Object> values, Scale scaleSpec,
      BigDecimal rangeStart, BigDecimal rangeEnd) {

    List<BigDecimal> numeric = values
        .collect { ValueConverter.asBigDecimal(it) }
        .findAll { it != null } as List<BigDecimal>

    // Apply transform to find domain in transformed space
    if (scaleSpec.transformStrategy != null) {
      numeric = numeric
          .collect { scaleSpec.transformStrategy.apply(it) }
          .findAll { it != null } as List<BigDecimal>
    }

    BigDecimal min, max
    if (numeric.isEmpty()) {
      min = 0.0
      max = 1.0
    } else {
      min = numeric.min()
      max = numeric.max()
      if (min == max) max = max + 1
    }

    // Apply limits from params
    List<Number> limits = scaleSpec.params['limits'] as List<Number>
    if (limits != null && limits.size() >= 2) {
      if (limits[0] != null) min = limits[0] as BigDecimal
      if (limits[1] != null) max = limits[1] as BigDecimal
    }

    // Apply expansion from params
    List<Number> expand = scaleSpec.params['expand'] as List<Number>
    if (expand != null && expand.size() >= 2) {
      BigDecimal mult = expand[0] != null ? expand[0] as BigDecimal : 0.05
      BigDecimal add = expand[1] != null ? expand[1] as BigDecimal : 0
      BigDecimal delta = max - min
      min = min - delta * mult - add
      max = max + delta * mult + add
    }

    new ContinuousCharmScale(
        scaleSpec: scaleSpec,
        rangeStart: rangeStart,
        rangeEnd: rangeEnd,
        domainMin: min,
        domainMax: max,
        expand: expand,
        limits: limits,
        transformStrategy: scaleSpec.transformStrategy
    )
  }

  private static DiscreteCharmScale trainDiscreteScale(
      List<Object> values, Scale scaleSpec,
      BigDecimal rangeStart, BigDecimal rangeEnd) {

    LinkedHashSet<String> unique = new LinkedHashSet<>()
    values.each { Object value ->
      if (value != null) {
        unique << value.toString()
      }
    }

    new DiscreteCharmScale(
        scaleSpec: scaleSpec,
        rangeStart: rangeStart,
        rangeEnd: rangeEnd,
        levels: new ArrayList<>(unique)
    )
  }

  private static BinnedCharmScale trainBinnedScale(
      List<Object> values, Scale scaleSpec,
      BigDecimal rangeStart, BigDecimal rangeEnd) {

    List<BigDecimal> numeric = coerceToNumeric(values)
    if (numeric.isEmpty()) {
      return new BinnedCharmScale(
          scaleSpec: scaleSpec,
          rangeStart: rangeStart,
          rangeEnd: rangeEnd,
          domainMin: 0.0,
          domainMax: 1.0,
          binBoundaries: [0.0, 1.0],
          binCenters: [0.5]
      )
    }

    BigDecimal min = numeric.min()
    BigDecimal max = numeric.max()
    if (min == max) {
      max = max + 1
    }

    int bins = ValueConverter.asBigDecimal(scaleSpec.params?.bins)?.intValue() ?: 30
    if (bins < 1) {
      bins = 30
    }
    BigDecimal binwidth = ValueConverter.asBigDecimal(scaleSpec.params?.binwidth)
    if (binwidth == null || binwidth <= 0) {
      binwidth = (max - min) / bins
    } else {
      bins = ((max - min) / binwidth).ceil().intValue()
      bins = bins < 1 ? 1 : bins
      max = min + binwidth * bins
    }

    List<BigDecimal> boundaries = []
    for (int i = 0; i <= bins; i++) {
      boundaries << (min + i * binwidth)
    }
    List<BigDecimal> centers = []
    for (int i = 0; i < bins; i++) {
      centers << (boundaries[i] + boundaries[i + 1]) / 2
    }

    new BinnedCharmScale(
        scaleSpec: scaleSpec,
        rangeStart: rangeStart,
        rangeEnd: rangeEnd,
        domainMin: min,
        domainMax: max,
        binBoundaries: boundaries,
        binCenters: centers
    )
  }

  /**
   * Trains a size scale mapping data values to pixel sizes (range 2-10).
   *
   * @param values data values for size aesthetic
   * @return trained continuous scale for size, or null if no numeric values
   */
  private static CharmScale trainSizeScale(List<Object> values, Scale spec) {
    Scale scaleSpec = spec ?: Scale.continuous()
    List<Number> range = scaleSpec.params?.range as List<Number>
    BigDecimal rangeStart = ValueConverter.asBigDecimal(range != null && range.size() > 0 ? range[0] : null) ?: 2.0
    BigDecimal rangeEnd = ValueConverter.asBigDecimal(range != null && range.size() > 1 ? range[1] : null) ?: 10.0
    if (scaleSpec.type == ScaleType.DISCRETE) {
      return trainDiscreteScale(values, scaleSpec, rangeStart, rangeEnd)
    }
    if (scaleSpec.type == ScaleType.BINNED) {
      return trainBinnedScale(values, scaleSpec, rangeStart, rangeEnd)
    }

    List<BigDecimal> numeric = coerceToNumeric(values)
    if (numeric.isEmpty()) {
      return null
    }

    BigDecimal min = numeric.min()
    BigDecimal max = ensureDomainRange(numeric.max(), min)
    if (scaleSpec.params?.identity == true) {
      rangeStart = min
      rangeEnd = max
    }

    new ContinuousCharmScale(
        scaleSpec: scaleSpec,
        rangeStart: rangeStart,
        rangeEnd: rangeEnd,
        domainMin: min,
        domainMax: max
    )
  }

  /**
   * Trains a shape scale mapping data values to shape names.
   *
   * @param values data values for shape aesthetic
   * @return trained discrete scale for shape
   */
  private static CharmScale trainShapeScale(List<Object> values, Scale spec) {
    Scale scaleSpec = spec ?: Scale.discrete()
    List<Number> range = scaleSpec.params?.range as List<Number>
    BigDecimal rangeStart = ValueConverter.asBigDecimal(range != null && range.size() > 0 ? range[0] : null) ?: 0.0
    BigDecimal rangeEnd = ValueConverter.asBigDecimal(range != null && range.size() > 1 ? range[1] : null) ?: 1.0
    LinkedHashSet<String> unique = new LinkedHashSet<>()
    values.each { Object value ->
      if (value != null) {
        unique << value.toString()
      }
    }

    new DiscreteCharmScale(
        scaleSpec: scaleSpec,
        rangeStart: rangeStart,
        rangeEnd: rangeEnd,
        levels: new ArrayList<>(unique)
    )
  }

  /**
   * Trains an alpha scale mapping data values to opacity (range 0-1).
   *
   * @param values data values for alpha aesthetic
   * @return trained continuous scale for alpha, or null if no numeric values
   */
  private static CharmScale trainAlphaScale(List<Object> values, Scale spec) {
    Scale scaleSpec = spec ?: Scale.continuous()
    List<Number> range = scaleSpec.params?.range as List<Number>
    BigDecimal rangeStart = ValueConverter.asBigDecimal(range != null && range.size() > 0 ? range[0] : null) ?: 0.1
    BigDecimal rangeEnd = ValueConverter.asBigDecimal(range != null && range.size() > 1 ? range[1] : null) ?: 1.0
    if (scaleSpec.type == ScaleType.DISCRETE) {
      return trainDiscreteScale(values, scaleSpec, rangeStart, rangeEnd)
    }
    if (scaleSpec.type == ScaleType.BINNED) {
      return trainBinnedScale(values, scaleSpec, rangeStart, rangeEnd)
    }

    List<BigDecimal> numeric = coerceToNumeric(values)
    if (numeric.isEmpty()) {
      return null
    }

    BigDecimal min = numeric.min()
    BigDecimal max = ensureDomainRange(numeric.max(), min)
    if (scaleSpec.params?.identity == true) {
      rangeStart = min
      rangeEnd = max
    }

    new ContinuousCharmScale(
        scaleSpec: scaleSpec,
        rangeStart: rangeStart,
        rangeEnd: rangeEnd,
        domainMin: min,
        domainMax: max
    )
  }

  private static CharmScale trainLinetypeScale(List<Object> values, Scale spec) {
    Scale scaleSpec = spec ?: Scale.discrete()
    trainDiscreteScale(values, scaleSpec, 0.0, 1.0)
  }

  /** Coerces a list of values to BigDecimal, filtering out nulls and non-numeric values. */
  private static List<BigDecimal> coerceToNumeric(List<Object> values) {
    values
        .collect { ValueConverter.asBigDecimal(it) }
        .findAll { it != null } as List<BigDecimal>
  }

  /** Returns max adjusted to avoid zero-width domain (which would cause division by zero). */
  private static BigDecimal ensureDomainRange(BigDecimal max, BigDecimal min) {
    max == min ? max + 1 : max
  }

  /**
   * Determine whether the data should be treated as discrete.
   */
  private static boolean resolveDiscrete(List<Object> values, Scale spec) {
    if (spec?.type == ScaleType.DISCRETE) return true
    if (spec?.type == ScaleType.CONTINUOUS || spec?.type == ScaleType.TRANSFORM || spec?.type == ScaleType.DATE) {
      return false
    }
    // Auto-detect: discrete if any non-null value is non-numeric
    !values.every { Object value ->
      value == null || ValueConverter.asBigDecimal(value) != null
    }
  }
}
