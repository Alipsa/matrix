package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.export.SvgRenderer
import se.alipsa.matrix.charm.Chart as CharmChart

import java.awt.image.BufferedImage

/**
 * Exports charts as in-memory {@link BufferedImage} objects.
 *
 * <p>Accepts {@link Svg} objects and {@link CharmChart} instances.
 * All paths converge through SVG rendering.</p>
 *
 * <p>For GgChart export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggplot.</p>
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
}
