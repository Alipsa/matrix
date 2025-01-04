package se.alipsa.matrix.charts.svg


import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charts.InitializationException

class SvgChart {
  protected Svg svg

  String asString(boolean prettyPrint = false) {
    if (svg == null) {
      throw new InitializationException("svg model is null, nothing to convert to String")
    }
    if (prettyPrint) {
      return SvgWriter.toXmlPretty(svg)
    }
    return SvgWriter.toXml(svg)
  }

  Svg getSvg() {
    return svg
  }
}
