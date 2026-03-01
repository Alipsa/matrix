package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for simple feature text layers.
 *
 * <p>Produces a {@code SF_TEXT / SF_COORDINATES} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   layers {
 *     geomSfText().size(3).color('#000000')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class SfTextBuilder extends LayerBuilder {

  /**
   * Sets text size.
   *
   * @param value size value
   * @return this builder
   */
  SfTextBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets text colour.
   *
   * @param value colour value
   * @return this builder
   */
  SfTextBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets text colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  SfTextBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets text font family.
   *
   * @param value font family name
   * @return this builder
   */
  SfTextBuilder family(String value) {
    params['family'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.SF_TEXT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.SF_COORDINATES
  }
}
