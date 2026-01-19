package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.AnnotationConstants
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.render.RenderContext
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleContinuous

/**
 * Raster annotation geometry for rendering pre-colored raster images.
 * Unlike GeomRaster, this geom:
 * - Takes pre-colored raster data (no fill scale transformation)
 * - Uses fixed positioning (xmin, xmax, ymin, ymax bounds)
 * - Does not affect scale training (annotation behavior)
 *
 * The raster is a 2D array/list where each cell contains a color value.
 * Row 0 is rendered at the top (ymax), consistent with image conventions.
 *
 * Usage:
 * <pre>{@code
 * def raster = [
 *   ['red', 'green', 'blue'],
 *   ['yellow', 'purple', 'orange']
 * ]
 * annotation_raster(raster: raster, xmin: 0, xmax: 10, ymin: 0, ymax: 5)
 * }</pre>
 */
@CompileStatic
class GeomRasterAnn extends Geom {

  /** Pre-colored raster data: 2D list of color strings */
  List<List<String>> raster

  /**
   * Interpolation mode.
   * When true, attempts to smooth between pixels.
   * Note: SVG has limited interpolation support; this primarily serves as API compatibility.
   */
  boolean interpolate = false

  GeomRasterAnn() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [:] as Map<String, Object>
  }

  GeomRasterAnn(Map params) {
    this()
    if (params.raster != null) {
      this.raster = normalizeRaster(params.raster)
    }
    if (params.interpolate != null) {
      this.interpolate = params.interpolate as boolean
    }
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (raster == null || raster.isEmpty()) return

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    // Get position bounds from data (in data-space coordinates)
    BigDecimal xmin = AnnotationConstants.getPositionValue(data, 'xmin', 0)
    BigDecimal xmax = AnnotationConstants.getPositionValue(data, 'xmax', 0)
    BigDecimal ymin = AnnotationConstants.getPositionValue(data, 'ymin', 0)
    BigDecimal ymax = AnnotationConstants.getPositionValue(data, 'ymax', 0)

    // Handle infinite values - use full panel extent in DATA-SPACE
    // Requires continuous scales for domain access
    if (!(xScale instanceof ScaleContinuous) || !(yScale instanceof ScaleContinuous)) {
      // Cannot render raster annotation with non-continuous scales
      return
    }
    List<BigDecimal> xDomain = (xScale as ScaleContinuous).computedDomain
    List<BigDecimal> yDomain = (yScale as ScaleContinuous).computedDomain

    if (AnnotationConstants.isInfinite(xmin)) xmin = xDomain[0]
    if (AnnotationConstants.isInfinite(xmax)) xmax = xDomain[1]
    if (AnnotationConstants.isInfinite(ymin)) ymin = yDomain[0]
    if (AnnotationConstants.isInfinite(ymax)) ymax = yDomain[1]

    // Transform bounds from data-space to pixel-space
    BigDecimal xminPx = xScale.transform(xmin) as BigDecimal
    BigDecimal xmaxPx = xScale.transform(xmax) as BigDecimal
    BigDecimal yminPx = yScale.transform(ymin) as BigDecimal
    BigDecimal ymaxPx = yScale.transform(ymax) as BigDecimal

    // Calculate raster dimensions
    int numRows = raster.size()
    if (numRows == 0) return

    // Find the first non-empty row to determine column count
    int numCols = 0
    for (List<String> row : raster) {
      if (row != null && !row.isEmpty()) {
        numCols = row.size()
        break
      }
    }
    if (numCols == 0) return

    // Calculate cell dimensions in pixels
    BigDecimal totalWidth = (xmaxPx - xminPx).abs()
    BigDecimal totalHeight = (ymaxPx - yminPx).abs()
    BigDecimal cellWidth = totalWidth / numCols
    BigDecimal cellHeight = totalHeight / numRows

    // Determine top-left corner (SVG Y increases downward)
    BigDecimal startX = [xminPx, xmaxPx].min()
    BigDecimal startY = [yminPx, ymaxPx].min()

    // Create group for raster with optional interpolation hint
    G rasterGroup = group.addG()
    if (interpolate) {
      // Add CSS hint for smooth rendering (browser support varies)
      rasterGroup.addAttribute('style', 'image-rendering: smooth;')
    }

    // Render each cell
    // Row 0 is rendered at the top (minimum Y pixel position in SVG coordinates)
    int elementIndex = 0
    for (int row = 0; row < numRows; row++) {
      List<String> rowData = raster[row]
      if (rowData == null || rowData.isEmpty()) continue
      for (int col = 0; col < numCols; col++) {
        if (col >= rowData.size()) continue

        String color = rowData[col]
        if (color == null || color.isEmpty()) continue

        // Normalize color
        String normalizedColor = ColorUtil.normalizeColor(color) ?: color

        BigDecimal x = startX + col * cellWidth
        BigDecimal y = startY + row * cellHeight

        // Render cell as rectangle (no stroke for performance)
        def rect = rasterGroup.addRect()
            .x(x as int)
            .y(y as int)
            .width((cellWidth as int).max(1))
            .height((cellHeight as int).max(1))
            .fill(normalizedColor)
            .addAttribute('stroke', 'none')

        GeomUtils.applyAttributes(rect, ctx, 'raster-ann', 'gg-raster-ann', elementIndex)
        elementIndex++
      }
    }
  }

  /**
   * Normalize raster input to List<List<String>>.
   * Accepts 2D arrays, lists, or other iterable structures.
   */
  @SuppressWarnings('Instanceof')
  private static List<List<String>> normalizeRaster(Object input) {
    if (input == null) return []

    List<List<String>> result = []

    if (input instanceof List) {
      for (Object row : input as List) {
        result.add(normalizeRow(row))
      }
    } else if (input.class.isArray()) {
      // Handle 2D arrays
      for (int i = 0; i < java.lang.reflect.Array.getLength(input); i++) {
        Object row = java.lang.reflect.Array.get(input, i)
        result.add(normalizeRow(row))
      }
    } else {
      throw new IllegalArgumentException(
          "Raster must be a 2D list or array of colors, got: ${input.class.name}")
    }

    return result
  }

  /**
   * Normalize a single row to List<String>.
   */
  @SuppressWarnings('Instanceof')
  private static List<String> normalizeRow(Object row) {
    if (row == null) return []

    if (row instanceof List) {
      return (row as List).collect { it?.toString() ?: '' }
    } else if (row.class.isArray()) {
      List<String> result = []
      for (int i = 0; i < java.lang.reflect.Array.getLength(row); i++) {
        Object cell = java.lang.reflect.Array.get(row, i)
        result.add(cell?.toString() ?: '')
      }
      return result
    } else {
      throw new IllegalArgumentException(
          "Raster row must be a list or array of colors, got: ${row.class.name}")
    }
  }
}
