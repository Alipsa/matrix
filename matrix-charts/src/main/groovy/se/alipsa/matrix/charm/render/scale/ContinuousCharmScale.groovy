package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
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
@CompileStatic
class ContinuousCharmScale extends CharmScale {

  private static final BigDecimal BREAK_TOLERANCE_RATIO = 0.001
  private static final int DEFAULT_BREAK_COUNT = 7

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
    if (value == null) return null

    BigDecimal numeric = ValueConverter.asBigDecimal(value)
    if (numeric == null) return null

    if (transformStrategy != null) {
      numeric = transformStrategy.apply(numeric)
      if (numeric == null) return null
    }

    if (domainMax == domainMin) {
      return (rangeStart + rangeEnd) / 2
    }

    if (transformStrategy instanceof ReverseScaleTransform) {
      ScaleUtils.linearTransformReversed(numeric, domainMin, domainMax, rangeStart, rangeEnd)
    } else {
      ScaleUtils.linearTransform(numeric, domainMin, domainMax, rangeStart, rangeEnd)
    }
  }

  @Override
  List<Object> ticks(int count) {
    if (domainMin == null || domainMax == null) return []
    int n = count <= 1 ? 2 : count

    if (transformStrategy instanceof Log10ScaleTransform) {
      return generateLog10Ticks() as List<Object>
    }

    List<Object> breaks = generateNiceBreaks(domainMin, domainMax, n) as List<Object>
    if (transformStrategy instanceof ReverseScaleTransform) {
      return breaks.reverse()
    }
    breaks
  }

  @Override
  List<String> tickLabels(int count) {
    List<Object> tickValues = ticks(count)

    if (transformStrategy instanceof Log10ScaleTransform) {
      return tickValues.collect { Object tick ->
        formatLogNumber(tick as Number)
      }
    }

    tickValues.collect { Object tick ->
      formatNumber(tick as Number)
    }
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
  private static String formatNumber(Number n) {
    if (n == null) return ''
    BigDecimal bd = n as BigDecimal
    if (bd.stripTrailingZeros().scale() <= 0) {
      return bd.toBigInteger().toString()
    }
    BigDecimal rounded = bd.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
    rounded.toPlainString()
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
      return String.format('%.2g', bd as double)
    }
    String.format('%.0f', bd as double)
  }
}
