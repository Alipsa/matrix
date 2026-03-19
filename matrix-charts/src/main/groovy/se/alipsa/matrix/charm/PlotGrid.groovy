package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.render.PlotGridRenderer

/**
 * Immutable compiled multi-plot grid specification.
 *
 * <p>Arranges multiple independent {@link Chart} instances into a single SVG
 * using nested {@code <svg>} elements for viewport isolation.</p>
 *
 * <p>Example:</p>
 * <pre>
 * PlotGrid grid = Charts.plotGrid {
 *   add chart1, chart2
 *   ncol 2
 *   title 'Dashboard'
 * }
 * Svg svg = grid.render(1200, 800)
 * </pre>
 */
@CompileStatic
class PlotGrid {

  private final List<Chart> charts
  private final int ncol
  private final int nrow
  private final List<BigDecimal> widths
  private final List<BigDecimal> heights
  private final String title
  private final int spacing

  /**
   * Creates a new plot grid specification.
   *
   * @param charts list of charts to arrange
   * @param ncol number of columns (default 1)
   * @param nrow number of rows (auto-computed when null)
   * @param widths fractional column weights (null means equal)
   * @param heights fractional row weights (null means equal)
   * @param title optional overall title
   * @param spacing pixel gap between cells (default 10)
   */
  PlotGrid(
      List<Chart> charts,
      int ncol = 1,
      Integer nrow = null,
      List<BigDecimal> widths = null,
      List<BigDecimal> heights = null,
      String title = null,
      int spacing = 10
  ) {
    if (charts == null || charts.isEmpty()) {
      throw new IllegalArgumentException('charts cannot be null or empty')
    }
    if (ncol < 1) {
      throw new IllegalArgumentException("ncol must be >= 1, got $ncol")
    }
    if (spacing < 0) {
      throw new IllegalArgumentException("spacing must be >= 0, got $spacing")
    }
    int minRows = Math.ceil(charts.size() / (double) ncol) as int
    if (nrow != null && nrow < 1) {
      throw new IllegalArgumentException("nrow must be >= 1, got $nrow")
    }
    if (nrow != null && nrow < minRows) {
      throw new IllegalArgumentException("nrow=$nrow is too small for ${charts.size()} charts with ncol=$ncol (need at least $minRows)")
    }
    this.charts = Collections.unmodifiableList(new ArrayList<>(charts))
    this.ncol = ncol
    this.nrow = nrow != null ? nrow : minRows
    this.widths = widths != null ? Collections.unmodifiableList(new ArrayList<>(widths)) : null
    this.heights = heights != null ? Collections.unmodifiableList(new ArrayList<>(heights)) : null
    this.title = title
    this.spacing = spacing
  }

  /**
   * Returns the list of charts in this grid.
   *
   * @return unmodifiable chart list
   */
  List<Chart> getCharts() {
    charts
  }

  /**
   * Returns the number of columns.
   *
   * @return column count
   */
  int getNcol() {
    ncol
  }

  /**
   * Returns the number of rows.
   *
   * @return row count
   */
  int getNrow() {
    nrow
  }

  /**
   * Returns fractional column weights, or null for equal widths.
   *
   * @return column weights
   */
  List<BigDecimal> getWidths() {
    widths
  }

  /**
   * Returns fractional row heights, or null for equal heights.
   *
   * @return row weights
   */
  List<BigDecimal> getHeights() {
    heights
  }

  /**
   * Returns the optional overall title.
   *
   * @return title or null
   */
  String getTitle() {
    title
  }

  /**
   * Returns the pixel spacing between cells.
   *
   * @return spacing in pixels
   */
  int getSpacing() {
    spacing
  }

  /**
   * Renders this grid to SVG with default dimensions (800x600).
   *
   * @return SVG model object
   */
  Svg render() {
    render(800, 600)
  }

  /**
   * Renders this grid to SVG at the given dimensions.
   *
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @return SVG model object
   */
  Svg render(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("PlotGrid render dimensions must be positive: width=$width, height=$height")
    }
    try {
      new PlotGridRenderer().render(this, width, height)
    } catch (IllegalArgumentException e) {
      throw e
    } catch (Exception e) {
      throw new CharmRenderException("Failed to render PlotGrid at ${width}x${height}px", e)
    }
  }
}
