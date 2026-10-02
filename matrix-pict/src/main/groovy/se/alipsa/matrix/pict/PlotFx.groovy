package se.alipsa.matrix.pict

import se.alipsa.matrix.chartexport.ChartToJfx

import javafx.scene.Group

/**
 * Typed JavaFX export API for pict charts. JavaFX must be present when using this class.
 * Use {@link Plot} for SVG and other exports that do not require JavaFX.
 */
class PlotFx {

  /**
   * Converts a chart to a JavaFX Group at the default 800 by 600 pixel dimensions.
   *
   * @param chart the chart to convert, must not be null
   * @return an SVGImage extending JavaFX Group
   * @throws IllegalArgumentException if chart is null
   */
  static Group jfx(Chart chart) {
    ChartToJfx.export(Plot.svg(chart))
  }

  /**
   * Converts a chart to a JavaFX Group with explicit dimensions.
   *
   * @param chart the chart to convert, must not be null
   * @param width chart width in pixels
   * @param height chart height in pixels
   * @return an SVGImage extending JavaFX Group
   * @throws IllegalArgumentException if chart is null
   */
  static Group jfx(Chart chart, int width, int height) {
    ChartToJfx.export(Plot.svg(chart, width, height))
  }
}
