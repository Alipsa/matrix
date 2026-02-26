package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for label layers.
 *
 * <p>Produces a {@code LABEL / IDENTITY} layer specification.
 * Labels are like text but with a filled background rectangle.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; label = 'name' }
 *   layers {
 *     geomLabel().size(4).fill('#ffffff').color('#333333')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class LabelBuilder extends LayerBuilder {

  /**
   * Sets label text size.
   *
   * @param value size value
   * @return this builder
   */
  LabelBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets label text colour.
   *
   * @param value colour value
   * @return this builder
   */
  LabelBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets label text colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  LabelBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets label background fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  LabelBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets label rotation angle in degrees.
   *
   * @param value angle in degrees
   * @return this builder
   */
  LabelBuilder angle(Number value) {
    params['angle'] = value
    this
  }

  /**
   * Sets font family.
   *
   * @param value font family name
   * @return this builder
   */
  LabelBuilder family(String value) {
    params['family'] = value
    this
  }

  /**
   * Sets font face (e.g. bold, italic).
   *
   * @param value fontface name or integer code
   * @return this builder
   */
  LabelBuilder fontface(Object value) {
    params['fontface'] = value
    this
  }

  /**
   * Sets horizontal justification (0 = left, 0.5 = centre, 1 = right).
   *
   * @param value horizontal justification
   * @return this builder
   */
  LabelBuilder hjust(Number value) {
    params['hjust'] = value
    this
  }

  /**
   * Sets vertical justification (0 = bottom, 0.5 = middle, 1 = top).
   *
   * @param value vertical justification
   * @return this builder
   */
  LabelBuilder vjust(Number value) {
    params['vjust'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.LABEL
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
