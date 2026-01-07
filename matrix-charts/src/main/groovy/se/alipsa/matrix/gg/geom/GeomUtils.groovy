package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.scale.Scale

/**
 * Utility methods shared across geom implementations.
 */
@CompileStatic
class GeomUtils {

  /** Default ggplot2-like color palette */
  static final List<String> DEFAULT_PALETTE = [
      '#F8766D', '#C49A00', '#53B400',
      '#00C094', '#00B6EB', '#A58AFF',
      '#FB61D7'
  ]

  /**
   * Convert line type to SVG stroke-dasharray.
   *
   * @param type the line type (solid, dashed, dotted, dotdash, longdash, twodash)
   * @return the stroke-dasharray value, or null for solid lines
   */
  static String getDashArray(String type) {
    switch (type?.toLowerCase()) {
      case 'dashed': return '8,4'
      case 'dotted': return '2,2'
      case 'dotdash': return '2,2,8,2'
      case 'longdash': return '12,4'
      case 'twodash': return '4,2,8,2'
      case 'solid':
      default: return null
    }
  }

  /**
   * Get a default color from a discrete palette based on a value.
   *
   * @param value the value to map to a color
   * @return a hex color string
   */
  static String getDefaultColor(Object value) {
    int index = Math.abs(value.hashCode()) % DEFAULT_PALETTE.size()
    return DEFAULT_PALETTE[index]
  }

