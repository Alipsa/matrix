package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Raster geometry for fast rendering of rectangular tiles on a regular grid.
 * Similar to geom_tile() but optimized for cases where data is on a regular grid
 * (e.g., image data, raster graphics). Faster than geom_tile() for large regular grids.
 *
 * Key differences from geom_tile():
 * - Assumes data is on a regular grid
 * - No stroke/border by default (for performance)
 * - Optimized for large numbers of tiles
 *
 * Required aesthetics: x, y
 * Optional aesthetics: fill, alpha
 *
 * Usage:
 * - geom_raster() - basic raster with automatic sizing
 * - geom_raster(aes(fill: 'value')) - colored by value
 * - geom_raster(interpolate: true) - smooth interpolation (future enhancement)
 */
@CompileStatic
class GeomRaster extends Geom {

  /** Fill color for raster cells */
  String fill = 'gray'

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Cell width (null for auto-calculation) */
  Number width = null

  /** Cell height (null for auto-calculation) */
  Number height = null

  /** Whether to interpolate between cells (future enhancement) */
  boolean interpolate = false

  GeomRaster() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', alpha: 1.0] as Map<String, Object>
  }

  GeomRaster(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.width != null) this.width = params.width as Number
    if (params.height != null) this.height = params.height as Number
    if (params.interpolate != null) this.interpolate = params.interpolate as boolean
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String fillCol = aes.fillColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomRaster requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Calculate cell dimensions based on data resolution if not specified
    double cellWidth = this.width != null ? (this.width as double) : calculateResolution(data, xCol)
    double cellHeight = this.height != null ? (this.height as double) : calculateResolution(data, yCol)

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def fillVal = fillCol ? row[fillCol] : null

      if (xVal == null || yVal == null) return

      // Get center position
      def xCenter = xScale?.transform(xVal)
      def yCenter = yScale?.transform(yVal)

      if (xCenter == null || yCenter == null) return

      // Calculate cell bounds in pixel space
      double xCenterPx = xCenter as double
      double yCenterPx = yCenter as double

      // Calculate half dimensions in data space
      double halfWidth = cellWidth / 2
      double halfHeight = cellHeight / 2

      // Transform to pixel space
      def xLeft = xScale.transform((xVal as double) - halfWidth)
      def xRight = xScale.transform((xVal as double) + halfWidth)
      def yTop = yScale.transform((yVal as double) + halfHeight)
      def yBottom = yScale.transform((yVal as double) - halfHeight)

      if (xLeft == null || xRight == null || yTop == null || yBottom == null) return

      double x = xLeft as double
      double y = yTop as double
      double w = ((xRight as BigDecimal) - (xLeft as BigDecimal)).abs() as double
      double h = ((yBottom as BigDecimal) - (yTop as BigDecimal)).abs() as double

      // Determine fill color
      String cellFill = this.fill
      if (fillVal != null && fillScale != null) {
        cellFill = fillScale.transform(fillVal)?.toString() ?: this.fill
      } else if (fillCol && fillVal != null) {
        cellFill = fillVal.toString()
      } else if (aes.fill instanceof Identity) {
        cellFill = (aes.fill as Identity).value.toString()
      }
      cellFill = ColorUtil.normalizeColor(cellFill) ?: cellFill

      // Draw raster cell (no stroke for performance)
      def rect = group.addRect()
          .x(x as int)
          .y(y as int)
          .width(w as int)
          .height(h as int)
          .fill(cellFill)

      // Apply alpha
      if (alpha < 1.0) {
        rect.addAttribute('opacity', alpha)
      }

      // No stroke by default (optimized for performance)
      rect.addAttribute('stroke', 'none')
    }
  }

  /**
   * Calculate data resolution (spacing between unique values).
   * For a regular grid, this should be constant.
   */
  private double calculateResolution(Matrix data, String col) {
    if (!data.columnNames().contains(col)) {
      return 1.0
    }

    List<Number> values = data[col].findAll { it instanceof Number } as List<Number>
    if (values.isEmpty()) {
      return 1.0
    }

    // Get unique sorted values
    List<Double> unique = values.collect { it as double }.unique().sort()

    if (unique.size() < 2) {
      return 1.0
    }

    // Calculate minimum spacing (for regular grid, all spacings should be equal)
    double minSpacing = Double.POSITIVE_INFINITY
    for (int i = 1; i < unique.size(); i++) {
      double spacing = unique[i] - unique[i - 1]
      if (spacing > 0 && spacing < minSpacing) {
        minSpacing = spacing
      }
    }

    return Double.isInfinite(minSpacing) ? 1.0d : (minSpacing as double)
  }
}
