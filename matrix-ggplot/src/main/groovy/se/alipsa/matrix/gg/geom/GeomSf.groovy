package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Simple Features geometry for rendering WKT-based spatial data.
 * Routes POINTs to geom_point, LINESTRINGs to geom_path, and POLYGONs to geom_polygon.
 */
@CompileStatic
class GeomSf extends Geom {

  /** Outline color */
  String color = 'black'

  /** Fill color */
  String fill = 'gray'

  /** Line width */
  Number size = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Point shape */
  String shape = 'circle'

  GeomSf() {
    defaultStat = StatType.SF
    requiredAes = []
    defaultAes = [color: 'black', fill: 'gray', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomSf(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.size != null) this.size = params.size as Number
    if (params.linewidth != null) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.shape) this.shape = params.shape as String
    this.params = params
  }

  /**
   * Render simple feature geometries using existing point/line/polygon geoms.
   *
   * @param group SVG group for rendering
   * @param data stat-expanded geometry data
   * @param aes aesthetic mappings
   * @param scales scale map for rendering
   * @param coord coordinate system
   */

}
