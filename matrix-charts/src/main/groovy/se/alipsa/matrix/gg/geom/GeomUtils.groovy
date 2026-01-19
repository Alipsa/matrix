package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.render.RenderContext
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
      case 'dashed' -> '8,4'
      case 'dotted' -> '2,2'
      case 'dotdash' -> '2,2,8,2'
      case 'longdash' -> '12,4'
      case 'twodash' -> '4,2,8,2'
      case 'solid' -> null
      default -> null
    }
  }

  /**
   * Get a default color from a discrete palette based on a value.
   *
   * @param value the value to map to a color
   * @return a hex color string
   */
  static String getDefaultColor(Object value) {
    int index = value.hashCode().abs() % DEFAULT_PALETTE.size()
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
      case 'square' -> {
        def rect = group.addRect(size as int, size as int)
            .x((cx - halfSize) as int)
            .y((cy - halfSize) as int)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          rect.addAttribute('fill-opacity', alphaVal)
        }
      }
      case 'plus', 'cross' -> {
        def hLine = group.addLine((cx - halfSize) as int, cy as int, (cx + halfSize) as int, cy as int)
            .stroke(color)
        def vLine = group.addLine(cx as int, (cy - halfSize) as int, cx as int, (cy + halfSize) as int)
            .stroke(color)
        if (alphaVal < 1.0d) {
          hLine.addAttribute('stroke-opacity', alphaVal)
          vLine.addAttribute('stroke-opacity', alphaVal)
        }
      }
      case 'x' -> {
        def diag1 = group.addLine((cx - halfSize) as int, (cy - halfSize) as int, (cx + halfSize) as int, (cy + halfSize) as int)
            .stroke(color)
        def diag2 = group.addLine((cx - halfSize) as int, (cy + halfSize) as int, (cx + halfSize) as int, (cy - halfSize) as int)
            .stroke(color)
        if (alphaVal < 1.0d) {
          diag1.addAttribute('stroke-opacity', alphaVal)
          diag2.addAttribute('stroke-opacity', alphaVal)
        }
      }
      case 'triangle' -> {
        double h = size * 3.sqrt() / 2
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
      }
      case 'diamond' -> {
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
      }
      case 'circle' -> {
        def circle = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(radius)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          circle.addAttribute('fill-opacity', alphaVal)
        }
      }
      default -> {
        def circle = group.addCircle()
            .cx(cx)
            .cy(cy)
            .r(radius)
            .fill(color)
            .stroke(color)
        if (alphaVal < 1.0d) {
          circle.addAttribute('fill-opacity', alphaVal)
        }
      }
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

  // ============ CSS Attribute Utilities ============

  /**
   * Normalize an ID prefix per the CSS naming scheme.
   * <p>
   * Normalization rules:
   * <ul>
   *   <li>Convert to lowercase</li>
   *   <li>Replace whitespace with hyphens</li>
   *   <li>Strip characters outside [a-z0-9-]</li>
   *   <li>Return 'gg' if result is empty or starts with a digit</li>
   * </ul>
   *
   * @param prefix the prefix to normalize (may be null)
   * @return the normalized prefix, or 'gg' if invalid
   */
  static String normalizeIdPrefix(String prefix) {
    if (prefix == null || prefix.trim().isEmpty()) {
      return 'gg'
    }

    // Lowercase, replace whitespace and underscores with hyphens, strip invalid chars
    String normalized = prefix.toLowerCase()
        .replaceAll(/[\s_]+/, '-')
        .replaceAll(/[^a-z0-9-]/, '')
        .replaceAll(/-+/, '-')  // Collapse multiple hyphens
        .replaceAll(/^-|-$/, '')  // Remove leading/trailing hyphens

    // If empty or starts with digit, use fallback
    if (normalized.isEmpty() || Character.isDigit(normalized.charAt(0))) {
      return 'gg'
    }

    return normalized
  }

  /**
   * Normalize a token (geom type, etc.) for use in IDs.
   * <p>
   * Tokens are normalized to lowercase with underscores replaced by hyphens.
   *
   * @param token the token to normalize (may be null)
   * @return the normalized token, or empty string if null
   */
  static String normalizeIdToken(String token) {
    if (token == null) {
      return ''
    }
    return token.toLowerCase().replace('_', '-')
  }

  /**
   * Generate a unique element ID based on the render context.
   * <p>
   * ID format:
   * <ul>
   *   <li>Single panel: {@code {prefix}-layer-{layer}-{geom}-{element}}</li>
   *   <li>Faceted: {@code {prefix}-panel-{row}-{col}-layer-{layer}-{geom}-{element}}</li>
   * </ul>
   *
   * @param ctx the render context (may be null)
   * @param geomType the geom type (e.g., 'point', 'bar')
   * @param elementIndex the element index (0-based)
   * @return the generated ID, or null if CSS attributes are disabled
   */
  static String generateElementId(RenderContext ctx, String geomType, int elementIndex) {
    // Return null if context or config is missing or disabled
    if (ctx == null || ctx.cssConfig == null || !ctx.cssConfig.enabled || !ctx.cssConfig.includeIds) {
      return null
    }

    // Determine prefix
    String prefix = ctx.cssConfig.chartIdPrefix
    if (prefix == null || prefix.trim().isEmpty()) {
      prefix = ctx.cssConfig.idPrefix ?: 'gg'
    }
    prefix = normalizeIdPrefix(prefix)

    // Normalize geom type
    String normalizedGeom = normalizeIdToken(geomType)

    // Build ID based on panel info
    if (ctx.panelRow != null && ctx.panelCol != null) {
      // Faceted chart
      return "${prefix}-panel-${ctx.panelRow}-${ctx.panelCol}-layer-${ctx.layerIndex}-${normalizedGeom}-${elementIndex}"
    } else {
      // Single panel
      return "${prefix}-layer-${ctx.layerIndex}-${normalizedGeom}-${elementIndex}"
    }
  }

  /**
   * Apply CSS class and ID attributes to an SVG element.
   * <p>
   * This is a Phase 1 method that applies CSS classes and IDs only.
   * Data attributes are not supported in this version (see Phase 2 overload).
   * <p>
   * This method is a no-op if:
   * <ul>
   *   <li>{@code ctx} is null</li>
   *   <li>{@code ctx.cssConfig} is null</li>
   *   <li>{@code ctx.cssConfig.enabled} is false</li>
   * </ul>
   *
   * @param element the SVG element to modify (may be null)
   * @param ctx the render context (may be null)
   * @param geomType the geom type for ID generation (e.g., 'point', 'bar')
   * @param cssClass the CSS class to apply (e.g., 'gg-point', 'gg-bar')
   * @param elementIndex the element index for ID generation (0-based)
   */
  static void applyAttributes(SvgElement element, RenderContext ctx, String geomType,
                               String cssClass, int elementIndex) {
    // Early return if disabled or missing dependencies
    if (element == null || ctx == null || ctx.cssConfig == null || !ctx.cssConfig.enabled) {
      return
    }

    // Apply CSS class
    if (ctx.cssConfig.includeClasses && cssClass != null && !cssClass.trim().isEmpty()) {
      element.styleClass(cssClass)
    }

    // Apply ID
    if (ctx.cssConfig.includeIds) {
      String elementId = generateElementId(ctx, geomType, elementIndex)
      if (elementId != null) {
        element.id(elementId)
      }
    }
  }

  /**
   * Apply CSS class, ID, and data attributes to an SVG element.
   * <p>
   * This is a Phase 2 method that applies CSS classes, IDs, and data attributes.
   * Data attributes include:
   * <ul>
   *   <li>{@code data-x}: stat output x value used for rendering</li>
   *   <li>{@code data-y}: stat output y value used for rendering</li>
   *   <li>{@code data-row}: stat output row index (0-based)</li>
   *   <li>{@code data-panel}: panel coordinates as "row-col" (faceted charts only)</li>
   *   <li>{@code data-layer}: layer index (0-based)</li>
   *   <li>{@code data-group}: group value (optional, if present in row data)</li>
   * </ul>
   * <p>
   * This method is a no-op if:
   * <ul>
   *   <li>{@code ctx} is null</li>
   *   <li>{@code ctx.cssConfig} is null</li>
   *   <li>{@code ctx.cssConfig.enabled} is false</li>
   * </ul>
   *
   * @param element the SVG element to modify (may be null)
   * @param ctx the render context (may be null)
   * @param geomType the geom type for ID generation (e.g., 'point', 'bar')
   * @param cssClass the CSS class to apply (e.g., 'gg-point', 'gg-bar')
   * @param elementIndex the element index for ID generation (0-based)
   * @param rowData the stat output row data (may be null)
   * @param aes the aesthetic mappings (may be null)
   */
  static void applyAttributes(SvgElement element, RenderContext ctx, String geomType,
                               String cssClass, int elementIndex, Map rowData, Aes aes) {
    // Apply Phase 1 attributes (class and ID)
    applyAttributes(element, ctx, geomType, cssClass, elementIndex)

    // Early return if data attributes are disabled
    if (element == null || ctx == null || ctx.cssConfig == null ||
        !ctx.cssConfig.enabled || !ctx.cssConfig.includeDataAttributes) {
      return
    }

    // Apply data attributes
    if (rowData != null) {
      // Add data-x and data-y from stat output
      if (rowData.x != null) {
        element.addAttribute('data-x', rowData.x.toString())
      }
      if (rowData.y != null) {
        element.addAttribute('data-y', rowData.y.toString())
      }

      // Add data-group if present
      if (rowData.group != null) {
        element.addAttribute('data-group', rowData.group.toString())
      }
    }

    // Add data-row (element index serves as row index)
    element.addAttribute('data-row', elementIndex.toString())

    // Add data-layer
    element.addAttribute('data-layer', ctx.layerIndex.toString())

    // Add data-panel for faceted charts
    if (ctx.panelRow != null && ctx.panelCol != null) {
      element.addAttribute('data-panel', "${ctx.panelRow}-${ctx.panelCol}")
    }
  }
}
