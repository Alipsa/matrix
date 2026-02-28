package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Raster annotation geometry for rendering pre-colored raster images.
 * Unlike GeomRaster, this geom:
 * - Takes pre-colored raster data (no fill scale transformation)
 * - Uses fixed positioning (xmin, xmax, ymin, ymax bounds)
 * - Does not affect scale training (annotation behavior)
 *
 * The raster is a 2D array/list where each cell contains a color value.
 * Row 0 is rendered at the top (ymax), consistent with image conventions.
 *
 * Usage:
 * <pre>{@code
 * def raster = [
 *   ['red', 'green', 'blue'],
 *   ['yellow', 'purple', 'orange']
 * ]
 * annotation_raster(raster: raster, xmin: 0, xmax: 10, ymin: 0, ymax: 5)
 * }</pre>
 */
@CompileStatic
class GeomRasterAnn extends Geom {

  /** Pre-colored raster data: 2D list of color strings */
  List<List<String>> raster

  /**
   * Interpolation mode.
   * When true, attempts to smooth between pixels.
   * Note: SVG has limited interpolation support; this primarily serves as API compatibility.
   */
  boolean interpolate = false

  GeomRasterAnn() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [:] as Map<String, Object>
  }

  GeomRasterAnn(Map params) {
    this()
    if (params.raster != null) {
      this.raster = normalizeRaster(params.raster)
    }
    if (params.interpolate != null) {
      this.interpolate = params.interpolate as boolean
    }
    this.params = params
  }

  private static List<List<String>> normalizeRaster(Object source) {
    if (!(source instanceof List)) {
      return []
    }
    List<List<String>> normalized = []
    (source as List).each { Object row ->
      if (row instanceof List) {
        normalized << (row as List).collect { Object cell ->
          cell == null ? null : ColorUtil.normalizeColor(cell.toString()) ?: cell.toString()
        }
      }
    }
    normalized
  }

}
