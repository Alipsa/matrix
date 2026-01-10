package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomCustom
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType

/**
 * Annotation for custom graphical objects (grobs).
 * Allows adding arbitrary SVG content to plots at specified positions.
 *
 * The grob can be:
 * - A Closure that receives (G group, Map bounds) or (G group, Map bounds, Map scales, Coord coord) and renders SVG
 * - A gsvg SvgElement (Rect, Circle, etc.) that will be cloned into position
 * - A String containing raw SVG markup that will be parsed and inserted
 *
 * Position parameters (xmin, xmax, ymin, ymax) support infinite values:
 * - -Inf/Inf are replaced with the full panel extent
 * - Default is -Inf to Inf (fills entire plot area)
 *
 * The custom annotation does not affect scale training or axis limits.
 *
 * Usage examples:
 * <pre>{@code
 * // Closure-based grob (recommended for complex graphics)
 * def chart = ggplot(data, aes('x', 'y')) +
 *   geom_point() +
 *   annotation_custom(
 *     grob: { G g, Map b ->
 *       g.addRect()
 *         .x(b.xmin as int)
 *         .y(b.ymin as int)
 *         .width((b.xmax - b.xmin) as int)
 *         .height((b.ymax - b.ymin) as int)
 *         .fill('red')
 *         .addAttribute('opacity', 0.2)
 *     },
 *     xmin: 1, xmax: 3, ymin: 5, ymax: 10
 *   )
 *
 * // SVG string grob
 * def chart2 = ggplot(data, aes('x', 'y')) +
 *   geom_point() +
 *   annotation_custom(
 *     grob: '<rect width="100" height="50" fill="blue" opacity="0.3"/>',
 *     xmin: 2, xmax: 4, ymin: 3, ymax: 7
 *   )
 * }</pre>
 *
 * Parameters:
 * - grob: (required) Closure, SvgElement, or String containing SVG markup
 * - xmin: minimum x position in data coordinates (default: -Inf, fills from left edge)
 * - xmax: maximum x position in data coordinates (default: Inf, fills to right edge)
 * - ymin: minimum y position in data coordinates (default: -Inf, fills from lower values)
 * - ymax: maximum y position in data coordinates (default: Inf, fills to higher values)
 *
 * Note: Position parameters are in DATA-SPACE coordinates, not pixel/screen coordinates.
 * The coordinate system follows standard plotting conventions where:
 * - x increases from left to right
 * - y increases from bottom to top in data space (though rendered top-to-bottom in SVG)
 * For pixel-level control, use a Closure grob and transform coordinates via the scales parameter.
 */
@CompileStatic
class AnnotationCustom {

  /** The custom graphical object */
  Object grob

  /** The geom that renders the custom grob */
  GeomCustom geom

  /** Data matrix containing position bounds */
  Matrix data

  /**
   * Create annotation with custom grob and position bounds.
   * @param params Map with required 'grob' and optional xmin, xmax, ymin, ymax
   */
  AnnotationCustom(Map params) {
    if (!params.grob) {
      throw new IllegalArgumentException("annotation_custom requires 'grob' parameter")
    }

    this.grob = params.grob

    // Get position bounds with defaults
    BigDecimal xmin = handleInfValue(params.xmin, true)
    BigDecimal xmax = handleInfValue(params.xmax, false)
    BigDecimal ymin = handleInfValue(params.ymin, true)
    BigDecimal ymax = handleInfValue(params.ymax, false)

    // Create data matrix with bounds
    this.data = Matrix.builder()
        .columnNames(['xmin', 'xmax', 'ymin', 'ymax'])
        .rows([[xmin, xmax, ymin, ymax]])
        .build()

    this.geom = new GeomCustom(grob: grob)
  }

  /**
   * Convert this annotation to a Layer for the rendering pipeline.
   * @return Layer with GeomCustom and position data
   */
  Layer toLayer() {
    return new Layer(
        geom: this.geom,
        data: this.data,
        aes: null,
        stat: StatType.IDENTITY,
        position: PositionType.IDENTITY,
        params: [:],
        inheritAes: false  // Don't inherit global aesthetics
    )
  }

  /**
   * Handle infinite position values.
   * Converts null, 'Inf', '-Inf' to appropriate infinity constants.
   *
   * @param value the input value
   * @param isMin whether this is a minimum bound (affects default for null)
   * @return BigDecimal representing the position or infinity
   */
  private static BigDecimal handleInfValue(Object value, boolean isMin) {
    if (value == null) {
      // Use extreme values as markers for infinity that BigDecimal can handle
      return isMin ? -AnnotationConstants.INFINITY_MARKER : AnnotationConstants.INFINITY_MARKER
    }

    // Handle string representations of infinity
    if (value instanceof String) {
      String str = (value as String).toLowerCase()
      if (str == 'inf' || str == '+inf') {
        return AnnotationConstants.INFINITY_MARKER
      }
      if (str == '-inf') {
        return -AnnotationConstants.INFINITY_MARKER
      }
      // Try to parse as number
      try {
        return new BigDecimal(str)
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid position value: ${value}")
      }
    }

    // Handle numeric infinity
    if (value instanceof Number) {
      double d = (value as Number).doubleValue()
      if (Double.isInfinite(d)) {
        return d > 0 ? AnnotationConstants.INFINITY_MARKER : -AnnotationConstants.INFINITY_MARKER
      }
      return value as BigDecimal
    }

    // Default conversion
    return value as BigDecimal
  }
}
