package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for simple feature (SF) geometry layers.
 *
 * <p>Produces a {@code SF / SF} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   layers {
 *     geomSf().fill('#336699').color('#000000')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class SfBuilder extends LayerBuilder {

  /**
   * Sets feature fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  SfBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets feature outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  SfBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets feature outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  SfBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets feature opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  SfBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets feature outline line width.
   *
   * @param value size value
   * @return this builder
   */
  SfBuilder size(Number value) {
    params['size'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.SF
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.SF
  }
}
