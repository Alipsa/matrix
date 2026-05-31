package se.alipsa.matrix.pict

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToJfx
import se.alipsa.matrix.chartexport.ChartToJpeg
import se.alipsa.matrix.chartexport.ChartToPdf
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.SvgPanel

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
  private static final String FILE_CANNOT_BE_NULL = 'file cannot be null'
  private static final String OUTPUT_STREAM_CANNOT_BE_NULL = 'output stream cannot be null'
  private static final String WRITER_CANNOT_BE_NULL = 'writer cannot be null'

  /**
   * Exports a chart as a PNG image file.
   *
   * @param chart the chart to export
   * @param file target file
   * @param width image width in pixels
   * @param height image height in pixels
   * @throws IllegalArgumentException if chart or file is null
   * @throws IOException if file writing fails
   */
  static void png(Chart chart, File file, double width = 800, double height = 600) throws IOException {
    requireChart(chart)
    try (OutputStream os = Files.newOutputStream(requireFile(file).toPath())) {
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
   * @throws IllegalArgumentException if chart or output stream is null
   */
  static void png(Chart chart, OutputStream os, double width = 800, double height = 600) {
    requireChart(chart)
    requireOutputStream(os)
    Svg svg = CharmBridge.renderSvg(chart, width as int, height as int)
    ChartToPng.export(svg, os)
  }

  /**
   * Export a {@link Chart} (e.g. BarChart, ScatterChart) to a JPEG image file.
   *
   * @param chart the legacy chart to export
   * @param targetFile the {@link File} where the JPEG image will be written
   * @param quality JPEG compression quality (0.0 to 1.0), default to 1.0 (maximum quality)
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void jpg(Chart chart, File targetFile, BigDecimal quality = 1.0) {
    requireChart(chart)
    requireFile(targetFile)
    ChartToJpeg.export(CharmBridge.convert(chart), targetFile, quality)
  }

  /**
   * Export a legacy {@link Chart} as JPEG to an {@link OutputStream}.
   *
   * @param chart the legacy chart to export
   * @param os the output stream to write the JPEG to
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if chart or os is null
   */
  static void jpg(Chart chart, OutputStream os, BigDecimal quality = 1.0) {
    requireChart(chart)
    requireOutputStream(os)
    ChartToJpeg.export(CharmBridge.convert(chart), os, quality)
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
   * @throws IllegalArgumentException if chart is null
  */
  static Svg svg(Chart chart, int width = 800, int height = 600) {
    CharmBridge.renderSvg(requireChart(chart), width, height)
  }

  /**
   * Exports a chart as an SVG file.
   *
   * @param chart the chart to export
   * @param file target file
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @throws IllegalArgumentException if chart or file is null
   * @throws IOException if file writing fails
   */
  static void svg(Chart chart, File file, int width = 800, int height = 600) throws IOException {
    requireChart(chart)
    try (OutputStream os = Files.newOutputStream(requireFile(file).toPath())) {
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
   * @throws IllegalArgumentException if chart or output stream is null
   * @throws IOException if writing fails
   */
  static void svg(Chart chart, OutputStream os, int width = 800, int height = 600) throws IOException {
    requireChart(chart)
    requireOutputStream(os)
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
   * @throws IllegalArgumentException if chart or writer is null
   * @throws IOException if writing fails
   */
  static void svg(Chart chart, Writer writer, int width = 800, int height = 600) throws IOException {
    requireChart(chart)
    requireWriter(writer)
    writer.write(SvgWriter.toXml(svg(chart, width, height)))
    writer.flush()
  }

  /**
   * Export a {@link Chart} to a PDF file.
   *
   * @param chart the chart to export
   * @param targetFile the PDF file to write
   */
  static void pdf(Chart chart, File targetFile) throws IOException {
    requireChart(chart)
    requireFile(targetFile)
    ChartToPdf.export(CharmBridge.convert(chart), targetFile)
  }

  /**
   * Export a {@link Chart} to a PDF file.
   *
   * @param chart the chart to export
   * @param os the output stream to write
   */
  static void pdf(Chart chart, OutputStream os) throws IOException {
    requireChart(chart)
    requireOutputStream(os)
    ChartToPdf.export(CharmBridge.convert(chart), os)
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
   * @throws IllegalArgumentException if chart is null
   */
  static Group jfx(Chart chart) {
    requireChart(chart)
    ChartToJfx.export(CharmBridge.convert(chart))
  }

  /**
   * Create a {@link se.alipsa.matrix.chartexport.SvgPanel} from a legacy {@link Chart} (e.g. BarChart, ScatterChart).
   *
   * @param chart the legacy chart to render
   * @return a {@link se.alipsa.matrix.chartexport.SvgPanel} displaying the rendered chart
   */
  static SvgPanel swing(Chart chart) {
    requireChart(chart)
    ChartToSwing.export(CharmBridge.convert(chart).render())
  }

  /**
   * Exports a chart as a base64-encoded PNG data URI.
   *
   * @param chart the chart to export
   * @param width image width in pixels
   * @param height image height in pixels
   * @return data URI string
   * @throws IllegalArgumentException if chart is null
   */
  static String base64(Chart chart, double width = 800, double height = 600) {
    Svg svg = CharmBridge.renderSvg(requireChart(chart), width as int, height as int)
    ChartToImage.base64(svg)
  }

  private static Chart requireChart(Chart chart) {
    if (chart == null) {
      throw new IllegalArgumentException(CHART_CANNOT_BE_NULL)
    }
    chart
  }

  private static File requireFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException(FILE_CANNOT_BE_NULL)
    }
    file
  }

  private static OutputStream requireOutputStream(OutputStream os) {
    if (os == null) {
      throw new IllegalArgumentException(OUTPUT_STREAM_CANNOT_BE_NULL)
    }
    os
  }

  private static Writer requireWriter(Writer writer) {
    if (writer == null) {
      throw new IllegalArgumentException(WRITER_CANNOT_BE_NULL)
    }
    writer
  }

}
