package se.alipsa.matrix.xchart.abstractions

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.VectorGraphicsEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.chartpart.AxesChart
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.MatrixTheme

import java.awt.Color
import java.lang.reflect.InvocationTargetException

import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

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
abstract class AbstractChart<T extends AbstractChart, C extends Chart, ST extends Styler, S extends Series> implements MatrixXChart<C> {

  private static final String DISPLAY_ERROR = 'Error displaying chart'

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
   * Get the chart title.
   *
   * @return the chart title text
   */
  String getTitle() {
    xchart.title
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
   * Export the chart to a PDF document stream.
   *
   * @param os the output stream to write the PDF data to
   */
  void exportPdf(OutputStream os) {
    VectorGraphicsEncoder.saveVectorGraphic(xchart, os, VectorGraphicsEncoder.VectorGraphicsFormat.PDF)
  }

  /**
   * Export the chart to a PDF document file.
   *
   * @param file the file to write the PDF document to (will be created or overwritten)
   */
  void exportPdf(File file) {
    VectorGraphicsEncoder.saveVectorGraphic(xchart, file.absolutePath, VectorGraphicsEncoder.VectorGraphicsFormat.PDF)
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

  /**
   * Get a specific series by name from this chart.
   * Each series represents a set of data points displayed on the chart.
   *
   * @param name the name of the series to retrieve
   * @return the series with the given name, or null if no such series exists
   */
  S getSeries(String name) {
    xchart.getSeries(name)
  }

  /**
   * Get all series in this chart as a map.
   * The map keys are series names, and values are the series objects.
   *
   * @return a map of all series in this chart (series name to series object)
   */
  Map<String, S> getSeries() {
    xchart.getSeriesCollection().collectEntries { S s -> [(s.name): s] }
  }

  /**
   * Set the X-axis label.
   *
   * @param label the X-axis label text
   * @return this chart for method chaining
   */
  T setXLabel(String label) {
    if (xchart instanceof AxesChart) {
      (xchart as AxesChart).XAxisTitle = label
    }
    return this as T
  }

  /**
   * Set the Y-axis label.
   *
   * @param label the Y-axis label text
   * @return this chart for method chaining
   */
  T setYLabel(String label) {
    if (xchart instanceof AxesChart) {
      (xchart as AxesChart).YAxisTitle = label
    }
    return this as T
  }

  /**
   * Get the X-axis label.
   *
   * @return the X-axis label text
   */
  String getXLabel() {
    xchart instanceof AxesChart ? (xchart as AxesChart).XAxisTitle : null
  }

  /**
   * Get the Y-axis label.
   *
   * @return the Y-axis label text
   */
  String getYLabel() {
    xchart instanceof AxesChart ? (xchart as AxesChart).YAxisTitle : null
  }

  /**
   * Initialize common chart properties: sets the matrix reference, XChart instance, theme, and title.
   * Subclasses should call this after building the XChart instance.
   *
   * @param chart the XChart instance to wrap
   * @param matrix the source Matrix data
   */
  protected void initChart(C chart, Matrix matrix) {
    this.xchart = chart
    this.matrix = matrix
    style.theme = new MatrixTheme()
    def matrixName = matrix.matrixName
    if (matrixName != null && !matrixName.isBlank()) {
      title = matrix.matrixName
    }
  }

  /**
   * Make the fill color of a series semi-transparent so overlapping areas remain visible.
   *
   * @param s the series whose fill color to adjust
   * @param numSeries the zero-based index of this series, used to select the color from the theme palette
   * @param transparency the alpha value (0 = fully transparent, 255 = fully opaque); defaults to 185
   */
  void makeFillTransparent(Series s, int numSeries, Integer transparency = 185) {
    // Make the fill transparent so that overlaps are visible
    Color[] colors = style.theme.seriesColors
    if (numSeries > colors.size() - 1) {
      def multiple = (((numSeries + 1.0)) / style.theme.seriesColors.size()).ceil() as int
      List<Color> colorList = []
      for (int i = 0; i < multiple; i++) {
        colorList.addAll(colors as List<Color>)
      }
      colors = colorList as Color[]
    }
    Color color = colors[numSeries]
    // s.lineColor = color.darker()
    s.fillColor = new Color(color.red, color.green, color.blue, transparency)
  }

  /**
   * Display the chart in a new Swing window.
   * This creates a standalone window showing the chart interactively.
   * The window will use the chart title or matrix name as the window title.
   */
  @Override
  void display() {
    String windowTitle = xchart.title ?: matrix.getMatrixName() ?: 'Matrix XChart'

    runOnEventDispatchThread(() -> {
      JFrame frame = new JFrame(windowTitle)
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      frame.add(exportSwing())
      frame.pack()
      frame.setVisible(true)
    })
  }

  protected static void runOnEventDispatchThread(Runnable action) {
    if (SwingUtilities.isEventDispatchThread()) {
      action.run()
      return
    }
    try {
      SwingUtilities.invokeAndWait(action)
    } catch (InterruptedException | InvocationTargetException e) {
      throw new IllegalStateException(DISPLAY_ERROR, e)
    }
  }

}
