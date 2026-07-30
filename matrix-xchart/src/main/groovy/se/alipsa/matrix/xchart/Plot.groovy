package se.alipsa.matrix.xchart

import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.chartpart.Chart

import se.alipsa.matrix.xchart.abstractions.MatrixXChart

/** Convenience export facade for Matrix XChart wrappers. */
@SuppressWarnings('IfStatementBraces')
class Plot {

  static void png(MatrixXChart chart, File file) { requireChart(chart).exportPng(requireFile(file)) }
  static void png(MatrixXChart chart, OutputStream outputStream) { requireChart(chart).exportPng(requireOutputStream(outputStream)) }
  static void svg(MatrixXChart chart, File file) { requireChart(chart).exportSvg(requireFile(file)) }
  static void svg(MatrixXChart chart, OutputStream outputStream) { requireChart(chart).exportSvg(requireOutputStream(outputStream)) }
  static void pdf(MatrixXChart chart, File file) { requireChart(chart).exportPdf(requireFile(file)) }
  static void pdf(MatrixXChart chart, OutputStream outputStream) { requireChart(chart).exportPdf(requireOutputStream(outputStream)) }
  static XChartPanel<Chart> swing(MatrixXChart<Chart> chart) { requireChart(chart).exportSwing() }
  static void display(MatrixXChart chart) { requireChart(chart).display() }

  private static MatrixXChart requireChart(MatrixXChart chart) {
    if (chart == null) throw new IllegalArgumentException('chart cannot be null')
    chart
  }
  private static File requireFile(File file) {
    if (file == null) throw new IllegalArgumentException('file cannot be null')
    file
  }
  private static OutputStream requireOutputStream(OutputStream outputStream) {
    if (outputStream == null) throw new IllegalArgumentException('output stream cannot be null')
    outputStream
  }
}
