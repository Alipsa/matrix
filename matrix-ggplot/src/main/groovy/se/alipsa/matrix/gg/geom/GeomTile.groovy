package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Tile geometry for creating heatmaps and correlation matrices.
 * Draws rectangular tiles centered at each x,y position.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: fill, color, alpha, width, height
 *
 * Usage:
 * - geom_tile() - basic tiles with automatic sizing
 * - geom_tile(fill: 'blue') - blue tiles
 * - geom_tile(aes(fill: 'value')) - tiles colored by value
 */
@CompileStatic
class GeomTile extends Geom {

  /** Fill color for tiles */
  String fill = 'gray'

  /** Stroke color for tile borders */
  String color = 'white'

  /** Line width for tile borders */
  BigDecimal linewidth = 0.5

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Tile width (null for auto-calculation based on data resolution) */
  BigDecimal width = null

  /** Tile height (null for auto-calculation based on data resolution) */
  BigDecimal height = null

  GeomTile() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'white', alpha: 1.0] as Map<String, Object>
  }

  GeomTile(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.width != null) this.width = params.width as BigDecimal
    if (params.height != null) this.height = params.height as BigDecimal
    this.params = params
  }

}
