package se.alipsa.matrix.chartexport

import groovy.transform.CompileDynamic

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.charm.PlotGrid

/**
 * Exports charts as Swing {@link SvgPanel} components.
 *
 * <p>Accepts SVG strings, {@link Svg} objects,
 * and {@link CharmChart} instances. All paths converge through SVG rendering.</p>
 *
 * <p>For GgPlot export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggplot.</p>
 */
@SuppressWarnings('DuplicateStringLiteral')
class ChartToSwing {

  /**
   * Create a {@link SvgPanel} from an SVG document represented as a string.
   *
   * @param svgChart the SVG content as an XML string
   * @return a {@link SvgPanel} displaying the provided SVG content
   */
  static SvgPanel export(String svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException('svgChart must not be null')
    }
    new SvgPanel(svgChart)
  }

  /**
   * Create a {@link SvgPanel} from an SVG document represented as a character sequence.
   *
   * @param svgChart the SVG content
   * @return a {@link SvgPanel} displaying the provided SVG content
   */
  static SvgPanel export(CharSequence svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException('svgChart must not be null')
    }
    export(svgChart.toString())
  }

  /**
   * Create a {@link SvgPanel} from an {@link Svg} instance.
   *
   * @param svgChart the {@link Svg} chart to render
   * @return a {@link SvgPanel} displaying the rendered SVG chart
   */
  static SvgPanel export(Svg svgChart) {
    if (svgChart == null) {
      throw new IllegalArgumentException('svgChart must not be null')
    }
    export(svgChart.toXml())
  }

  /**
   * Create a {@link SvgPanel} from a Charm {@link CharmChart} instance.
   *
   * @param chart the Charm chart to render as SVG
   * @return a {@link SvgPanel} displaying the rendered chart
   */
  static SvgPanel export(CharmChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException('chart must not be null')
    }
    export(chart.render())
  }

  /**
   * Create a {@link SvgPanel} from a {@link PlotGrid}.
   *
   * @param grid the plot grid to render
   * @return a {@link SvgPanel} displaying the rendered grid
   */
  static SvgPanel export(PlotGrid grid) {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    export(grid.render())
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed overload.
   *
   * @param chart a chart object (CharmChart, Chart, PlotGrid, Svg, or CharSequence)
   * @return a {@link SvgPanel} displaying the rendered chart
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static SvgPanel export(Object chart) {
    if (chart == null) {
      throw new IllegalArgumentException('chart must not be null')
    }
    switch (chart) {
      case PlotGrid -> export(chart as PlotGrid)
      case CharmChart -> export(chart as CharmChart)
      case Svg -> export(chart as Svg)
      case CharSequence -> export(chart as CharSequence)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

}
