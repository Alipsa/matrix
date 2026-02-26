package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

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
   * Sets ribbon outline line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  RibbonBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.RIBBON
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
