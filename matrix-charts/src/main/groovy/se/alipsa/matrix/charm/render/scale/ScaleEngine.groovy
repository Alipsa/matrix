package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.ScaleType
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerRenderJob
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
   * @return trained scales container
   */
  static TrainedScales train(Chart chart, RenderConfig config,
                              List<Object> xValues, List<Object> yValues,
                              List<Object> colorValues, List<Object> fillValues) {
    TrainedScales trained = new TrainedScales()

    trained.x = trainPositionalScale(xValues, chart.scale.x, 0, config.plotWidth())
    trained.y = trainPositionalScale(yValues, chart.scale.y, config.plotHeight(), 0)

    boolean hasColor = colorValues.any { it != null }
    if (hasColor) {
      trained.color = trainColorScale(colorValues, chart.scale.color)
    }

    boolean hasFill = fillValues.any { it != null }
    if (hasFill) {
      trained.fill = trainColorScale(fillValues, chart.scale.fill)
    }

    trained
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
    ColorCharmScale colorScale = new ColorCharmScale(
        rangeStart: 0.0,
        rangeEnd: 1.0
    )
    colorScale.trainFromValues(values, spec ?: Scale.discrete())
    colorScale
  }

  private static ContinuousCharmScale trainContinuousScale(
      List<Object> values, Scale scaleSpec,
      BigDecimal rangeStart, BigDecimal rangeEnd) {

    List<BigDecimal> numeric = values
        .collect { NumberCoercionUtil.coerceToBigDecimal(it) }
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
      value == null || NumberCoercionUtil.coerceToBigDecimal(value) != null
    }
  }
}
