package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for raster layers.
 *
 * <p>Produces a {@code RASTER / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; fill = 'z' }
 *   layers {
 *     geomRaster().alpha(0.9).interpolate(true)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class RasterBuilder extends LayerBuilder {

  /**
   * Sets raster fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  RasterBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets raster opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  RasterBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets whether to interpolate between cells.
   *
   * @param value true to enable interpolation
   * @return this builder
   */
  RasterBuilder interpolate(boolean value) {
    params['interpolate'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.RASTER
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
