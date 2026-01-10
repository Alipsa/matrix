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
 * The grob is positioned within bounds (xmin, xmax, ymin, ymax) and does not affect scale limits.
 * Infinite bounds (-Inf/Inf) are replaced with the full panel extent.
 *
 * Supported grob types:
 * - Closure: Flexible rendering using gsvg API (recommended for complex graphics)
 * - SvgElement: Direct gsvg elements (cloned into position)
 * - String: Raw SVG markup (parsed and inserted)
 *
 * Usage:
 * - Closure: annotation_custom(grob: { G g, Map b -> g.addRect()... }, xmin: 1, xmax: 3, ymin: 5, ymax: 10)
 * - String: annotation_custom(grob: '<rect width="100" height="50" fill="red"/>', xmin: 1, xmax: 3)
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

    // Get position bounds from data
    BigDecimal xmin = getPositionValue(data, 'xmin', 0)
    BigDecimal xmax = getPositionValue(data, 'xmax', 0)
    BigDecimal ymin = getPositionValue(data, 'ymin', 0)
    BigDecimal ymax = getPositionValue(data, 'ymax', 0)

    // Handle infinite values - use full panel extent
    List<BigDecimal> xRange = (xScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getRange()
    List<BigDecimal> yRange = (yScale as se.alipsa.matrix.gg.scale.ScaleContinuous).getRange()

    if (isInfinite(xmin)) xmin = xRange[0]
    if (isInfinite(xmax)) xmax = xRange[1]
    if (isInfinite(ymin)) ymin = yRange[0]
    if (isInfinite(ymax)) ymax = yRange[1]

    // Create bounds map for closure
    Map<String, BigDecimal> bounds = [
        xmin: xmin,
        xmax: xmax,
        ymin: ymin,
        ymax: ymax
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
   */
  private static BigDecimal getPositionValue(Matrix data, String colName, int rowIdx) {
    if (data == null || !data.columnNames().contains(colName)) {
      return Double.NEGATIVE_INFINITY as BigDecimal
    }
    Object value = data[colName][rowIdx]
    if (value == null) return Double.NEGATIVE_INFINITY as BigDecimal
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
   * The element is cloned into the group with positioning transform.
   * Uses gsvg 0.5.0's element.clone() capability for proper element copying.
   */
  private static void renderSvgElement(G group, SvgElement element, Map<String, BigDecimal> bounds) {
    // Create a nested group with positioning
    G customGroup = group.addG()
    BigDecimal xPos = bounds.xmin
    BigDecimal yPos = bounds.ymin
    customGroup.addAttribute('transform', "translate(${xPos as int},${yPos as int})")

    // Clone the element into the custom group to avoid modifying the original
    element.clone(customGroup)
  }

  /**
   * Render raw SVG string.
   * Parses the SVG markup and inserts it into the group with positioning transform.
   * Uses gsvg 0.5.0's SvgReader.parse() capability for parsing SVG strings.
   */
  private static void renderSvgString(G group, String svgString, Map<String, BigDecimal> bounds) {
    // Create a nested group with positioning
    G customGroup = group.addG()
    BigDecimal xPos = bounds.xmin
    BigDecimal yPos = bounds.ymin
    customGroup.addAttribute('transform', "translate(${xPos as int},${yPos as int})")

    // Wrap the SVG string in a proper SVG document if it's not already wrapped
    String wrappedSvg = svgString.trim()
    if (!wrappedSvg.startsWith('<svg')) {
      wrappedSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\">${wrappedSvg}</svg>"
    }

    // Parse the SVG string using gsvg's SvgReader
    Svg parsedSvg = SvgReader.parse(wrappedSvg)

    // Clone the parsed SVG directly into the custom group
    // The clone() method will copy all child elements
    parsedSvg.clone(customGroup)
  }
}
