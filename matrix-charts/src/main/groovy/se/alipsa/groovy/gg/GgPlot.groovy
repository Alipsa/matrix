package se.alipsa.groovy.gg

import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.gg.aes.Aes
import se.alipsa.groovy.gg.coord.CoordFlip
import se.alipsa.groovy.gg.coord.CoordPolar
import se.alipsa.groovy.gg.geom.GeomAbline
import se.alipsa.groovy.gg.geom.GeomBar
import se.alipsa.groovy.gg.geom.GeomBin2d
import se.alipsa.groovy.gg.geom.GeomBlank
import se.alipsa.groovy.gg.geom.GeomBoxplot
import se.alipsa.groovy.gg.geom.GeomCol
import se.alipsa.groovy.gg.geom.GeomContour
import se.alipsa.groovy.gg.geom.GeomContourFilled
import se.alipsa.groovy.gg.geom.GeomCount
import se.alipsa.groovy.gg.geom.GeomHistogram
import se.alipsa.groovy.gg.geom.GeomHline
import se.alipsa.groovy.gg.geom.GeomPoint
import se.alipsa.groovy.gg.geom.GeomRug
import se.alipsa.groovy.gg.geom.GeomSegment
import se.alipsa.groovy.gg.geom.GeomSmooth
import se.alipsa.groovy.gg.geom.GeomViolin
import se.alipsa.groovy.gg.geom.GeomVline
import se.alipsa.groovy.gg.scale.ScaleColorManual
import se.alipsa.groovy.gg.stat.StatBin2d
import se.alipsa.groovy.gg.stat.StatBoxplot
import se.alipsa.groovy.gg.stat.StatContour
import se.alipsa.groovy.gg.stat.StatContourFilled
import se.alipsa.groovy.gg.stat.StatCount
import se.alipsa.groovy.gg.stat.StatSum
import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.ValueConverter

/**
 * An api very similar to ggplot2 making ports from R code using ggplot2 simple.
 */
class GgPlot {

  static GgChart ggplot(Matrix data, Aes aes) {
    return new GgChart(data, aes)
  }

  static Aes aes(Map params) {
    def p = [:]
    params.computeIfPresent('x', (k,v) -> p.xCol = v)
    params.computeIfPresent('y', (k,v) -> p.yCol = v)
    params.computeIfPresent('col', (k,v) -> p.colorCol = v)
    return new Aes(p)
  }

  static Aes aes(String... colNames) {
    return new Aes(Arrays.asList(colNames))
  }

  static Aes aes(List<String> colNames) {
    return new Aes(colNames)
  }

  static Aes aes(List<String> colNames, String colour) {
    return new Aes(colNames, colour)
  }

  static CoordFlip coord_flip() {
    return new CoordFlip()
  }

  /**
   * @param theta: variable to map angle to (x or y)
   * @param start: Offset of starting point from 12 o'clock in radians.
   * Offset is applied clockwise or anticlockwise depending on value of direction.
   * @param direction: 1, clockwise; -1, anticlockwise
   * @param clip: Should drawing be clipped to the extent of the plot panel? A setting of "on"
   * (the default) means yes, and a setting of "off" means no.
   */
  static CoordPolar coord_polar(String theta = "x", BigDecimal start = 0, Integer direction = 1, String clip = "on") {
    return new CoordPolar(theta, start, 1 == direction, "on" == clip)
  }

  static CoordPolar coord_polar(Map params) {
    coord_polar(
            params.getOrDefault('theta', 'x') as String,
            params.getOrDefault('start', 0) as BigDecimal,
            params.getOrDefault('direction', 1) as Integer,
            params.getOrDefault('clip', 'on') as String
    )
  }

  static GeomAbline geom_abline() {
    return new GeomAbline()
  }

  static GeomBar geom_bar() {
    return new GeomBar()
  }

  static GeomBar geom_bar(Map params) {
    return new GeomBar(params)
  }

  static GeomBin2d geom_bin_2d() {
    return new GeomBin2d()
  }

  static GeomBlank geom_blank() {
    return new GeomBlank()
  }

  static GeomBoxplot geom_boxplot() {
    return new GeomBoxplot()
  }

  static GeomCol geom_col() {
    return new GeomCol()
  }

  static GeomContour geom_contour() {
    return new GeomContour()
  }

  static GeomContourFilled geom_contour_filled() {
    return new GeomContourFilled()
  }

  static GeomCount geom_count() {
    return new GeomCount()
  }

  static GeomHistogram geom_histogram() {
    return new GeomHistogram()
  }

  static GeomHline geom_hline() {
    return new GeomHline()
  }

  static GeomPoint geom_point() {
    return new GeomPoint()
  }

  static GeomPoint geom_point(Map params) {
    return new GeomPoint(params)
  }

  static GeomRug geom_rug(Map params) {
    return new GeomRug(params)
  }

  static GeomSegment geom_segment() {
    return new GeomSegment()
  }

  static GeomSmooth geom_smooth() {
    return new GeomSmooth()
  }

  static GeomSmooth geom_smooth(Map params) {
    return new GeomSmooth(params)
  }

  static GeomViolin geom_violin(Aes aes) {
    return new GeomViolin(aes)
  }

  static GeomVline geom_vline() {
    return new GeomVline()
  }

  static ScaleColorManual scale_color_manual(Map mappings) {
    return new ScaleColorManual(mappings)
  }

  static ScaleColorManual scale_colour_manual(Map mappings) {
    return scale_color_manual(mappings)
  }

  static StatBin2d stat_bin_2d() {
    return new StatBin2d()
  }

  static StatBoxplot stat_boxplot() {
    return new StatBoxplot()
  }

  static StatContour stat_contour() {
    return new StatContour()
  }

  static StatContourFilled stat_contour_filled() {
    return new StatContourFilled()
  }

  static StatCount stat_count() {
    return new StatCount()
  }

  static StatSum stat_summary(Map params) {
    return new StatSum(params)
  }

  static class As {

    static List factor(List column) {
      return ListConverter.toString(column)
    }
  }
  static Matrix map_data(String map, String region = null, boolean exact = false) {
    return Dataset.mapData(map, region, exact)
  }

}
