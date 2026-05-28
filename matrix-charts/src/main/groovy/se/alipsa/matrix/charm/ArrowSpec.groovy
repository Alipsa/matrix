package se.alipsa.matrix.charm

/**
 * Arrow marker specification for segment and curve geoms.
 */
class ArrowSpec {

  private final BigDecimal length
  private final BigDecimal width
  private final ArrowEnds ends
  private final boolean closed

  /**
   * Creates an arrow specification.
   *
   * @param length marker length in pixels
   * @param width marker width in pixels
   * @param ends endpoints where arrows are rendered
   * @param closed true to render a filled triangular arrowhead, false for an open arrowhead
   */
  ArrowSpec(Number length = 6, Number width = 6, ArrowEnds ends = ArrowEnds.END, boolean closed = true) {
    this.length = coercePositive(length, 'length')
    this.width = coercePositive(width, 'width')
    this.ends = ends ?: ArrowEnds.END
    this.closed = closed
  }

  /**
   * Creates an arrow rendered at the end of the line or curve.
   *
   * @param length marker length in pixels
   * @param width marker width in pixels
   * @param closed true for a filled triangular arrowhead
   * @return arrow specification
   */
  static ArrowSpec end(Number length = 6, Number width = 6, boolean closed = true) {
    new ArrowSpec(length, width, ArrowEnds.END, closed)
  }

  /**
   * Creates an arrow rendered at the start of the line or curve.
   *
   * @param length marker length in pixels
   * @param width marker width in pixels
   * @param closed true for a filled triangular arrowhead
   * @return arrow specification
   */
  static ArrowSpec start(Number length = 6, Number width = 6, boolean closed = true) {
    new ArrowSpec(length, width, ArrowEnds.START, closed)
  }

  /**
   * Creates arrows rendered at both ends of the line or curve.
   *
   * @param length marker length in pixels
   * @param width marker width in pixels
   * @param closed true for filled triangular arrowheads
   * @return arrow specification
   */
  static ArrowSpec both(Number length = 6, Number width = 6, boolean closed = true) {
    new ArrowSpec(length, width, ArrowEnds.BOTH, closed)
  }

  /**
   * Returns marker length in pixels.
   *
   * @return marker length
   */
  BigDecimal getLength() {
    length
  }

  /**
   * Returns marker width in pixels.
   *
   * @return marker width
   */
  BigDecimal getWidth() {
    width
  }

  /**
   * Returns endpoints where arrows are rendered.
   *
   * @return endpoint selection
   */
  ArrowEnds getEnds() {
    ends
  }

  /**
   * Returns true for a filled triangular arrowhead.
   *
   * @return closed arrowhead flag
   */
  boolean isClosed() {
    closed
  }

  private static BigDecimal coercePositive(Number value, String name) {
    if (value == null) {
      throw new IllegalArgumentException("${name} must not be null")
    }
    BigDecimal resolved = value as BigDecimal
    if (resolved <= 0) {
      throw new IllegalArgumentException("${name} must be > 0, got ${value}")
    }
    resolved
  }

}
