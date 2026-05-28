package se.alipsa.matrix.charm.geom

/**
 * Shared fluent API for text-like geoms.
 *
 * @param <T> concrete builder type
 */
abstract class TextLayerBuilder<T extends TextLayerBuilder<T>> extends LayerBuilder {

  private static final String FONTFACE = 'fontface'

  /**
   * Sets text size.
   *
   * @param value size value
   * @return this builder
   */
  T size(Number value) {
    params['size'] = value
    self()
  }

  /**
   * Sets text colour.
   *
   * @param value colour value
   * @return this builder
   */
  T color(String value) {
    params['color'] = value
    self()
  }

  /**
   * Sets text colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  T colour(String value) {
    color(value)
  }

  /**
   * Sets text rotation angle in degrees.
   *
   * @param value angle in degrees
   * @return this builder
   */
  T angle(Number value) {
    params['angle'] = value
    self()
  }

  /**
   * Sets font family.
   *
   * @param value font family name
   * @return this builder
   */
  T family(String value) {
    params['family'] = value
    self()
  }

  /**
   * Sets font face by name (e.g. 'bold', 'italic').
   *
   * @param value fontface name
   * @return this builder
   */
  T fontface(String value) {
    params[FONTFACE] = value
    self()
  }

  /**
   * Sets font face by integer code (1 = plain, 2 = bold, 3 = italic, 4 = bold-italic).
   *
   * @param value fontface integer code
   * @return this builder
   */
  T fontface(int value) {
    params[FONTFACE] = value
    self()
  }

  /**
   * Sets horizontal justification (0 = left, 0.5 = centre, 1 = right).
   *
   * @param value horizontal justification
   * @return this builder
   */
  T hjust(Number value) {
    params['hjust'] = value
    self()
  }

  /**
   * Sets vertical justification (0 = bottom, 0.5 = middle, 1 = top).
   *
   * @param value vertical justification
   * @return this builder
   */
  T vjust(Number value) {
    params['vjust'] = value
    self()
  }

  protected T self() {
    this as T
  }

}
