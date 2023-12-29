package se.alipsa.groovy.charts

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.groovy.charts.swing.SwingConverter

import java.nio.file.Files

class SwingPlot {

  static XChartPanel<? extends org.knowm.xchart.internal.chartpart.Chart<? extends Styler,? extends Series>> swing(Chart chart) {
    SwingConverter.convert(chart)
  }

  static void png(Chart chart, File file, double width, double height) throws IOException {
    try(OutputStream os = Files.newOutputStream(file.toPath())) {
      png(chart, os, width, height)
    }
  }

  static void png(Chart chart, OutputStream os, double width = 800, double height = 600) {
    def xchart = SwingConverter.convert(chart).getChart()

    //xchart.width = (int) width
    //xchart.height = (int)height
    BitmapEncoder.saveBitmap(xchart, os, BitmapEncoder.BitmapFormat.PNG)
  }

  static String base64(Chart chart, double width = 800, double height = 600) {
    def xchart = SwingConverter.convert(chart).getChart()
    byte[] bytes = BitmapEncoder.getBitmapBytes(xchart, BitmapEncoder.BitmapFormat.PNG)
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes)
  }
}
