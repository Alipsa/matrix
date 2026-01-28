package se.alipsa.matrix.xchart.abstractions
import org.knowm.xchart.internal.chartpart.Chart

import org.knowm.xchart.XChartPanel

/**
 * Base interface for all Matrix chart types.
 * Provides methods for chart export, display, and access to the underlying XChart instance.
 * <p>
 * All chart implementations in matrix-xchart implement this interface, which provides
 * a consistent API for exporting charts to various formats (PNG, SVG, Swing) and
 * displaying them interactively.
 * </p>
 *
 * @param <C> the XChart chart type (e.g., XYChart, CategoryChart, PieChart)
 */
interface MatrixXChart<C extends Chart> {

  /**
   * Export the chart to a PNG image stream.
   *
   * @param os the output stream to write the PNG data to
   */
  void exportPng(OutputStream os)

  /**
   * Export the chart to a PNG image file.
   *
   * @param file the file to write the PNG image to (will be created or overwritten)
   */
  void exportPng(File file)

  /**
   * Export the chart to an SVG vector graphics stream.
   *
   * @param os the output stream to write the SVG data to
   */
  void exportSvg(OutputStream os)

  /**
   * Export the chart to an SVG vector graphics file.
   *
   * @param file the file to write the SVG image to (will be created or overwritten)
   */
  void exportSvg(File file)

  /**
   * Export the chart as a Swing component panel.
   * This creates a displayable panel that can be embedded in Swing applications.
   *
   * @return an XChartPanel containing the chart
   */
  XChartPanel<C> exportSwing()

  /**
   * Display the chart in a new Swing window.
   * This creates a standalone window showing the chart interactively.
   */
  void display()

  /**
   * Get the underlying XChart chart instance.
   * This allows direct access to XChart's API for advanced customization
   * not exposed through the Matrix wrapper.
   *
   * @return the XChart chart object
   */
  C getXChart()
}