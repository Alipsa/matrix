package se.alipsa.matrix.pict

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToJfx
import se.alipsa.matrix.chartexport.ChartToPng

import java.nio.charset.StandardCharsets
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
 * javafx.scene.Node node = Plot.jfx(chart)
 * Plot.png(chart, new File('chart.png'))
 * </pre>
 */
class Plot {

  private static final String CHART_CANNOT_BE_NULL = 'chart cannot be null'

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
    if (chart == null) {
      throw new IllegalArgumentException(CHART_CANNOT_BE_NULL)
    }
    CharmBridge.renderSvg(chart, width, height)
  }

  /**
   * Exports a chart as an SVG file.
   *
   * @param chart the chart to export
   * @param file target file
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @throws IOException if file writing fails
   */
  static void svg(Chart chart, File file, int width = 800, int height = 600) throws IOException {
    try (OutputStream os = Files.newOutputStream(file.toPath())) {
      svg(chart, os, width, height)
    }
  }

  /**
   * Exports a chart as SVG to an output stream.
   * The method flushes the output stream after writing. The caller is responsible
   * for closing the output stream.
   *
   * @param chart the chart to export
   * @param os target output stream
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @throws IOException if writing fails
   */
  static void svg(Chart chart, OutputStream os, int width = 800, int height = 600) throws IOException {
    os.write(SvgWriter.toXml(svg(chart, width, height)).getBytes(StandardCharsets.UTF_8))
    os.flush()
  }

  /**
   * Exports a chart as SVG to a writer.
   * The method flushes the writer after writing. The caller is responsible for
   * closing the writer.
   *
   * @param chart the chart to export
   * @param writer target writer
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @throws IOException if writing fails
   */
  static void svg(Chart chart, Writer writer, int width = 800, int height = 600) throws IOException {
    writer.write(SvgWriter.toXml(svg(chart, width, height)))
    writer.flush()
  }

  /**
   * Converts a chart to a JavaFX Node for display.
   *
   * <p><b>Breaking change:</b> Previously returned {@code javafx.scene.chart.Chart}.
   * Now returns {@code javafx.scene.Group} (an SVGImage extending Group).
   * Use the returned {@code Node} in a JavaFX scene graph.</p>
   *
   * @param chart the chart to convert
   * @return a JavaFX Node rendering the chart
   */
  static Group jfx(Chart chart) {
    if (chart == null) {
      throw new IllegalArgumentException(CHART_CANNOT_BE_NULL)
    }
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
    Svg svg = CharmBridge.renderSvg(chart, width as int, height as int)
    ChartToImage.base64(svg)
  }

}
