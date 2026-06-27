package se.alipsa.matrix.pict

/**
 * Defines a custom axis scale with start, end, and step values.
 * Instances are immutable after construction.
 */
class AxisScale {

  private final BigDecimal start
  private final BigDecimal end
  private final BigDecimal step

  AxisScale(BigDecimal start, BigDecimal end, BigDecimal step) {
    if (start == null || end == null || step == null) {
      throw new IllegalArgumentException('start, end, and step must not be null')
    }
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

  /** Returns the start (minimum) value of this scale. */
  BigDecimal getStart() {
    return start
  }

  /** Returns the end (maximum) value of this scale. */
  BigDecimal getEnd() {
    return end
  }

  /** Returns the step size between consecutive axis breaks. */
  BigDecimal getStep() {
    return step
  }

}
