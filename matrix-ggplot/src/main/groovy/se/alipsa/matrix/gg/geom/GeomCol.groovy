package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

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
