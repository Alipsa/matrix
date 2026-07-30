package se.alipsa.matrix.xchart

import org.knowm.xchart.XChartPanel

import se.alipsa.matrix.xchart.abstractions.MatrixXChart

/** Convenience export facade for Matrix XChart wrappers. */
class Plot {

  /** Exports a chart as PNG to a file. */
  static void png(MatrixXChart chart, File file) { requireChart(chart).exportPng(requireFile(file)) }
  /** Exports a chart as PNG to an output stream. */
  static void png(MatrixXChart chart, OutputStream outputStream) { requireChart(chart).exportPng(requireOutputStream(outputStream)) }
  /** Exports a chart as SVG to a file. */
  static void svg(MatrixXChart chart, File file) { requireChart(chart).exportSvg(requireFile(file)) }
  /** Exports a chart as SVG to an output stream. */
  static void svg(MatrixXChart chart, OutputStream outputStream) { requireChart(chart).exportSvg(requireOutputStream(outputStream)) }
  /** Exports a chart as PDF to a file. */
  static void pdf(MatrixXChart chart, File file) { requireChart(chart).exportPdf(requireFile(file)) }
  /** Exports a chart as PDF to an output stream. */
  static void pdf(MatrixXChart chart, OutputStream outputStream) { requireChart(chart).exportPdf(requireOutputStream(outputStream)) }
  /** Returns a Swing panel containing the chart. */
  static XChartPanel swing(MatrixXChart chart) { requireChart(chart).exportSwing() }
  /** Opens the chart in a Swing window. */
  static void display(MatrixXChart chart) { requireChart(chart).display() }

  private static MatrixXChart requireChart(MatrixXChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    chart
  }
  private static File requireFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException('file cannot be null')
    }
    file
  }
  private static OutputStream requireOutputStream(OutputStream outputStream) {
    if (outputStream == null) {
      throw new IllegalArgumentException('output stream cannot be null')
    }
    outputStream
  }
}
