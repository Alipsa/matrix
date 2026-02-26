package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for parallel coordinate layers.
 *
 * <p>Produces a {@code PARALLEL / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomParallel().color('#336699').alpha(0.3)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class ParallelBuilder extends LayerBuilder {

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  ParallelBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  ParallelBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  ParallelBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  ParallelBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  ParallelBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.PARALLEL
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
