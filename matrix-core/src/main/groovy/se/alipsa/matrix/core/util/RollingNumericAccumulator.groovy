package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.ValueConverter

import java.math.RoundingMode

/**
 * Internal sliding-window accumulator for numeric rolling operations.
 */
@CompileStatic
class RollingNumericAccumulator {

  private static final int MEAN_SCALE = 16

  private final List<?> orderedValues
  private BigDecimal rawSum = BigDecimal.ZERO
  private BigDecimal scaledSum = BigDecimal.ZERO.setScale(MEAN_SCALE, RoundingMode.HALF_UP)
  private int numericCount = 0
  private int currentStart = 0
  private int currentEnd = -1

  RollingNumericAccumulator(List<?> orderedValues) {
    this.orderedValues = orderedValues
  }

  void moveTo(IntRange targetRange) {
    while (currentStart < targetRange.from) {
      removeValue(orderedValues[currentStart])
      currentStart++
    }
    while (currentEnd < targetRange.to) {
      currentEnd++
      addValue(orderedValues[currentEnd])
    }
  }

  BigDecimal sumOrNull(int minPeriods) {
    numericCount < minPeriods ? null : rawSum
  }

  BigDecimal meanOrNull(int minPeriods) {
    if (numericCount < minPeriods) {
      return null
    }
    if (scaledSum == 0) {
      return BigDecimal.ZERO.setScale(MEAN_SCALE, RoundingMode.HALF_UP)
    }
    scaledSum.divide(numericCount as BigDecimal, MEAN_SCALE, RoundingMode.HALF_UP)
  }

  private void addValue(Object value) {
    BigDecimal numericValue = numericValue(value)
    if (numericValue == null) {
      return
    }
    rawSum = rawSum.add(numericValue)
    scaledSum = scaledSum.add(numericValue.setScale(MEAN_SCALE, RoundingMode.HALF_UP))
    numericCount++
  }

  private void removeValue(Object value) {
    BigDecimal numericValue = numericValue(value)
    if (numericValue == null) {
      return
    }
    rawSum = rawSum.subtract(numericValue)
    scaledSum = scaledSum.subtract(numericValue.setScale(MEAN_SCALE, RoundingMode.HALF_UP))
    numericCount--
  }

  @SuppressWarnings('Instanceof')
  private static BigDecimal numericValue(Object value) {
    value instanceof Number ? ValueConverter.asBigDecimal(value as Number) : null
  }

}
