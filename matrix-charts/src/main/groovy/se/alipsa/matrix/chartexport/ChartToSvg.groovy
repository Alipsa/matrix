package se.alipsa.matrix.chartexport

import groovy.transform.CompileDynamic

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.charm.PlotGrid

import java.nio.charset.StandardCharsets

/**
 * Exports charts as SVG files.
 *
 * <p>Accepts rendered {@link Svg} objects, Charm {@link CharmChart} instances,
 * and {@link PlotGrid} grids.</p>
 *
 * <pre>
 * // Export a Charm chart
 * ChartToSvg.export(charmChart, new File('chart.svg'))
 * </pre>
 */
@SuppressWarnings('DuplicateStringLiteral')
class ChartToSvg {

  /**
   * Export an already-rendered SVG as a file.
   *
   * @param svg the SVG to export
   * @param targetFile the file to write the SVG to
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if svg or targetFile is null
   */
  static void export(Svg svg, File targetFile) throws IOException {
    if (svg == null) {
      throw new IllegalArgumentException('svg cannot be null')
    }
    if (targetFile == null) {
      throw new IllegalArgumentException('targetFile cannot be null')
    }
    writeSvg(svg, targetFile)
  }

  /**
   * Export an already-rendered SVG to an {@link OutputStream}.
   *
   * @param svg the SVG to export
   * @param os the output stream to write the SVG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if svg or os is null
   */
  static void export(Svg svg, OutputStream os) throws IOException {
    if (svg == null) {
      throw new IllegalArgumentException('svg cannot be null')
    }
    if (os == null) {
      throw new IllegalArgumentException('outputStream cannot be null')
    }
    writeSvg(svg, os)
  }

  /**
   * Export an already-rendered SVG to a {@link Writer}.
   *
   * @param svg the SVG to export
   * @param writer the writer to write the SVG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if svg or writer is null
   */
  static void export(Svg svg, Writer writer) throws IOException {
    if (svg == null) {
      throw new IllegalArgumentException('svg cannot be null')
    }
    if (writer == null) {
      throw new IllegalArgumentException('writer cannot be null')
    }
    writeSvg(svg, writer)
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
      throw new IllegalArgumentException('chart cannot be null')
    }
    if (targetFile == null) {
      throw new IllegalArgumentException('targetFile cannot be null')
    }
    writeSvg(chart.render(), targetFile)
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
      throw new IllegalArgumentException('chart cannot be null')
    }
    if (os == null) {
      throw new IllegalArgumentException('outputStream cannot be null')
    }
    writeSvg(chart.render(), os)
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
      throw new IllegalArgumentException('chart cannot be null')
    }
    if (writer == null) {
      throw new IllegalArgumentException('writer cannot be null')
    }
    writeSvg(chart.render(), writer)
  }

  /**
   * Export a {@link PlotGrid} as an SVG file.
   *
   * @param grid the plot grid to export
   * @param targetFile the file to write the SVG to
   * @throws IllegalArgumentException if grid or targetFile is null
   */
  static void export(PlotGrid grid, File targetFile) throws IOException {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    if (targetFile == null) {
      throw new IllegalArgumentException('targetFile cannot be null')
    }
    writeSvg(grid.render(), targetFile)
  }

  /**
   * Export a {@link PlotGrid} as SVG to an {@link OutputStream}.
   *
   * @param grid the plot grid to export
   * @param os the output stream to write the SVG to
   * @throws IllegalArgumentException if grid or os is null
   */
  static void export(PlotGrid grid, OutputStream os) throws IOException {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    if (os == null) {
      throw new IllegalArgumentException('outputStream cannot be null')
    }
    writeSvg(grid.render(), os)
  }

  /**
   * Export a {@link PlotGrid} as SVG to a {@link Writer}.
   *
   * @param grid the plot grid to export
   * @param writer the writer to write the SVG to
   * @throws IllegalArgumentException if grid or writer is null
   */
  static void export(PlotGrid grid, Writer writer) throws IOException {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    if (writer == null) {
      throw new IllegalArgumentException('writer cannot be null')
    }
    writeSvg(grid.render(), writer)
  }

  private static void writeSvg(Svg svg, File targetFile) throws IOException {
    targetFile.parentFile?.mkdirs()
    targetFile.withWriter('UTF-8') { Writer writer ->
      writer.write(SvgWriter.toXmlPretty(svg))
      writer.flush()
    }
  }

  private static void writeSvg(Svg svg, OutputStream os) throws IOException {
    os.write(SvgWriter.toXml(svg).getBytes(StandardCharsets.UTF_8))
    os.flush()
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed overload.
   *
   * @param chart a chart object (CharmChart, PlotGrid, or Svg)
   * @param targetFile the file to write the SVG to
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static void export(Object chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    switch (chart) {
      case PlotGrid -> export(chart as PlotGrid, targetFile)
      case CharmChart -> export(chart as CharmChart, targetFile)
      case Svg -> export(chart as Svg, targetFile)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed overload.
   *
   * @param chart a chart object (CharmChart, PlotGrid, or Svg)
   * @param os the output stream to write the SVG to
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static void export(Object chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    switch (chart) {
      case PlotGrid -> export(chart as PlotGrid, os)
      case CharmChart -> export(chart as CharmChart, os)
      case Svg -> export(chart as Svg, os)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed overload.
   *
   * @param chart a chart object (CharmChart, PlotGrid, or Svg)
   * @param writer the writer to write the SVG to
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static void export(Object chart, Writer writer) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    switch (chart) {
      case PlotGrid -> export(chart as PlotGrid, writer)
      case CharmChart -> export(chart as CharmChart, writer)
      case Svg -> export(chart as Svg, writer)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  private static void writeSvg(Svg svg, Writer writer) throws IOException {
    writer.write(SvgWriter.toXmlPretty(svg))
    writer.flush()
  }

}
