package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for dotplot layers.
 *
 * <p>Produces a {@code DOTPLOT / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'value' }
 *   layers {
 *     geomDotplot().fill('#336699').binwidth(0.5)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class DotplotBuilder extends LayerBuilder {

  /**
   * Sets dot fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  DotplotBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets dot outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  DotplotBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets dot outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  DotplotBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets the bin width for dot stacking.
   *
   * @param value bin width
   * @return this builder
   */
  DotplotBuilder binwidth(Number value) {
    params['binwidth'] = value
    this
  }

  /**
   * Sets the stacking ratio between dots.
   *
   * @param value stack ratio
   * @return this builder
   */
  DotplotBuilder stackratio(Number value) {
    params['stackratio'] = value
    this
  }

  /**
   * Sets the dot size scaling factor.
   *
   * @param value dot size
   * @return this builder
   */
  DotplotBuilder dotsize(Number value) {
    params['dotsize'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.DOTPLOT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
