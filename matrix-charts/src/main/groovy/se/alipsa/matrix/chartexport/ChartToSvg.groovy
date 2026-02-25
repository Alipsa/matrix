package se.alipsa.matrix.chartexport

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
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
@CompileStatic
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
    writeSvg(CharmBridge.convert(chart).render(), targetFile)
  }

  /**
   * Export a Charm chart as an SVG file.
   *
   * @param chart the Charm chart to export
   * @param targetFile the file to write the SVG to
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void export(CharmChart chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    writeSvg(chart.render(), targetFile)
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
    writeSvg(CharmBridge.convert(chart).render(), os)
  }

  /**
   * Export a Charm chart as SVG to an {@link OutputStream}.
   * The caller is responsible for closing the OutputStream.
   *
   * @param chart the Charm chart to export
   * @param os the output stream to write the SVG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if chart or os is null
   */
  static void export(CharmChart chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    writeSvg(chart.render(), os)
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
    writeSvg(CharmBridge.convert(chart).render(), writer)
  }

  /**
   * Export a Charm chart as SVG to a {@link Writer}.
   * The caller is responsible for closing the Writer.
   *
   * @param chart the Charm chart to export
   * @param writer the writer to write the SVG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if chart or writer is null
   */
  static void export(CharmChart chart, Writer writer) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (writer == null) {
      throw new IllegalArgumentException("writer cannot be null")
    }
    writeSvg(chart.render(), writer)
  }

  private static void writeSvg(Svg svg, File targetFile) throws IOException {
    String xml = SvgWriter.toXmlPretty(svg)
    targetFile.withWriter('UTF-8') { Writer writer ->
      writer.write(xml)
      writer.flush()
    }
  }

  private static void writeSvg(Svg svg, OutputStream os) throws IOException {
    String xml = SvgWriter.toXmlPretty(svg)
    os.write(xml.getBytes('UTF-8'))
    os.flush()
  }

  private static void writeSvg(Svg svg, Writer writer) throws IOException {
    writer.write(SvgWriter.toXmlPretty(svg))
    writer.flush()
  }
}
