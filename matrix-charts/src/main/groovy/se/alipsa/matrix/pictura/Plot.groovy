package se.alipsa.matrix.pictura

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.chartexport.ChartToJfx

import java.nio.file.Files

/**
 * Exports legacy chart types to PNG, base64, and JavaFX formats.
 *
 * <p>All methods now delegate through Charm for SVG-first rendering.
 * For direct Charm API usage, see {@link se.alipsa.matrix.charm.Charts}.</p>
 *
 * @deprecated Use the Charm API ({@link se.alipsa.matrix.charm.Charts#plot}) directly for
 * new code. This class is retained for backward compatibility with existing chart type factories.
 */
@Deprecated
@CompileStatic
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
   * Converts a chart to a JavaFX Node for display.
   *
   * <p><b>Breaking change:</b> Previously returned {@code javafx.scene.chart.Chart}.
   * Now returns {@code javafx.scene.Node} (an SVGImage extending Group).
   * Callers using {@code inout.view(Plot.jfx(chart))} are unaffected since
   * {@code view()} accepts {@code Node}.</p>
   *
   * @param chart the chart to convert
   * @return a JavaFX Node rendering the chart
   */
  static javafx.scene.Node jfx(Chart chart) {
    Svg svg = CharmBridge.convert(chart).render()
    ChartToJfx.export(svg)
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
