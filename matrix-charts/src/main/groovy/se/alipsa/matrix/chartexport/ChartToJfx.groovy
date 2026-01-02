package se.alipsa.matrix.chartexport

import org.girod.javafx.svgimage.SVGImage
import org.girod.javafx.svgimage.SVGLoader
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.gg.GgChart

class ChartToJfx {

  static SVGImage export(String svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    SVGLoader.load(svgChart)
  }

  static SVGImage export(Svg chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    SVGLoader.load(chart.toXml())
  }

  static SVGImage export(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    SVGLoader.load(chart.render().toXml())
  }
}
