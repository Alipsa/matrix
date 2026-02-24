package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.charts.Chart
import se.alipsa.matrix.charts.CharmBridge

/**
 * Exports charts as SVG files.
 *
 * <p>Accepts legacy {@link Chart} instances (e.g. ScatterChart, BarChart)
 * and Charm {@link CharmChart} instances. Legacy charts are converted
 * via {@link CharmBridge} before rendering.</p>
 *
 * <pre>
 * // Export a legacy chart
 * ChartToSvg.export(scatterChart, new File('chart.svg'))
 *
 * // Export a Charm chart
 * ChartToSvg.export(charmChart, new File('chart.svg'))
 * </pre>
 */
class ChartToSvg {

  /**
   * Export a legacy chart as an SVG file.
   *
   * @param chart the chart to export
   * @param targetFile the file to write the SVG to
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void export(Chart chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    CharmBridge.convert(chart).writeTo(targetFile)
  }

  /**
   * Export a legacy chart as SVG to an {@link OutputStream}.
   * The caller is responsible for closing the OutputStream.
   *
   * @param chart the chart to export
   * @param os the output stream to write the SVG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if chart or os is null
   */
  static void export(Chart chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    String svg = SvgWriter.toXmlPretty(CharmBridge.convert(chart).render())
    os.write(svg.getBytes('UTF-8'))
    os.flush()
  }

  /**
   * Export a legacy chart as SVG to a {@link Writer}.
   * The caller is responsible for closing the Writer.
   *
   * @param chart the chart to export
   * @param writer the writer to write the SVG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if chart or writer is null
   */
  static void export(Chart chart, Writer writer) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (writer == null) {
      throw new IllegalArgumentException("writer cannot be null")
    }
    writer.write(SvgWriter.toXmlPretty(CharmBridge.convert(chart).render()))
    writer.flush()
  }
}
