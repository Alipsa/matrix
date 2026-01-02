package se.alipsa.matrix.charts

import se.alipsa.matrix.charts.jfx.JfxConverter
import se.alipsa.matrix.charts.png.PngConverter

import java.nio.file.Files

class Plot {

  static void png(Chart chart, File file, double width = 800, double height = 600) throws IOException {
    try(OutputStream os = Files.newOutputStream(file.toPath())) {
      png(chart, os, width, height)
    }
  }

  static void png(Chart chart, OutputStream os, double width = 800, double height = 600) {
    PngConverter.convert(chart, os, width, height)
  }

  static javafx.scene.chart.Chart jfx(Chart chart) {
    JfxConverter.convert(chart)
  }

  static String base64(Chart chart, double width = 800, double height = 600) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    png(chart, baos, width, height)
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray())
  }

}
