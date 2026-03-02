package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Mutable DSL delegate for building {@link PlotGrid} specifications.
 *
 * <p>Used as the closure delegate inside {@code plotGrid { ... }} blocks.
 * Mirrors the pattern of {@link PlotSpec} as a mutable builder that
 * compiles into an immutable spec.</p>
 *
 * <p>Example:</p>
 * <pre>
 * PlotGrid grid = Charts.plotGrid {
 *   add chart1, chart2
 *   ncol 2
 *   title 'Dashboard'
 *   widths [1.0, 2.0]
 * }
 * </pre>
 */
@CompileStatic
class PlotGridDsl {

  private final List<Chart> charts = []
  private int ncol = 1
  private Integer nrow = null
  private List<BigDecimal> widths = null
  private List<BigDecimal> heights = null
  private String title = null
  private int spacing = 10

  /**
   * Adds a single chart to the grid.
   *
   * @param chart chart to add
   * @return this DSL for chaining
   */
  PlotGridDsl add(Chart chart) {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    charts << chart
    this
  }

  /**
   * Adds multiple charts to the grid.
   *
   * @param chartsToAdd charts to add
   * @return this DSL for chaining
   */
  PlotGridDsl add(Chart... chartsToAdd) {
    chartsToAdd.each { Chart c -> add(c) }
    this
  }

  /**
   * Adds a list of charts to the grid.
   *
   * @param chartsToAdd charts to add
   * @return this DSL for chaining
   */
  PlotGridDsl add(List<Chart> chartsToAdd) {
    if (chartsToAdd == null) {
      throw new IllegalArgumentException('charts list cannot be null')
    }
    chartsToAdd.each { Chart c -> add(c) }
    this
  }

  /**
   * Sets the number of columns.
   *
   * @param value column count
   * @return this DSL for chaining
   */
  PlotGridDsl ncol(int value) {
    this.ncol = value
    this
  }

  /**
   * Sets the number of rows. When null, rows are auto-computed.
   *
   * @param value row count or null for auto
   * @return this DSL for chaining
   */
  PlotGridDsl nrow(Integer value) {
    this.nrow = value
    this
  }

  /**
   * Sets fractional column width weights.
   *
   * @param value list of weights (e.g. [1.0, 2.0])
   * @return this DSL for chaining
   */
  PlotGridDsl widths(List<BigDecimal> value) {
    this.widths = value
    this
  }

  /**
   * Sets fractional row height weights.
   *
   * @param value list of weights (e.g. [1.0, 3.0])
   * @return this DSL for chaining
   */
  PlotGridDsl heights(List<BigDecimal> value) {
    this.heights = value
    this
  }

  /**
   * Sets the overall grid title.
   *
   * @param value title text
   * @return this DSL for chaining
   */
  PlotGridDsl title(String value) {
    this.title = value
    this
  }

  /**
   * Sets the pixel spacing between cells.
   *
   * @param value spacing in pixels
   * @return this DSL for chaining
   */
  PlotGridDsl spacing(int value) {
    this.spacing = value
    this
  }

  /**
   * Builds an immutable {@link PlotGrid} from the current configuration.
   *
   * @return compiled plot grid
   */
  PlotGrid build() {
    new PlotGrid(charts, ncol, nrow, widths, heights, title, spacing)
  }
}
