package se.alipsa.matrix.chartexport

import org.girod.javafx.svgimage.SVGImage
import org.girod.javafx.svgimage.SVGLoader
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.gg.GgChart

class ChartToJfx {

  static SVGImage export(String svgChart) {
    SVGLoader.load(svgChart)
  }

  static SVGImage export(Svg chart) {
    SVGLoader.load(chart.toXml())
  }

  static SVGImage export(GgChart chart) {
    SVGLoader.load(chart.render().toXml())
  }
}
