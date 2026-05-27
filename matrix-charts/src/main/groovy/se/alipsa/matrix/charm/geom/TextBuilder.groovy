package se.alipsa.matrix.charm.geom

import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for text layers.
 *
 * <p>Produces a {@code TEXT / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; label = 'name' }
 *   layers {
 *     geomText().size(4).color('#333333')
 *   }
 * }
 * }</pre>
 */
class TextBuilder extends LayerBuilder {

  private static final String FONTFACE = 'fontface'

  /**
   * Sets the label text content.
   *
   * @param value label text
   * @return this builder
   */
  TextBuilder label(String value) {
    params['label'] = value
    this
  }

  /**
   * Sets text size.
   *
   * @param value size value
   * @return this builder
   */
  TextBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets text colour.
   *
   * @param value colour value
   * @return this builder
   */
  TextBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets text colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  TextBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets text rotation angle in degrees.
   *
   * @param value angle in degrees
   * @return this builder
   */
  TextBuilder angle(Number value) {
    params['angle'] = value
    this
  }

  /**
   * Sets font family.
   *
   * @param value font family name
   * @return this builder
   */
  TextBuilder family(String value) {
    params['family'] = value
    this
  }

  /**
   * Sets font face by name (e.g. 'bold', 'italic').
   *
   * @param value fontface name
   * @return this builder
   */
  TextBuilder fontface(String value) {
    params[FONTFACE] = value
    this
  }

  /**
   * Sets font face by integer code (1 = plain, 2 = bold, 3 = italic, 4 = bold-italic).
   *
   * @param value fontface integer code
   * @return this builder
   */
  TextBuilder fontface(int value) {
    params[FONTFACE] = value
    this
  }

  /**
   * Sets horizontal justification (0 = left, 0.5 = centre, 1 = right).
   *
   * @param value horizontal justification
   * @return this builder
   */
  TextBuilder hjust(Number value) {
    params['hjust'] = value
    this
  }

  /**
   * Sets vertical justification (0 = bottom, 0.5 = middle, 1 = top).
   *
   * @param value vertical justification
   * @return this builder
   */
  TextBuilder vjust(Number value) {
    params['vjust'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.TEXT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }

}
