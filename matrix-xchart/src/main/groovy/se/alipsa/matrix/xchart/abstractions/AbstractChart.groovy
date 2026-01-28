package se.alipsa.matrix.xchart.abstractions

import groovy.transform.CompileStatic
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.VectorGraphicsEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.core.Matrix

import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import java.awt.Color
import java.lang.reflect.InvocationTargetException

/**
 * Base class for all Matrix chart implementations.
 * Provides common functionality for chart creation, styling, export, and display.
 * <p>
 * This abstract class implements the {@link MatrixXChart} interface and provides
 * concrete implementations for export methods (PNG, SVG, Swing), display functionality,
 * and access to chart properties. Subclasses must initialize the 'xchart' field
 * with the appropriate XChart type.
 * </p>
 * <p>
 * All charts maintain a reference to the source Matrix data and provide methods
 * for styling, theming, and axis customization.
 * </p>
 *
 * @param <T> the concrete chart type for fluent API chaining
 * @param <C> the XChart chart type (e.g., XYChart, CategoryChart)
 * @param <ST> the XChart styler type for this chart
 * @param <S> the XChart series type for this chart
 */
@CompileStatic
abstract class AbstractChart<T extends AbstractChart, C extends Chart, ST extends Styler, S extends Series> implements MatrixXChart {

  protected C xchart
  protected Matrix matrix

  /**
   * Get the source Matrix data used to create this chart.
   *
   * @return the Matrix data
   */
  Matrix getMatrix() {
    return matrix
  }

  /**
   * Set the chart title.
   *
   * @param title the chart title
   * @return this chart for method chaining
   */
  T setTitle(String title) {
    xchart.setTitle(title)
    return this as T
  }

  /**
   * Get the chart's styler for advanced customization.
   * The styler provides access to all visual styling options for the chart.
   *
   * @return the chart styler
   */
  ST getStyle() {
    return xchart.getStyler() as ST
  }

  /**
   * Export the chart to a PNG image stream.
   *
   * @param os the output stream to write the PNG data to
   */
  void exportPng(OutputStream os) {
    BitmapEncoder.saveBitmap(xchart, os, BitmapEncoder.BitmapFormat.PNG)
  }

  /**
   * Export the chart to a PNG image file.
   *
   * @param file the file to write the PNG image to (will be created or overwritten)
   */
  void exportPng(File file) {
    BitmapEncoder.saveBitmap(xchart, file.absolutePath, BitmapEncoder.BitmapFormat.PNG)
  }

  /**
   * Export the chart to an SVG vector graphics stream.
   *
   * @param os the output stream to write the SVG data to
   */
  void exportSvg(OutputStream os) {
    VectorGraphicsEncoder.saveVectorGraphic(xchart, os, VectorGraphicsEncoder.VectorGraphicsFormat.SVG)
  }

  /**
   * Export the chart to an SVG vector graphics file.
   *
   * @param file the file to write the SVG image to (will be created or overwritten)
   */
  void exportSvg(File file) {
    VectorGraphicsEncoder.saveVectorGraphic(xchart, file.absolutePath, VectorGraphicsEncoder.VectorGraphicsFormat.SVG)
  }

  /**
   * Export the chart as a Swing component panel.
   * This creates a displayable panel that can be embedded in Swing applications.
   *
   * @return an XChartPanel containing the chart
   */
  XChartPanel<C> exportSwing() {
    new XChartPanel<>(getXChart())
  }

  /**
   * Get the underlying XChart chart instance.
   * This allows direct access to XChart\'s API for advanced customization
   * not exposed through the Matrix wrapper.
   *
   * @return the XChart chart object
   */
  C getXChart() {
    return this.xchart
  }

  S getSeries(String name) {
    xchart.getSeriesMap().get(name)
  }

  Map<String, S> getSeries() {
    xchart.getSeriesMap()
  }

  /**
   * Set the X-axis label.
   *
   * @param label the X-axis label text
   * @return this chart for method chaining
   */
  T setXLabel(String label) {
    xchart.XAxisTitle = label
    return this as T
  }

  /**
   * Set the Y-axis label.
   *
   * @param label the Y-axis label text
   * @return this chart for method chaining
   */
  T setYLabel(String label) {
    xchart.YAxisTitle = label
    return this as T
  }

  /**
   * Get the X-axis label.
   *
   * @return the X-axis label text
   */
  String getXLabel() {
    xchart.XAxisTitle
  }

  /**
   * Get the Y-axis label.
   *
   * @return the Y-axis label text
   */
  String getYLabel() {
    xchart.YAxisTitle
  }

  void makeFillTransparent(Series s, int numSeries, Integer transparency = 185) {
    // Make the fill transparent so that overlaps are visible
    Color[] colors = style.theme.seriesColors
    if (numSeries > colors.size() - 1) {
      def multiple = (style.theme.seriesColors.size() / numSeries).ceil() as int
      List<Color> colorList = []
      for (int i = 0; i < multiple; i++) {
        colorList.addAll(colors as List<Color>)
      }
      colors = colorList as Color[]
    }
    Color color = colors[numSeries]
    //s.lineColor = color.darker()
    s.fillColor = new Color(color.red, color.green, color.blue, transparency)
  }

  /**
   * Display the chart in a new Swing window.
   * This creates a standalone window showing the chart interactively.
   * The window will use the chart title or matrix name as the window title.
   */
  @Override
  void display() {
    String windowTitle = xchart.title ?: matrix.getMatrixName() ?: "Matrix XChart"
    final JFrame frame = new JFrame(windowTitle);

    try {
      SwingUtilities.invokeAndWait(() -> {
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        frame.add(exportSwing())
        frame.pack()
        frame.setVisible(true)
      })
    } catch (InterruptedException | InvocationTargetException e) {
      throw new RuntimeException("Error displaying chart", e)
    }
  }
}
