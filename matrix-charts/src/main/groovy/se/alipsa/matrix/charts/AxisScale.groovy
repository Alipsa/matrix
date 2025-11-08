package se.alipsa.matrix.charts

class AxisScale {
  private BigDecimal start
  private BigDecimal end
  private BigDecimal step

  AxisScale() {
  }

  AxisScale(BigDecimal start, BigDecimal end, BigDecimal step) {
    this.start = start
    this.end = end
    this.step = step
  }

  BigDecimal getStart() {
    return start
  }

  void setStart(BigDecimal start) {
    this.start = start
  }

  BigDecimal getEnd() {
    return end
  }

  void setEnd(BigDecimal end) {
    this.end = end
  }

  BigDecimal getStep() {
    return step
  }

  void setStep(BigDecimal step) {
    this.step = step
  }
}
