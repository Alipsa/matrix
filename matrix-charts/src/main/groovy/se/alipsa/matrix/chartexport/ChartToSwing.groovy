package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.gg.GgChart

/**
 * Exports charts as Swing {@link SvgPanel} components.
 *
 * <p>Accepts SVG strings, {@link Svg} objects, {@link GgChart} instances,
 * and {@link CharmChart} instances. All paths converge through SVG rendering.</p>
 */
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
    new SvgPanel(svgChart)
  }

  /**
   * Create a {@link SvgPanel} from an {@link Svg} instance.
   *
   * @param svgChart the {@link Svg} chart to render
   * @return a {@link SvgPanel} displaying the rendered SVG chart
   */
  static SvgPanel export(Svg svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    export(svgChart.toXml())
  }

  /**
   * Create a {@link SvgPanel} from a {@link GgChart} instance.
   *
   * @param svgChart the {@link GgChart} to render as SVG
   * @return a {@link SvgPanel} displaying the rendered chart
   */
  static SvgPanel export(GgChart svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    export(svgChart.render())
  }

  /**
   * Create a {@link SvgPanel} from a Charm {@link CharmChart} instance.
   *
   * @param chart the Charm chart to render as SVG
   * @return a {@link SvgPanel} displaying the rendered chart
   */
  static SvgPanel export(CharmChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    export(chart.render())
  }

  /**
   * Dynamic dispatch overload for untyped chart objects.
   *
   * @param svgChart an SVG string, {@link Svg}, {@link GgChart}, or {@link CharmChart}
   * @return a {@link SvgPanel} displaying the rendered chart
   * @throws IllegalArgumentException if svgChart is null or an unsupported type
   */
  static SvgPanel export(Object svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    if (svgChart instanceof CharSequence) {
      return export(String.valueOf(svgChart))
    }
    if (svgChart instanceof Svg) {
      return export((Svg) svgChart)
    }
    if (svgChart instanceof GgChart) {
      return export((GgChart) svgChart)
    }
    if (svgChart instanceof CharmChart) {
      return export((CharmChart) svgChart)
    }
    throw new IllegalArgumentException("Unsupported chart type: ${svgChart.getClass().name}")
  }
}
