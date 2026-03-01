package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for crossbar layers.
 *
 * <p>Produces a {@code CROSSBAR / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'mid'; ymin = 'lo'; ymax = 'hi' }
 *   layers {
 *     geomCrossbar().fill('#eeeeee').width(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class CrossbarBuilder extends LayerBuilder {

  /**
   * Sets crossbar width.
   *
   * @param value width value
   * @return this builder
   */
  CrossbarBuilder width(Number value) {
    params['width'] = value
    this
  }

  /**
   * Sets crossbar fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  CrossbarBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets crossbar outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  CrossbarBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets crossbar outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  CrossbarBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets the multiplicative factor for the middle bar thickness.
   *
   * @param value fatten factor
   * @return this builder
   */
  CrossbarBuilder fatten(Number value) {
    params['fatten'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.CROSSBAR
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
