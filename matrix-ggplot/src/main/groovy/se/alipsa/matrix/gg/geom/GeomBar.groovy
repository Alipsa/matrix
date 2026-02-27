package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.coord.CoordRadial
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleDiscrete

import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Bar geometry for bar charts (counts/frequencies).
 * Uses stat_count by default to count observations in each category.
 * For pre-computed heights, use GeomCol instead.
 */
@CompileStatic
class GeomBar extends Geom {

  /** Bar fill color */
  String fill = '#595959'

  /** Bar outline color */
  String color = null

  /** Bar width as fraction of bandwidth (0-1), null = auto */
  BigDecimal width = null

  /** Alpha transparency */
  BigDecimal alpha = 1.0

  /** Outline width */
  Number linewidth = 0.5

  GeomBar() {
    defaultStat = StatType.COUNT
    defaultPosition = PositionType.STACK
    requiredAes = ['x']
    defaultAes = [fill: '#595959', alpha: 1.0] as Map<String, Object>
  }

  GeomBar(Map params) {
    this()
    this.fill = ColorUtil.normalizeColor(params.fill as String) ?: this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.width = params.width as BigDecimal ?: this.width
    this.alpha = params.alpha as BigDecimal ?: this.alpha
    this.linewidth = params.linewidth as Number ?: this.linewidth
    this.params = params
  }

}
