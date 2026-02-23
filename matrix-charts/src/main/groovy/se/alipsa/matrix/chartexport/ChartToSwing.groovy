package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart

/**
 * Exports charts as Swing {@link SvgPanel} components.
 *
 * <p>Accepts SVG strings, {@link Svg} objects,
 * and {@link CharmChart} instances. All paths converge through SVG rendering.</p>
 *
 * <p>For GgChart export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggcharts.</p>
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
   * @param svgChart an SVG string, {@link Svg}, or {@link CharmChart}
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
    if (svgChart instanceof CharmChart) {
      return export((CharmChart) svgChart)
    }
    throw new IllegalArgumentException("Unsupported chart type: ${svgChart.getClass().name}")
  }
}
