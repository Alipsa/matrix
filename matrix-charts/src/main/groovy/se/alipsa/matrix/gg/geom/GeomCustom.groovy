package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.groovy.svg.io.SvgReader
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Custom graphical object (grob) geometry.
 * Renders custom SVG elements via closures, SvgElements, or SVG strings at specified positions.
 *
 * The grob is positioned within bounds (xmin, xmax, ymin, ymax) specified in DATA coordinates.
 * These bounds are automatically transformed to PIXEL coordinates before being passed to closures.
 * Infinite bounds (-Inf/Inf) fill the entire plot panel.
 *
 * Supported grob types:
 * - Closure: Receives (G group, Map bounds) where bounds contains PIXEL coordinates
 *            Signature can be 2-4 params: (G, Map) or (G, Map, Map scales, Coord)
 * - SvgElement: Direct gsvg elements (cloned into position)
 * - String: Raw SVG markup (parsed and inserted)
 *
 * Usage:
 * - Closure: annotation_custom(grob: { G g, Map b -> g.addRect().x(b.xmin as int)... }, xmin: 1, xmax: 3)
 * - String: annotation_custom(grob: '<rect width="100" height="50" fill="red"/>', xmin: 1, xmax: 3)
 *
 * Note: Position parameters (xmin, xmax, ymin, ymax) are specified in DATA coordinates.
 *       The bounds map passed to closures contains the transformed PIXEL coordinates.
 */
@CompileStatic
class GeomCustom extends Geom {

  /** The custom graphical object (Closure, SvgElement, or String) */
  Object grob

