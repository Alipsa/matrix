package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.gg.GgChart

class ChartToSwing {

  /**
   * Create a {@link SvgPanel} from an SVG document represented as a string.
   *
   * @param svgChart the SVG content as an XML string
   * @return a {@link SvgPanel} displaying the provided SVG content
   */
  static SvgPanel export(String svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    new SvgPanel(svgChart)
  }

  /**
   * Create a {@link SvgPanel} from an {@link Svg} instance.
   * <p>
   * The {@code Svg} object is converted to its XML representation and
   * delegated to {@link #export(String)}.
   *
   * @param svgChart the {@link Svg} chart to render
   * @return a {@link SvgPanel} displaying the rendered SVG chart
   */
  static SvgPanel export(Svg svgChart) {
    export(svgChart.toXml())
  }

  /**
   * Create a {@link SvgPanel} from a {@link GgChart} instance.
   * <p>
   * The chart is rendered to SVG and delegated to {@link #export(String)}.
   *
   * @param chart the {@link GgChart} to render as SVG
   * @return a {@link SvgPanel} displaying the rendered chart
   */
  static SvgPanel export(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    export(chart.render().toXml())
  }
}
