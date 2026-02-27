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
 * Column geometry for bar charts with pre-computed heights.
 * Uses stat_identity by default - expects y values to be provided directly.
 * For automatic counting, use GeomBar instead.
 *
 * Difference from GeomBar:
 * - GeomBar: stat = COUNT, only requires x aesthetic, counts occurrences
 * - GeomCol: stat = IDENTITY, requires both x and y aesthetics
 */
@CompileStatic
class GeomCol extends GeomBar {

  GeomCol() {
    super()
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
  }

  GeomCol(Map params) {
    super(params)
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
  }

}
