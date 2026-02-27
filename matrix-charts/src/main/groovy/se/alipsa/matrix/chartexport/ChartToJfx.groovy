package se.alipsa.matrix.chartexport

import org.girod.javafx.svgimage.SVGImage
import org.girod.javafx.svgimage.SVGLoader
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.pictura.Chart
import se.alipsa.matrix.pictura.CharmBridge

/**
 * Exports charts as JavaFX {@link SVGImage} nodes.
 *
 * <p>Accepts SVG strings, {@link Svg} objects,
 * and {@link CharmChart} instances. All paths converge through SVG rendering.</p>
 *
 * <p>For GgPlot export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggplot.</p>
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
    SVGLoader.load(svgChart)
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
    SVGLoader.load(chart.toXml())
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

  /**
   * Create a JavaFX {@link SVGImage} from a legacy {@link Chart} (e.g. BarChart, ScatterChart).
   *
   * @param chart the legacy chart to render and convert
   * @return an {@link SVGImage} representing the rendered chart
   * @throws RuntimeException if rendering or SVG loading fails
   */
  static SVGImage export(Chart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    export(CharmBridge.convert(chart).render())
  }
}
