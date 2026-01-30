package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.export.SvgRenderer
import se.alipsa.matrix.gg.GgChart

class ChartToJpeg {

  static void export(Svg svgChart, File targetFile, BigDecimal quality = 1.0) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    SvgRenderer.toJpeg(svgChart, targetFile, [quality: quality])
  }

  static void export(GgChart chart, File targetFile, BigDecimal quality) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    export(chart.render(), targetFile, quality)
  }

}
