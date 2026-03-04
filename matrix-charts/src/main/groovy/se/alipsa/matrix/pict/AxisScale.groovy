package se.alipsa.matrix.pict

import groovy.transform.CompileStatic

/**
 * Defines a custom axis scale with start, end, and step values.
 * Instances are immutable after construction.
 */
@CompileStatic
class AxisScale {
  private final BigDecimal start
  private final BigDecimal end
  private final BigDecimal step

  AxisScale(BigDecimal start, BigDecimal end, BigDecimal step) {
    if (start >= end) {
      throw new IllegalArgumentException("start ($start) must be less than end ($end)")
    }
    if (step <= 0) {
      throw new IllegalArgumentException("step ($step) must be positive")
    }
    this.start = start
    this.end = end
    this.step = step
  }

  BigDecimal getStart() {
    return start
  }

  BigDecimal getEnd() {
    return end
  }

  BigDecimal getStep() {
    return step
  }
}
