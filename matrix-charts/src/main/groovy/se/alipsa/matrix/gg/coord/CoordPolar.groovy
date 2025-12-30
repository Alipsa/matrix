package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

/**
 * The polar coordinate system is most commonly used for pie charts,
 * which are a stacked bar chart in polar coordinates
 */
@CompileStatic
class CoordPolar extends Coord {

    CoordPolar(String theta = "x", BigDecimal offset = 0, Boolean clockwise = true, Boolean clip = true) {

    }
}
