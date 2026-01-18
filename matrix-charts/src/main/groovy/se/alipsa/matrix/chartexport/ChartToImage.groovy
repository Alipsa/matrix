package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.export.SvgRenderer
import se.alipsa.matrix.gg.GgChart

import java.awt.image.BufferedImage

class ChartToImage {

  static BufferedImage export(Svg svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    return SvgRenderer.toBufferedImage(svgChart)
  }

  static BufferedImage export(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    return export(chart.render())
  }
}