  /**
   * Draw a point of the specified shape.
   *
   * @param group the SVG group to draw into
   * @param cx center x coordinate
   * @param cy center y coordinate
   * @param radius point radius
   * @param color point color
   * @param shape shape name (circle, square, plus, cross, x, triangle, diamond)
   * @param alphaVal alpha transparency (0.0 - 1.0)
   */
  static void drawPoint(G group, BigDecimal cx, BigDecimal cy, BigDecimal radius, String color, String shape, BigDecimal alphaVal) {
    BigDecimal size = radius * 2
    BigDecimal halfSize = size / 2.0d

    switch (shape?.toLowerCase()) {
      case 'square':
        def rect = group.addRect(size as int, size as int)
            .x((cx - halfSize) as int)
            .y((cy - halfSize) as int)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          rect.addAttribute('fill-opacity', alphaVal)
        }
        break
      case 'plus':
      case 'cross':
        def hLine = group.addLine((cx - halfSize) as int, cy as int, (cx + halfSize) as int, cy as int)
            .stroke(color)
        def vLine = group.addLine(cx as int, (cy - halfSize) as int, cx as int, (cy + halfSize) as int)
            .stroke(color)
        if (alphaVal < 1.0d) {
          hLine.addAttribute('stroke-opacity', alphaVal)
          vLine.addAttribute('stroke-opacity', alphaVal)
        }
        break
      case 'x':
        def diag1 = group.addLine((cx - halfSize) as int, (cy - halfSize) as int, (cx + halfSize) as int, (cy + halfSize) as int)
            .stroke(color)
        def diag2 = group.addLine((cx - halfSize) as int, (cy + halfSize) as int, (cx + halfSize) as int, (cy - halfSize) as int)
            .stroke(color)
        if (alphaVal < 1.0d) {
          diag1.addAttribute('stroke-opacity', alphaVal)
          diag2.addAttribute('stroke-opacity', alphaVal)
        }
        break
      case 'triangle':
        double h = size * Math.sqrt(3) / 2
        double topY = cy - h * 2 / 3
        double bottomY = cy + h / 3
        double leftX = cx - halfSize
        double rightX = cx + halfSize
        String pathD = "M ${cx} ${topY as int} L ${leftX as int} ${bottomY as int} L ${rightX as int} ${bottomY as int} Z"
        def path = group.addPath().d(pathD)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          path.addAttribute('fill-opacity', alphaVal)
        }
        break
      case 'diamond':
        String diamond = "M ${cx} ${(cy - halfSize) as int} " +
            "L ${(cx + halfSize) as int} ${cy} " +
            "L ${cx} ${(cy + halfSize) as int} " +
            "L ${(cx - halfSize) as int} ${cy} Z"
        def diamondPath = group.addPath().d(diamond)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          diamondPath.addAttribute('fill-opacity', alphaVal)
        }
        break
      case 'circle':
      default:
        def circle = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(radius)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          circle.addAttribute('fill-opacity', alphaVal)
        }
        break
    }
  }

  /**
   * Extract size value for a line-based geom from aesthetics and scales.
   * For line geoms, size is typically constant per group (taken from first row).
   *
   * @param defaultSize the geom's default size
   * @param aes the aesthetic mappings
   * @param sizeCol the size column name (may be null)
   * @param rows the data rows for the group
   * @param sizeScale the size scale (may be null)
   * @return the resolved size value
   */
  static BigDecimal extractLineSize(Number defaultSize, Aes aes, String sizeCol, List<Map> rows, Scale sizeScale) {
    if (aes.size instanceof Identity) {
      def identityValue = (aes.size as Identity).value
      if (identityValue != null) {
        return identityValue as BigDecimal
      }
    }
    if (sizeCol && !rows.isEmpty() && rows[0][sizeCol] != null) {
      def rawSize = rows[0][sizeCol]
      if (sizeScale) {
        def scaled = sizeScale.transform(rawSize)
        if (scaled instanceof Number) {
          return scaled as BigDecimal
        }
      } else if (rawSize instanceof Number) {
        return rawSize as BigDecimal
      }
    }
    return defaultSize as BigDecimal
  }

  /**
   * Extract alpha value for a line-based geom from aesthetics and scales.
   * For line geoms, alpha is typically constant per group (taken from first row).
   *
   * @param defaultAlpha the geom's default alpha
   * @param aes the aesthetic mappings
   * @param alphaCol the alpha column name (may be null)
   * @param rows the data rows for the group
   * @param alphaScale the alpha scale (may be null)
   * @return the resolved alpha value
   */
  static BigDecimal extractLineAlpha(Number defaultAlpha, Aes aes, String alphaCol, List<Map> rows, Scale alphaScale) {
    if (aes.alpha instanceof Identity) {
      def identityValue = (aes.alpha as Identity).value
      if (identityValue != null) {
        return identityValue as BigDecimal
      }
    }
    if (alphaCol && !rows.isEmpty() && rows[0][alphaCol] != null) {
      def rawAlpha = rows[0][alphaCol]
      if (alphaScale) {
        def scaled = alphaScale.transform(rawAlpha)
        if (scaled instanceof Number) {
          return scaled as BigDecimal
        }
      } else if (rawAlpha instanceof Number) {
        return rawAlpha as BigDecimal
      }
    }
    return defaultAlpha as BigDecimal
  }

  /**
   * Extract size value for a point-based geom from a data row.
   *
   * @param defaultSize the geom's default size
   * @param aes the aesthetic mappings
   * @param sizeCol the size column name (may be null)
   * @param row the current data row
   * @param sizeScale the size scale (may be null)
   * @return the resolved size value
   */
  static BigDecimal extractPointSize(Number defaultSize, Aes aes, String sizeCol, Map row, Scale sizeScale) {
    if (sizeCol && row[sizeCol] != null) {
      if (sizeScale) {
        def scaled = sizeScale.transform(row[sizeCol])
        if (scaled instanceof Number) {
          return scaled as BigDecimal
        }
      } else if (row[sizeCol] instanceof Number) {
        return row[sizeCol] as BigDecimal
      }
    } else if (aes.size instanceof Identity) {
      def identityValue = (aes.size as Identity).value
      if (identityValue != null) {
        return identityValue as BigDecimal
      }
    }
    return defaultSize as BigDecimal
  }

  /**
   * Extract alpha value for a point-based geom from a data row.
   *
   * @param defaultAlpha the geom's default alpha
   * @param aes the aesthetic mappings
   * @param alphaCol the alpha column name (may be null)
   * @param row the current data row
   * @param alphaScale the alpha scale (may be null)
   * @return the resolved alpha value
   */
  static BigDecimal extractPointAlpha(Number defaultAlpha, Aes aes, String alphaCol, Map row, Scale alphaScale) {
    if (aes.alpha instanceof Identity) {
      def identityValue = (aes.alpha as Identity).value
      if (identityValue != null) {
        return identityValue as BigDecimal
      }
    }
    if (alphaCol && row[alphaCol] != null) {
      if (alphaScale) {
        def scaled = alphaScale.transform(row[alphaCol])
        if (scaled instanceof Number) {
          return scaled as BigDecimal
        }
      } else if (row[alphaCol] instanceof Number) {
        return row[alphaCol] as BigDecimal
      }
    }
    return defaultAlpha as BigDecimal
  }
}
