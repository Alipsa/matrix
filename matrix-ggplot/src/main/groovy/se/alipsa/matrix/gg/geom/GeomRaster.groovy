package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Raster geometry for fast rendering of rectangular tiles on a regular grid.
 * Similar to geom_tile() but optimized for cases where data is on a regular grid
 * (e.g., image data, raster graphics). Faster than geom_tile() for large regular grids.
 *
 * Key differences from geom_tile():
 * - Assumes data is on a regular grid
 * - No stroke/border by default (for performance)
 * - Optimized for large numbers of tiles
 *
 * Required aesthetics: x, y
 * Optional aesthetics: fill, alpha
 *
 * Usage:
 * - geom_raster() - basic raster with automatic sizing
 * - geom_raster(aes(fill: 'value')) - colored by value
 * - geom_raster(interpolate: true) - smooth interpolation (future enhancement)
 */
@CompileStatic
class GeomRaster extends Geom {

  /** Fill color for raster cells */
  String fill = 'gray'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Cell width (null for auto-calculation) */
  Number width = null

  /** Cell height (null for auto-calculation) */
  Number height = null

  /** Whether to interpolate between cells (future enhancement) */
  boolean interpolate = false

  GeomRaster() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', alpha: 1.0] as Map<String, Object>
  }

  GeomRaster(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.width != null) this.width = params.width as Number
    if (params.height != null) this.height = params.height as Number
    if (params.interpolate != null) this.interpolate = params.interpolate as boolean
    this.params = params
  }

}
