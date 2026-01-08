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
 * Tile geometry for creating heatmaps and correlation matrices.
 * Draws rectangular tiles centered at each x,y position.
 *
 * Required aesthetics: x, y
 * Optional aesthetics: fill, color, alpha, width, height
 *
 * Usage:
 * - geom_tile() - basic tiles with automatic sizing
 * - geom_tile(fill: 'blue') - blue tiles
 * - geom_tile(aes(fill: 'value')) - tiles colored by value
 */
@CompileStatic
class GeomTile extends Geom {

  /** Fill color for tiles */
  String fill = 'gray'

  /** Stroke color for tile borders */
  String color = 'white'

  /** Line width for tile borders */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 1.0

  /** Tile width (null for auto-calculation based on data resolution) */
  Number width = null

  /** Tile height (null for auto-calculation based on data resolution) */
  Number height = null

  GeomTile() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'white', alpha: 1.0] as Map<String, Object>
  }

  GeomTile(Map params) {
    this()
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.width != null) this.width = params.width as Number
    if (params.height != null) this.height = params.height as Number
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String fillCol = aes.fillColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomTile requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale fillScale = scales['fill'] ?: scales['color']

    // Calculate tile dimensions based on data resolution if not specified
    double tileWidth = this.width != null ? (this.width as double) : calculateResolution(data, xCol)
    double tileHeight = this.height != null ? (this.height as double) : calculateResolution(data, yCol)

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def fillVal = fillCol ? row[fillCol] : null

      if (xVal == null || yVal == null) return

      // Get center position
      def xCenter = xScale?.transform(xVal)
      def yCenter = yScale?.transform(yVal)

      if (xCenter == null || yCenter == null) return

      // Calculate tile bounds in pixel space
      double xCenterPx = xCenter as double
      double yCenterPx = yCenter as double

      // Calculate half-widths in data space, then transform to get pixel dimensions
      double halfW, halfH
      if (xVal instanceof Number) {
        def xLeft = xScale?.transform((xVal as Number) - tileWidth / 2)
        def xRight = xScale?.transform((xVal as Number) + tileWidth / 2)
        halfW = xLeft != null && xRight != null ? Math.abs((xRight as double) - (xLeft as double)) / 2 : 20
      } else {
        // For discrete scales, use bandwidth
        halfW = 20
      }

      if (yVal instanceof Number) {
        def yBottom = yScale?.transform((yVal as Number) - tileHeight / 2)
        def yTop = yScale?.transform((yVal as Number) + tileHeight / 2)
        halfH = yBottom != null && yTop != null ? Math.abs((yTop as double) - (yBottom as double)) / 2 : 20
      } else {
        halfH = 20
      }

      // Determine fill color
      String tileFill = this.fill
      if (fillCol && fillVal != null) {
        if (fillScale) {
          tileFill = fillScale.transform(fillVal)?.toString() ?: this.fill
        } else if (fillVal instanceof Number) {
          tileFill = this.fill
        } else {
          tileFill = getDefaultColor(fillVal)
        }
      } else if (aes.fill instanceof Identity) {
        tileFill = (aes.fill as Identity).value.toString()
      }
      tileFill = ColorUtil.normalizeColor(tileFill) ?: tileFill

      // Draw the tile
      double x = xCenterPx - halfW
      double y = yCenterPx - halfH
      double w = halfW * 2
      double h = halfH * 2

      def rect = group.addRect()
          .x(x as int)
          .y(y as int)
          .width(w as int)
          .height(h as int)
          .fill(tileFill)

      // Apply alpha
      if ((alpha as double) < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Apply stroke
      if (color != null && (linewidth as double) > 0) {
        String strokeColor = ColorUtil.normalizeColor(color) ?: color
        rect.stroke(strokeColor)
        rect.addAttribute('stroke-width', linewidth)
      } else {
        rect.stroke('none')
      }
    }
  }

  /**
   * Calculate the resolution (minimum difference between adjacent values) in a column.
   */
  private double calculateResolution(Matrix data, String col) {
    List<Number> values = data[col].findAll { it instanceof Number } as List<Number>
    if (values.size() < 2) return 1.0

    List<Double> sorted = values.collect { it as double }.unique().sort()
    if (sorted.size() < 2) return 1.0

    double minDiff = Double.POSITIVE_INFINITY
    for (int i = 1; i < sorted.size(); i++) {
      double diff = sorted[i] - sorted[i - 1]
      if (diff > 0 && diff < minDiff) {
        minDiff = diff
      }
    }

    return Double.isInfinite(minDiff) ? 1.0d : (minDiff as double)
  }

  /**
   * Get a default color from a discrete palette.
   */
  private String getDefaultColor(Object value) {
    List<String> palette = [
      '#F8766D', '#C49A00', '#53B400',
      '#00C094', '#00B6EB', '#A58AFF',
      '#FB61D7'
    ]

    int index = Math.abs(value.hashCode()) % palette.size()
    return palette[index]
  }
}
