package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.export.SvgRenderer
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.pictura.Chart
import se.alipsa.matrix.pictura.CharmBridge

/**
 * Exports charts as JPEG images.
 *
 * <p>Accepts {@link Svg} objects and {@link CharmChart} instances.
 * All paths converge through SVG rendering.</p>
 *
 * <p>For GgPlot export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggplot.</p>
 */
class ChartToJpeg {

  /**
   * Export an {@link Svg} chart as a JPEG image file.
   *
   * @param svgChart the {@link Svg} object containing the chart
   * @param targetFile the {@link File} where the JPEG image will be written
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if svgChart or targetFile is null
   */
  static void export(Svg svgChart, File targetFile, BigDecimal quality = 1.0) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    SvgRenderer.toJpeg(svgChart, targetFile, [quality: quality])
  }

  /**
   * Export a Charm {@link CharmChart} as a JPEG image file.
   *
   * @param chart the Charm chart to export
   * @param targetFile the {@link File} where the JPEG image will be written
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void export(CharmChart chart, File targetFile, BigDecimal quality = 1.0) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(chart.render(), targetFile, quality)
  }

  /**
   * Export a legacy {@link Chart} (e.g. BarChart, ScatterChart) as a JPEG image file.
   *
   * @param chart the legacy chart to export
   * @param targetFile the {@link File} where the JPEG image will be written
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void export(Chart chart, File targetFile, BigDecimal quality = 1.0) {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(CharmBridge.convert(chart).render(), targetFile, quality)
  }

  /**
   * Export an {@link Svg} chart as JPEG to an {@link OutputStream}.
   *
   * @param svgChart the {@link Svg} object containing the chart
   * @param os the output stream to write the JPEG to
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if svgChart or os is null
   */
  static void export(Svg svgChart, OutputStream os, BigDecimal quality = 1.0) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    SvgRenderer.toJpeg(svgChart, os, [quality: quality])
  }

  /**
   * Export a Charm {@link CharmChart} as JPEG to an {@link OutputStream}.
   *
   * @param chart the Charm chart to export
   * @param os the output stream to write the JPEG to
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if chart or os is null
   */
  static void export(CharmChart chart, OutputStream os, BigDecimal quality = 1.0) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    export(chart.render(), os, quality)
  }

  /**
   * Export a legacy {@link Chart} as JPEG to an {@link OutputStream}.
   *
   * @param chart the legacy chart to export
   * @param os the output stream to write the JPEG to
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if chart or os is null
   */
  static void export(Chart chart, OutputStream os, BigDecimal quality = 1.0) {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    export(CharmBridge.convert(chart).render(), os, quality)
  }
}
