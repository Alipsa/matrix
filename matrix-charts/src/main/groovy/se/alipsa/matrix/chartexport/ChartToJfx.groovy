package se.alipsa.matrix.chartexport

import org.girod.javafx.svgimage.SVGImage
import org.girod.javafx.svgimage.SVGLoader
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.gg.GgChart

/**
 * Exports charts as JavaFX {@link SVGImage} nodes.
 *
 * <p>Accepts SVG strings, {@link Svg} objects, {@link GgChart} instances,
 * and {@link CharmChart} instances. All paths converge through SVG rendering.</p>
 */
class ChartToJfx {

  /**
   * Create a JavaFX {@link SVGImage} from an SVG document provided as a {@link String}.
   *
   * @param svgChart the SVG content as a string
   * @return an {@link SVGImage} representing the provided SVG content
   * @throws RuntimeException if the SVG content cannot be parsed or loaded
   */
  static SVGImage export(String svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    return SVGLoader.load(svgChart)
  }

  /**
   * Create a JavaFX {@link SVGImage} from a {@link Svg} chart.
   *
   * @param chart the {@link Svg} chart to convert
   * @return an {@link SVGImage} representing the rendered chart
   * @throws RuntimeException if the SVG representation of the chart cannot be parsed or loaded
   */
  static SVGImage export(Svg chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    return SVGLoader.load(chart.toXml())
  }

  /**
   * Create a JavaFX {@link SVGImage} from a {@link GgChart}.
   * The chart is rendered to SVG before being converted to an {@link SVGImage}.
   *
   * @param chart the {@link GgChart} to render and convert
   * @return an {@link SVGImage} representing the rendered chart
   * @throws RuntimeException if rendering or SVG loading fails
   */
  static SVGImage export(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    SVGLoader.load(chart.render().toXml())
  }

  /**
   * Create a JavaFX {@link SVGImage} from a Charm {@link CharmChart}.
   * The chart is rendered to SVG before being converted to an {@link SVGImage}.
   *
   * @param chart the Charm chart to render and convert
   * @return an {@link SVGImage} representing the rendered chart
   * @throws RuntimeException if rendering or SVG loading fails
   */
  static SVGImage export(CharmChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    export(chart.render())
  }
}
