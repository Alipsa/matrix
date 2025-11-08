package se.alipsa.matrix.charts.swing

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.charts.Chart

import java.nio.file.Files

class SwingPlot {

  /**
   * Convert a chart to an XChart panel for use in Swing applications.
   *
   * @param chart the chart to convert
   * @return the Swing {@link XChartPanel}
   */
  static XChartPanel<? extends org.knowm.xchart.internal.chartpart.Chart<? extends Styler, ? extends Series>> swing(Chart chart) {
    SwingConverter.convert(chart)
  }

  /**
   * Write the chart to a PNG file using the Swing renderer.
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
   * Write the chart to a PNG stream using the Swing renderer.
   *
   * @param chart the chart to render
   * @param os the destination stream
   * @param width the desired chart width in pixels
   * @param height the desired chart height in pixels
   */
  static void png(Chart chart, OutputStream os, double width = 800, double height = 600) {
    def xchart = SwingConverter.convert(chart).getChart()

    //xchart.width = (int) width
    //xchart.height = (int)height
    BitmapEncoder.saveBitmap(xchart, os, BitmapEncoder.BitmapFormat.PNG)
  }

  /**
   * Render the chart to a Base64 encoded PNG data URI using the Swing renderer.
   *
   * @param chart the chart to render
   * @param width the desired chart width in pixels
   * @param height the desired chart height in pixels
   * @return the Base64 encoded PNG data URI
   */
  static String base64(Chart chart, double width = 800, double height = 600) {
    def xchart = SwingConverter.convert(chart).getChart()
    byte[] bytes = BitmapEncoder.getBitmapBytes(xchart, BitmapEncoder.BitmapFormat.PNG)
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes)
  }
}
