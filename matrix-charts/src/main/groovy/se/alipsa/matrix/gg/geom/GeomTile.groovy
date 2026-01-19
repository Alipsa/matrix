package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.RenderContext
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
  BigDecimal linewidth = 0.5

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Tile width (null for auto-calculation based on data resolution) */
  BigDecimal width = null

  /** Tile height (null for auto-calculation based on data resolution) */
  BigDecimal height = null

  GeomTile() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [fill: 'gray', color: 'white', alpha: 1.0] as Map<String, Object>
  }

  GeomTile(Map params) {
    this()
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.width != null) this.width = params.width as BigDecimal
    if (params.height != null) this.height = params.height as BigDecimal
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    render(group, data, aes, scales, coord, null)
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
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
    BigDecimal tileWidth = this.width ?: calculateResolution(data, xCol)
    BigDecimal tileHeight = this.height ?: calculateResolution(data, yCol)

    int elementIndex = 0
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def fillVal = fillCol ? row[fillCol] : null

      if (xVal == null || yVal == null) {
        elementIndex++
        return
      }

      // Get center position
      BigDecimal xCenterPx = xScale?.transform(xVal) as BigDecimal
      BigDecimal yCenterPx = yScale?.transform(yVal) as BigDecimal

      if (xCenterPx == null || yCenterPx == null) {
        elementIndex++
        return
      }

      // Calculate half-widths in data space, then transform to get pixel dimensions
      BigDecimal halfW, halfH
      if (xVal instanceof Number) {
        BigDecimal xLeft = xScale?.transform((xVal as BigDecimal) - tileWidth / 2) as BigDecimal
        BigDecimal xRight = xScale?.transform((xVal as BigDecimal) + tileWidth / 2) as BigDecimal
        halfW = xLeft != null && xRight != null ? (xRight - xLeft).abs() / 2 : 20
      } else {
        // For discrete scales, use bandwidth
        halfW = 20
      }

      if (yVal instanceof Number) {
        BigDecimal yBottom = yScale?.transform((yVal as BigDecimal) - tileHeight / 2) as BigDecimal
        BigDecimal yTop = yScale?.transform((yVal as BigDecimal) + tileHeight / 2) as BigDecimal
        halfH = yBottom != null && yTop != null ? (yTop - yBottom).abs() / 2 : 20
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
      BigDecimal x = xCenterPx - halfW
      BigDecimal y = yCenterPx - halfH
      BigDecimal w = halfW * 2
      BigDecimal h = halfH * 2

      def rect = group.addRect()
          .x(x)
          .y(y)
          .width(w)
          .height(h)
          .fill(tileFill)

      // Apply alpha
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Apply stroke
      if (color != null && linewidth > 0) {
        String strokeColor = ColorUtil.normalizeColor(color) ?: color
        rect.stroke(strokeColor)
        rect.addAttribute('stroke-width', linewidth)
      } else {
        rect.stroke('none')
      }

      // Apply CSS attributes
      GeomUtils.applyAttributes(rect, ctx, 'tile', 'gg-tile', elementIndex)
      elementIndex++
    }
  }

  /**
   * Calculate the resolution (minimum difference between adjacent values) in a column.
   */
  private BigDecimal calculateResolution(Matrix data, String col) {
    List<Number> values = data[col].findAll { it instanceof Number } as List<Number>
    if (values.size() < 2) return 1.0

    List<BigDecimal> sorted = values.collect { it as BigDecimal }.unique().sort()
    if (sorted.size() < 2) return 1.0

    BigDecimal minDiff = null
    for (int i = 1; i < sorted.size(); i++) {
      BigDecimal diff = sorted[i] - sorted[i - 1]
      if (diff > 0 && (minDiff == null || diff < minDiff)) {
        minDiff = diff
      }
    }

    return minDiff ?: 1.0
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

    int index = value.hashCode().abs() % palette.size()
    return palette[index]
  }
}
