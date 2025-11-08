package se.alipsa.matrix.charts

import se.alipsa.matrix.charts.jfx.JfxConverter
import se.alipsa.matrix.charts.png.PngConverter

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.util.Base64

/**
 * Entry points for rendering charts to different output formats.
 */
class Plot {

  /**
   * Write the supplied chart to a PNG file.
   *
   * @param chart the chart to render
   * @param file the destination file
   * @param width the desired chart width in pixels
   * @param height the desired chart height in pixels
   * @throws IOException if the file cannot be written
   */
  static void png(Chart chart, File file, double width = 800, double height = 600) throws IOException {
    OutputStream os = Files.newOutputStream(file.toPath())
    try {
      png(chart, os, width, height)
    } finally {
      os.close()
    }
  }

  /**
   * Write the supplied chart to a PNG stream.
   *
   * @param chart the chart to render
   * @param os the destination stream
   * @param width the desired chart width in pixels
   * @param height the desired chart height in pixels
   */
  static void png(Chart chart, OutputStream os, double width = 800, double height = 600) {
    PngConverter.convert(chart, os, width, height)
  }

  /**
   * Convert the chart to its JavaFX representation.
   *
   * @param chart the chart to convert
   * @return the JavaFX chart instance
   */
  static javafx.scene.chart.Chart jfx(Chart chart) {
    JfxConverter.convert(chart)
  }

  /**
   * Render the chart to a Base64 encoded PNG data URI.
   *
   * @param chart the chart to render
   * @param width the desired chart width in pixels
   * @param height the desired chart height in pixels
   * @return the Base64 encoded PNG data URI
   */
  static String base64(Chart chart, double width = 800, double height = 600) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    png(chart, baos, width, height)
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray())
  }
}
