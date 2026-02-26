package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for density layers.
 *
 * <p>Produces a {@code DENSITY / DENSITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'value' }
 *   layers {
 *     geomDensity().fill('#336699').alpha(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class DensityBuilder extends LayerBuilder {

  /**
   * Sets density fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  DensityBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets density outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  DensityBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets density outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  DensityBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets density opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  DensityBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets density outline line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  DensityBuilder linetype(Object value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets the bandwidth adjustment multiplier.
   *
   * @param value adjustment factor (default 1)
   * @return this builder
   */
  DensityBuilder adjust(Number value) {
    params['adjust'] = value
    this
  }

  /**
   * Sets the kernel function for density estimation.
   *
   * @param value kernel name (e.g. 'gaussian', 'epanechnikov')
   * @return this builder
   */
  DensityBuilder kernel(String value) {
    params['kernel'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.DENSITY
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.DENSITY
  }
}
