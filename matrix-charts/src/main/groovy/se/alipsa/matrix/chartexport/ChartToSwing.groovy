package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.gg.GgChart

class ChartToSwing {

  static SvgPanel export(String svgChart) {
    new SvgPanel(svgChart)
  }

  static SvgPanel export(Svg svgChart) {
    export(svgChart.toXml())
  }

  static SvgPanel export(GgChart chart) {
    export(chart.render().toXml())
  }
}
