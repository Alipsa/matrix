package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmRenderException

/**
 * Fluent builder for rendering a {@link Chart} with custom configuration.
 *
 * <p>Obtain via {@link Chart#renderConfig()}:</p>
 * <pre>
 * Svg svg = chart.renderConfig()
 *     .width(640)
 *     .height(420)
 *     .marginLeft(100)
 *     .render()
 * </pre>
 */
@CompileStatic
class RenderBuilder {

  private final Chart chart
  private final RenderConfig config = new RenderConfig()

  RenderBuilder(Chart chart) {
    this.chart = chart
  }

  RenderBuilder width(int width) {
    if (width <= 0) throw new IllegalArgumentException("width must be > 0, was ${width}")
    config.width = width; this
  }

  RenderBuilder height(int height) {
    if (height <= 0) throw new IllegalArgumentException("height must be > 0, was ${height}")
    config.height = height; this
  }

  RenderBuilder marginTop(int margin) { config.marginTop = margin; this }
  RenderBuilder marginRight(int margin) { config.marginRight = margin; this }
  RenderBuilder marginBottom(int margin) { config.marginBottom = margin; this }
  RenderBuilder marginLeft(int margin) { config.marginLeft = margin; this }
  RenderBuilder panelSpacing(int spacing) { config.panelSpacing = spacing; this }
  RenderBuilder stripHeight(int height) { config.stripHeight = height; this }
  RenderBuilder stripWidth(int width) { config.stripWidth = width; this }
  RenderBuilder axisTickCount(int count) { config.axisTickCount = count; this }
  RenderBuilder axisTickLength(int length) { config.axisTickLength = length; this }
  RenderBuilder pointRadius(int radius) { config.pointRadius = radius; this }
  RenderBuilder labelPadding(int padding) { config.labelPadding = padding; this }
  RenderBuilder legendKeySize(int size) { config.legendKeySize = size; this }
  RenderBuilder legendSpacing(int spacing) { config.legendSpacing = spacing; this }

  /**
   * Renders the chart with the configured settings.
   *
   * @return SVG model object
   */
  Svg render() {
    try {
      new CharmRenderer().render(chart, config)
    } catch (Exception e) {
      throw new CharmRenderException("Failed to render Charm chart: ${e.message}", e)
    }
  }
}