  GeomCustom() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [:] as Map<String, Object>
  }

  GeomCustom(Map params) {
    this()
    if (params.grob) this.grob = params.grob
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (grob == null) return

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    if (xScale == null || yScale == null) return

    // Get position bounds from data (in data-space coordinates)
    BigDecimal xmin = getPositionValue(data, 'xmin', 0)
    BigDecimal xmax = getPositionValue(data, 'xmax', 0)
    BigDecimal ymin = getPositionValue(data, 'ymin', 0)
    BigDecimal ymax = getPositionValue(data, 'ymax', 0)

    // Handle infinite values - use full panel extent in DATA-SPACE
    // (We need domain, not range, because we'll transform to pixels later)
    List<BigDecimal> xDomain = (xScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getComputedDomain()
    List<BigDecimal> yDomain = (yScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getComputedDomain()

    if (isInfinite(xmin)) xmin = xDomain[0]
    if (isInfinite(xmax)) xmax = xDomain[1]
    if (isInfinite(ymin)) ymin = yDomain[0]
    if (isInfinite(ymax)) ymax = yDomain[1]

    // Transform ALL bounds from data-space to pixel-space for consistency
    // This ensures the closure always receives pixel coordinates
    BigDecimal xminPx = xScale.transform(xmin) as BigDecimal
    BigDecimal xmaxPx = xScale.transform(xmax) as BigDecimal
    BigDecimal yminPx = yScale.transform(ymin) as BigDecimal
    BigDecimal ymaxPx = yScale.transform(ymax) as BigDecimal

    // Create bounds map with PIXEL coordinates
    Map<String, BigDecimal> bounds = [
        xmin: xminPx,
        xmax: xmaxPx,
        ymin: yminPx,
        ymax: ymaxPx
    ]

    // Render grob based on type
    if (grob instanceof Closure) {
      renderClosure(group, grob as Closure, bounds, scales, coord)
    } else if (grob instanceof SvgElement) {
      renderSvgElement(group, grob as SvgElement, bounds)
    } else if (grob instanceof String) {
      renderSvgString(group, grob as String, bounds)
    } else {
      throw new IllegalArgumentException(
          "Unsupported grob type: ${grob.class.name}. " +
          "Expected Closure, SvgElement, or String.")
    }
  }

  /**
   * Get position value from data matrix.
   * Returns appropriate INFINITY_MARKER for missing or null values based on column name:
   * - Negative infinity for 'min' columns (xmin, ymin)
   * - Positive infinity for 'max' columns (xmax, ymax)
   *
   * This ensures that default infinite bounds expand to fill the entire panel correctly.
   */
  private static BigDecimal getPositionValue(Matrix data, String colName, int rowIdx) {
    if (data == null || !data.columnNames().contains(colName)) {
      // Determine if this is a min or max bound based on column name
      boolean isMinBound = colName?.toLowerCase()?.contains('min') ?: true
      return isMinBound ? -se.alipsa.matrix.gg.AnnotationConstants.INFINITY_MARKER
                        : se.alipsa.matrix.gg.AnnotationConstants.INFINITY_MARKER
    }
    Object value = data[colName][rowIdx]
    if (value == null) {
      // Determine if this is a min or max bound based on column name
      boolean isMinBound = colName?.toLowerCase()?.contains('min') ?: true
      return isMinBound ? -se.alipsa.matrix.gg.AnnotationConstants.INFINITY_MARKER
                        : se.alipsa.matrix.gg.AnnotationConstants.INFINITY_MARKER
    }
    return value as BigDecimal
  }

  /**
   * Check if a value represents infinity (using our marker values).
   */
  private static boolean isInfinite(BigDecimal value) {
    if (value == null) return true
    // Check for our special infinity marker values
    return value.abs() >= se.alipsa.matrix.gg.AnnotationConstants.INFINITY_MARKER
  }

  /**
   * Render a Closure-based grob.
   * Closure can receive 2-4 parameters: (G group, Map bounds) or (G group, Map bounds, Map scales, Coord coord).
   */
  private static void renderClosure(G group, Closure grob, Map<String, BigDecimal> bounds,
                                     Map<String, Scale> scales, Coord coord) {
    // Create a nested group for the custom content
    G customGroup = group.addG()

    // Call the closure with appropriate number of parameters based on its arity
    int arity = grob.getMaximumNumberOfParameters()
    if (arity >= 4) {
      grob.call(customGroup, bounds, scales, coord)
    } else if (arity == 3) {
      grob.call(customGroup, bounds, scales)
    } else {
      grob.call(customGroup, bounds)
    }
  }

  /**
   * Render a gsvg SvgElement.
   * The element is cloned into the group without positioning transform.
   * Uses gsvg 0.5.0's element.clone() capability for proper element copying.
   *
   * Note: The bounds parameter contains pixel coordinates (for API consistency with closures)
   * but is not used in SvgElement rendering. SvgElements are cloned directly into the group
   * without transformation, as they typically have their own internal coordinate system.
   * For positioned rendering with bounds, use a Closure-based grob instead.
   */
  private static void renderSvgElement(G group, SvgElement element, Map<String, BigDecimal> bounds) {
    // Create a nested group for the custom content
    // Note: We don't apply transform here because SvgElements typically have their own
    // internal coordinate system. Users can wrap in a closure if transformation is needed.
    G customGroup = group.addG()

    // Clone the element into the custom group to avoid modifying the original
    element.clone(customGroup)
  }

  /**
   * Render raw SVG string.
   * Parses the SVG markup and inserts it into the group with positioning transform.
   * Uses gsvg 0.5.0's SvgReader.parse() capability for parsing SVG strings.
   *
   * SECURITY WARNING: This method accepts arbitrary SVG markup and embeds it directly
   * in the output without sanitization. Only use TRUSTED, STATIC SVG strings from your
   * own codebase. NEVER pass user-controlled input (HTTP parameters, form data, database
   * content from untrusted sources) to this method, as it can lead to XSS vulnerabilities
   * when the resulting SVG is rendered in a browser.
   *
   * For dynamic content, use Closure-based grobs instead, which provide programmatic
   * control over SVG element creation.
   *
   * @param group the SVG group to render into
   * @param svgString TRUSTED, STATIC SVG markup string (never user input!)
   * @param bounds the pixel coordinate bounds (not used for SVG strings)
   * @throws IllegalArgumentException if the SVG markup is malformed or cannot be parsed
   */
  private static void renderSvgString(G group, String svgString, Map<String, BigDecimal> bounds) {
    // Create a nested group for the custom content
    G customGroup = group.addG()

    // Wrap the SVG string in a proper SVG document if it's not already wrapped
    String wrappedSvg = svgString.trim()
    if (!wrappedSvg.startsWith('<svg')) {
      wrappedSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\">${wrappedSvg}</svg>"
    }

    try {
      // Parse the SVG string using gsvg's SvgReader
      Svg parsedSvg = SvgReader.parse(wrappedSvg)

      // Clone the parsed SVG directly into the custom group
      // The clone() method will copy all child elements
      parsedSvg.clone(customGroup)
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed to parse SVG string for annotation_custom. " +
          "Please check that the SVG markup is valid. " +
          "Error: ${e.message}", e)
    }
  }
}
