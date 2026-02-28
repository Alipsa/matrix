package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.export.SvgRenderer
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.pict.Chart
import se.alipsa.matrix.pict.CharmBridge

import java.awt.image.BufferedImage

/**
 * Exports charts as in-memory image representations ({@link BufferedImage} or base64 data URI).
 *
 * <p>Accepts {@link Svg} objects, {@link CharmChart} instances,
 * and legacy {@link Chart} objects. All paths converge through SVG rendering.</p>
 *
 * <p>For GgPlot export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggplot.</p>
 */
class ChartToImage {

  /**
   * Export an {@link Svg} chart to a {@link BufferedImage}.
   *
   * @param svgChart the {@link Svg} object containing the chart
   * @return rendered image
   * @throws IllegalArgumentException if svgChart is null
   */
  static BufferedImage export(Svg svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart must not be null")
    }
    return SvgRenderer.toBufferedImage(svgChart)
  }

  /**
   * Export a Charm {@link CharmChart} to a {@link BufferedImage}.
   *
   * @param chart the Charm chart to export
   * @return rendered image
   * @throws IllegalArgumentException if chart is null
   */
  static BufferedImage export(CharmChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    return export(chart.render())
  }

  /**
   * Export a legacy {@link Chart} (e.g. BarChart, ScatterChart) to a {@link BufferedImage}.
   *
   * @param chart the legacy chart to export
   * @return rendered image
   * @throws IllegalArgumentException if chart is null
   */
  static BufferedImage export(Chart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    return export(CharmBridge.convert(chart).render())
  }

  /**
   * Export an {@link Svg} chart as a base64-encoded PNG data URI.
   *
   * @param svgChart the {@link Svg} object containing the chart
   * @return data URI string (e.g. "data:image/png;base64,iVBOR...")
   * @throws IllegalArgumentException if svgChart is null
   */
  static String base64(Svg svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart cannot be null")
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToPng.export(svgChart, baos)
    "data:image/png;base64," + Base64.encoder.encodeToString(baos.toByteArray())
  }

  /**
   * Export a Charm {@link CharmChart} as a base64-encoded PNG data URI.
   *
   * @param chart the Charm chart to export
   * @return data URI string (e.g. "data:image/png;base64,iVBOR...")
   * @throws IllegalArgumentException if chart is null
   */
  static String base64(CharmChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    base64(chart.render())
  }

  /**
   * Export a legacy {@link Chart} as a base64-encoded PNG data URI.
   *
   * @param chart the legacy chart to export
   * @return data URI string (e.g. "data:image/png;base64,iVBOR...")
   * @throws IllegalArgumentException if chart is null
   */
  static String base64(Chart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    base64(CharmBridge.convert(chart).render())
  }
}
