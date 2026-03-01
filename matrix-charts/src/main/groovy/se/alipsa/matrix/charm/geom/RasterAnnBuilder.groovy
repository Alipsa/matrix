package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for raster annotation layers.
 *
 * <p>Produces a {@code RASTER_ANN / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomRasterAnn().alpha(0.8).interpolate(true)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class RasterAnnBuilder extends LayerBuilder {

  /**
   * Sets raster opacity.
   *
   * @param value alpha 0–1
   * @return this builder
   */
  RasterAnnBuilder alpha(Number value) {
    params['alpha'] = value
    this
  }

  /**
   * Sets whether to interpolate between raster cells.
   *
   * @param value true to interpolate
   * @return this builder
   */
  RasterAnnBuilder interpolate(boolean value) {
    params['interpolate'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.RASTER_ANN
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
