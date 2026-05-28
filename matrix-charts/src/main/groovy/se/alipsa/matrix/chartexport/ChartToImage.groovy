package se.alipsa.matrix.chartexport

import groovy.transform.CompileDynamic

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.export.SvgRenderer
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.charm.PlotGrid
import se.alipsa.matrix.pict.CharmBridge
import se.alipsa.matrix.pict.Chart

import java.awt.image.BufferedImage

/**
 * Exports charts as in-memory image representations ({@link BufferedImage} or base64 data URI).
 *
 * <p>Accepts {@link Svg} objects, {@link CharmChart} instances,
 * and legacy {@link Chart} objects. All paths converge through SVG rendering.</p>
 *
 * <p>For GgPlot export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggplot.</p>
 */
@SuppressWarnings('DuplicateStringLiteral')
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
      throw new IllegalArgumentException('svgChart must not be null')
    }
    return SvgRenderer.toBufferedImage(AnimationCssStripper.stripFromSvg(svgChart))
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
      throw new IllegalArgumentException('chart must not be null')
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
      throw new IllegalArgumentException('chart cannot be null')
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
      throw new IllegalArgumentException('svgChart cannot be null')
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToPng.export(svgChart, baos)
    'data:image/png;base64,' + Base64.encoder.encodeToString(baos.toByteArray())
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
      throw new IllegalArgumentException('chart cannot be null')
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
      throw new IllegalArgumentException('chart cannot be null')
    }
    base64(CharmBridge.convert(chart).render())
  }

  /**
   * Export a {@link PlotGrid} to a {@link BufferedImage}.
   *
   * @param grid the plot grid to export
   * @return rendered image
   * @throws IllegalArgumentException if grid is null
   */
  static BufferedImage export(PlotGrid grid) {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    export(grid.render())
  }

  /**
   * Export a {@link PlotGrid} as a base64-encoded PNG data URI.
   *
   * @param grid the plot grid to export
   * @return data URI string (e.g. "data:image/png;base64,iVBOR...")
   * @throws IllegalArgumentException if grid is null
   */
  static String base64(PlotGrid grid) {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    base64(grid.render())
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed overload.
   *
   * @param chart a chart object (CharmChart, Chart, PlotGrid, or Svg)
   * @return rendered image
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static BufferedImage export(Object chart) {
    if (chart == null) {
      throw new IllegalArgumentException('chart must not be null')
    }
    switch (chart) {
      case PlotGrid -> export(chart as PlotGrid)
      case CharmChart -> export(chart as CharmChart)
      case Chart -> export(chart as Chart)
      case Svg -> export(chart as Svg)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed base64 overload.
   *
   * @param chart a chart object (CharmChart, Chart, PlotGrid, or Svg)
   * @return data URI string (e.g. "data:image/png;base64,iVBOR...")
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static String base64(Object chart) {
    if (chart == null) {
      throw new IllegalArgumentException('chart must not be null')
    }
    switch (chart) {
      case PlotGrid -> base64(chart as PlotGrid)
      case CharmChart -> base64(chart as CharmChart)
      case Chart -> base64(chart as Chart)
      case Svg -> base64(chart as Svg)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

}
