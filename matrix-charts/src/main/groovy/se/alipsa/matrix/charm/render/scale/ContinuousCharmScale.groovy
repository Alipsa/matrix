package se.alipsa.matrix.charm.render.scale

import se.alipsa.matrix.charm.Log10ScaleTransform
import se.alipsa.matrix.charm.ReverseScaleTransform
import se.alipsa.matrix.charm.ScaleTransform
import se.alipsa.matrix.core.ValueConverter

import java.math.RoundingMode

/**
 * Trained continuous scale for numeric data.
 *
 * Maps numeric values from a data domain to a pixel range using linear interpolation,
 * with support for transform strategies (log10, reverse, etc.), nice Wilkinson breaks,
 * domain expansion, and explicit limits.
 */
@SuppressWarnings('DuplicateNumberLiteral')
@SuppressWarnings('IfStatementBraces')
@SuppressWarnings('UnnecessaryCast')
@SuppressWarnings('UnnecessaryToString')
class ContinuousCharmScale extends CharmScale {

  private static final BigDecimal BREAK_TOLERANCE_RATIO = 0.001

  /** Computed data domain minimum (possibly transformed). */
  BigDecimal domainMin

  /** Computed data domain maximum (possibly transformed). */
  BigDecimal domainMax

  /** Optional expansion [mult, add]. */
  List<Number> expand

  /** Optional explicit limits [min, max]. */
  List<Number> limits

  /** Optional transform strategy (log10, reverse, etc.). */
  ScaleTransform transformStrategy

  @Override
  BigDecimal transform(Object value) {
    if (value == null) {
      return null
    }

    BigDecimal numeric
    if (TemporalScaleUtil.isTemporalTransform(transformStrategy)) {
      numeric = TemporalScaleUtil.toCanonicalValue(value, transformStrategy, scaleSpec?.params ?: [:])
    } else {
      numeric = ValueConverter.asBigDecimal(value)
    }
    if (numeric == null) {
      return null
    }

    if (transformStrategy != null) {
      numeric = transformStrategy.apply(numeric)
      if (numeric == null) {
        return null
      }
    }

    if (domainMax == domainMin) {
      return (rangeStart + rangeEnd) / 2
    }

    ScaleUtils.linearTransform(numeric, domainMin, domainMax, rangeStart, rangeEnd)
  }

  @Override
  List<Object> ticks(int count) {
    if (domainMin == null || domainMax == null) {
      return []
    }

    List<Object> configured = resolveConfiguredBreaks()
    if (!configured.isEmpty()) {
      return configured
    }

    int n = count <= 1 ? 2 : count

    String temporalId = TemporalScaleUtil.normalizeTransformId(transformStrategy?.id())
    if (TemporalScaleUtil.isTemporalTransformId(temporalId)) {
      String intervalSpec = resolveIntervalSpec(temporalId)
      List<BigDecimal> temporalBreaks = intervalSpec
          ? TemporalScaleUtil.breaksFromSpec(temporalId, intervalSpec, domainMin, domainMax, scaleSpec?.params ?: [:])
          : TemporalScaleUtil.autoBreaks(temporalId, domainMin, domainMax, n, scaleSpec?.params ?: [:])
      if (temporalBreaks.isEmpty()) {
        temporalBreaks = TemporalScaleUtil.autoBreaks(temporalId, domainMin, domainMax, n, scaleSpec?.params ?: [:])
      }
      return temporalBreaks.collect { BigDecimal value -> value } as List<Object>
    }

    if (transformStrategy instanceof Log10ScaleTransform) {
      return generateLog10Ticks() as List<Object>
    }

    List<Number> breaks = generateNiceBreaks(domainMin, domainMax, n)
    if (transformStrategy == null) {
      return breaks as List<Object>
    }
    List<Object> dataBreaks = breaks.collect { Number value -> transformStrategy.invert(value as BigDecimal) }
        .findAll { it != null } as List<Object>
    if (transformStrategy instanceof ReverseScaleTransform) {
      return dataBreaks.reverse()
    }
    dataBreaks
  }

  @Override
  List<String> tickLabels(int count) {
    List<Object> tickValues = ticks(count)
    String temporalId = TemporalScaleUtil.normalizeTransformId(transformStrategy?.id())
    boolean temporal = TemporalScaleUtil.isTemporalTransformId(temporalId)
    List<String> configured = scaleSpec?.labels
    if (configured != null && !configured.isEmpty()) {
      List<String> labels = []
      tickValues.eachWithIndex { Object tick, int idx ->
        if (idx < configured.size() && configured[idx] != null) {
          labels << configured[idx]
          return
        }
        if (temporal) {
          BigDecimal numeric = ValueConverter.asBigDecimal(tick)
          labels << TemporalScaleUtil.formatTick(numeric, temporalId, scaleSpec?.params ?: [:])
        } else {
          labels << defaultTickLabel(tick)
        }
      }
      return labels
    }

    if (temporal) {
      return tickValues.collect { Object tick ->
        BigDecimal numeric = ValueConverter.asBigDecimal(tick)
        TemporalScaleUtil.formatTick(numeric, temporalId, scaleSpec?.params ?: [:])
      }
    }

    if (transformStrategy instanceof Log10ScaleTransform) {
      return tickValues.collect { Object tick ->
        formatLogNumber(tick as Number)
      }
    }

    BigDecimal spacing = tickSpacing(tickValues)
    tickValues.collect { Object tick -> defaultTickLabel(tick, spacing) }
  }

