package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic

import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.LinetypeName

/**
 * Fluent builder for ribbon layers.
 *
 * <p>Produces a {@code RIBBON / IDENTITY} layer specification.
 * Ribbons require {@code ymin} and {@code ymax} mappings.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; ymin = 'lo'; ymax = 'hi' }
 *   layers {
 *     geomRibbon().fill('#336699').alpha(0.3)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class RibbonBuilder extends LayerBuilder {

  /**
   * Sets ribbon fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  RibbonBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets ribbon outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  RibbonBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets ribbon outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  RibbonBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets ribbon opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  RibbonBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name
   * @return this builder
   */
  RibbonBuilder linetype(LinetypeName value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.RIBBON
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
