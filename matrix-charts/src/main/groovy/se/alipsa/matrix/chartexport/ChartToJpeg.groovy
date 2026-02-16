package se.alipsa.matrix.chartexport

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.export.SvgRenderer
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.gg.GgChart

/**
 * Exports charts as JPEG images.
 *
 * <p>Accepts {@link Svg} objects, {@link GgChart} instances,
 * and {@link CharmChart} instances. All paths converge through SVG rendering.</p>
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
   * Export a {@link GgChart} as a JPEG image file.
   *
   * @param chart the {@link GgChart} to export
   * @param targetFile the {@link File} where the JPEG image will be written
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void export(GgChart chart, File targetFile, BigDecimal quality) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(chart.render(), targetFile, quality)
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
}
