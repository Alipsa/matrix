package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.gg.GgChart

class ChartToSwing {

  static SvgPanel export(String svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null");
    }
    new SvgPanel(svgChart)
  }

  static SvgPanel export(Svg svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null");
    }
    export(svgChart.toXml())
  }

  static SvgPanel export(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null");
    }
    export(chart.render().toXml())
  }
}