  @Override
  boolean isDiscrete() {
    false
  }

  /**
   * Generate nice round breaks for axis ticks using Wilkinson's algorithm.
   *
   * @param min domain minimum
   * @param max domain maximum
   * @param n target number of breaks
   * @return list of break values
   */
  private List<Number> generateNiceBreaks(BigDecimal min, BigDecimal max, int n) {
    if (max == min) return [min] as List<Number>

    BigDecimal rawRange = max - min
    BigDecimal spacing = ScaleUtils.niceNum(rawRange / (n - 1), true)

    BigDecimal niceMin = (min / spacing).floor() * spacing
    BigDecimal niceMax = (max / spacing).ceil() * spacing

    List<Number> breaks = []
    BigDecimal tolerance = spacing * BREAK_TOLERANCE_RATIO
    for (BigDecimal val = niceMin; val <= niceMax + tolerance; val += spacing) {
      if (val >= min - tolerance && val <= max + tolerance) {
        breaks << val
      }
    }
    breaks
  }

  /**
   * Generate power-of-10 breaks for log10 scales.
   */
  private List<Number> generateLog10Ticks() {
    List<Number> breaks = []
    int minPow = domainMin.floor().intValue()
    int maxPow = domainMax.ceil().intValue()

    for (int pow = minPow; pow <= maxPow; pow++) {
      BigDecimal logVal = pow as BigDecimal
      if (logVal >= domainMin && logVal <= domainMax) {
        BigDecimal value = (10 ** pow) as BigDecimal
        breaks << value
      }
    }

    if (breaks.size() < 3) {
      List<Number> intermediates = []
      for (int pow = minPow; pow <= maxPow; pow++) {
        for (BigDecimal mult : [2.0, 5.0]) {
          BigDecimal val = mult * ((10 ** pow) as BigDecimal)
          BigDecimal logVal = val.log10()
          if (logVal >= domainMin && logVal <= domainMax) {
            intermediates << val
          }
        }
      }
      breaks.addAll(intermediates)
      breaks = breaks.sort() as List<Number>
    }
    breaks
  }

  /**
   * Format a number for display on axis labels.
   *
   * @param n number to format
   * @return formatted string
   */
  private static String formatNumber(Number n, BigDecimal spacing) {
    if (n == null) return ''
    BigDecimal bd = n as BigDecimal
    if (bd.stripTrailingZeros().scale() <= 0) {
      return bd.toBigInteger().toString()
    }
    int decimals = 2
    if (spacing != null && spacing != 0) {
      decimals = spacing.stripTrailingZeros().scale().intValue().max(0) as int
    }
    bd.setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
  }

  /**
   * Format a number for display on log scale axis.
   *
   * @param n number to format
   * @return formatted string
   */
  private static String formatLogNumber(Number n) {
    if (n == null) return ''
    BigDecimal bd = n as BigDecimal
    if (bd.stripTrailingZeros().scale() <= 0) {
      return bd.toBigInteger().toString()
    }
    if (bd < BigDecimal.ONE) {
      return String.format(Locale.ROOT, '%.2g', bd)
    }
    String.format(Locale.ROOT, '%.0f', bd)
  }

  private List<Object> resolveConfiguredBreaks() {
    List configured = scaleSpec?.breaks
    if (configured == null || configured.isEmpty()) {
      return []
    }
    if (TemporalScaleUtil.isTemporalTransform(transformStrategy)) {
      return ScaleUtils.coerceConfiguredBreaksOrThrow(
          configured,
          { Object value -> TemporalScaleUtil.toCanonicalValue(value, transformStrategy, scaleSpec?.params ?: [:]) },
          'temporal'
      )
    }
    ScaleUtils.coerceConfiguredBreaksOrThrow(
        configured,
        { Object value -> ValueConverter.asBigDecimal(value) },
        'numeric'
    )
  }

  private String resolveIntervalSpec(String temporalId) {
    Map<String, Object> params = scaleSpec?.params ?: [:]
    String dateBreaks = params['dateBreaks']?.toString()
    String timeBreaks = params['timeBreaks']?.toString()
    if (temporalId == 'time') {
      return timeBreaks ?: dateBreaks
    }
    if (temporalId == 'datetime') {
      return dateBreaks ?: timeBreaks
    }
    dateBreaks
  }

  private static BigDecimal tickSpacing(List<Object> ticks) {
    if (ticks.size() < 2) {
      return null
    }
    BigDecimal smallest = null
    for (int idx = 1; idx < ticks.size(); idx++) {
      BigDecimal previous = ValueConverter.asBigDecimal(ticks[idx - 1])
      BigDecimal current = ValueConverter.asBigDecimal(ticks[idx])
      if (previous == null || current == null) {
        continue
      }
      BigDecimal spacing = (current - previous).abs()
      if (spacing != 0 && (smallest == null || spacing < smallest)) {
        smallest = spacing
      }
    }
    smallest
  }

  private static String defaultTickLabel(Object tick, BigDecimal spacing = null) {
    if (tick instanceof Number) {
      return formatNumber(tick as Number, spacing)
    }
    tick?.toString() ?: ''
  }

}
