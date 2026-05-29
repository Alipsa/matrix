package se.alipsa.matrix.pict

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToJfx
import se.alipsa.matrix.chartexport.ChartToPng

import java.nio.file.Files
import javafx.scene.Group

/**
 * Primary export API for pict chart types (AreaChart, BarChart, BoxChart, BubbleChart,
 * Histogram, LineChart, PieChart, ScatterChart).
 *
 * <p>All methods delegate through {@link CharmBridge} for SVG-first rendering via Charm.
 * For direct Charm API usage, see {@link se.alipsa.matrix.charm.Charts}.</p>
 *
 * <p>Example:
 * <pre>
 * LineChart chart = LineChart.builder(data).title('Sales').x('month').y('revenue').build()
 * inout.view(Plot.jfx(chart))
 * Plot.png(chart, new File('chart.png'))
 * </pre>
 */
class Plot {

  /**
   * Exports a chart as a PNG image file.
   *
   * @param chart the chart to export
   * @param file target file
   * @param width image width in pixels
   * @param height image height in pixels
   * @throws IOException if file writing fails
   */
  static void png(Chart chart, File file, double width = 800, double height = 600) throws IOException {
    try (OutputStream os = Files.newOutputStream(file.toPath())) {
      png(chart, os, width, height)
    }
  }

  /**
   * Exports a chart as PNG to an output stream.
   *
   * @param chart the chart to export
   * @param os target output stream
   * @param width image width in pixels
   * @param height image height in pixels
   */
  static void png(Chart chart, OutputStream os, double width = 800, double height = 600) {
    Svg svg = CharmBridge.renderSvg(chart, width as int, height as int)
    ChartToPng.export(svg, os)
  }

  /**
   * Renders a chart to an SVG object.
   *
   * <p>Use this when you need the raw SVG for HTML embedding, further manipulation,
   * or writing to a file.</p>
   *
   * @param chart the chart to render
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @return the rendered SVG
   */
  static Svg svg(Chart chart, int width = 800, int height = 600) {
    CharmBridge.renderSvg(chart, width, height)
  }

  /**
   * Converts a chart to a JavaFX Node for display.
   *
   * <p><b>Breaking change:</b> Previously returned {@code javafx.scene.chart.Chart}.
   * Now returns {@code javafx.scene.Group} (an SVGImage extending Group).
   * Callers using {@code inout.view(Plot.jfx(chart))} are unaffected since
   * {@code view()} accepts {@code Node}.</p>
   *
   * @param chart the chart to convert
   * @return a JavaFX Node rendering the chart
   */
  static Group jfx(Chart chart) {
    ChartToJfx.export(chart)
  }

  /**
   * Exports a chart as a base64-encoded PNG data URI.
   *
   * @param chart the chart to export
   * @param width image width in pixels
   * @param height image height in pixels
   * @return data URI string
   */
  static String base64(Chart chart, double width = 800, double height = 600) {
    Svg svgChart = CharmBridge.renderSvg(chart, width as int, height as int)
    ChartToImage.base64(svgChart)
  }

}
