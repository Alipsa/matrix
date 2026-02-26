package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for area layers.
 *
 * <p>Produces an {@code AREA / ALIGN} layer specification. The ALIGN stat
 * aligns multi-series data to a shared x-grid using interpolation.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; group = 'series' }
 *   layers {
 *     geomArea().fill('#336699').alpha(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class AreaBuilder extends LayerBuilder {

  /**
   * Sets area fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  AreaBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets area outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  AreaBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets area outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  AreaBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets area opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  AreaBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets area outline line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  AreaBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.AREA
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.ALIGN
  }
}
