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
 * Count geometry for showing point counts at each location.
 * Useful for dealing with overplotting - shows points sized by count.
 *
 * Usage:
 * - geom_count() - points sized by number of observations
 * - geom_count(color: 'blue') - colored count points
 *
 * The n (count) aesthetic is computed automatically.
 */
@CompileStatic
class GeomCount extends Geom {

  /** Point color */
  String color = 'black'

  /** Point fill color */
  String fill = 'steelblue'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 0.7

  /** Minimum point size (for count=1) */
  BigDecimal sizeMin = 3

  /** Maximum point size (for max count) */
  BigDecimal sizeMax = 15

  /** Point shape: 'circle', 'square', 'triangle' */
  String shape = 'circle'

  /** Stroke width for point border */
  BigDecimal stroke = 1

  GeomCount() {
    defaultStat = StatType.IDENTITY  // We compute counts internally
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'steelblue', color: 'black', alpha: 0.7] as Map<String, Object>
  }

  GeomCount(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.sizeMin = (params.sizeMin ?: params.size_min) as BigDecimal ?: this.sizeMin
    this.sizeMax = (params.sizeMax ?: params.size_max) as BigDecimal ?: this.sizeMax
    this.shape = params.shape as String ?: this.shape
    this.stroke = params.stroke as BigDecimal ?: this.stroke
    this.params = params
  }

}
