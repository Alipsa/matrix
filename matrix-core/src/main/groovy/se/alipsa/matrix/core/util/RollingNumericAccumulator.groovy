package se.alipsa.matrix.core.util

import se.alipsa.matrix.core.ValueConverter

import java.math.MathContext

/**
 * Internal sliding-window accumulator for numeric rolling operations.
 */
class RollingNumericAccumulator {

  private static final MathContext MEAN_CONTEXT = new MathContext(16)

  private final List<?> orderedValues
  private BigDecimal rawSum = BigDecimal.ZERO
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
    rawSum.divide(numericCount as BigDecimal, MEAN_CONTEXT)
  }

  private void addValue(Object value) {
    BigDecimal numericValue = numericValue(value)
    if (numericValue == null) {
      return
    }
    rawSum = rawSum.add(numericValue)
    numericCount++
  }

  private void removeValue(Object value) {
    BigDecimal numericValue = numericValue(value)
    if (numericValue == null) {
      return
    }
    rawSum = rawSum.subtract(numericValue)
    numericCount--
  }

  private static BigDecimal numericValue(Object value) {
    value instanceof Number ? ValueConverter.asBigDecimal(value as Number) : null
  }

}
