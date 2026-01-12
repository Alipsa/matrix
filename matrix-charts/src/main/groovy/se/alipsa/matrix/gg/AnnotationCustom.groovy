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
 * - A Closure that receives (G group, Map bounds) and renders SVG using pixel coordinates
 *   Optional parameters: (G group, Map bounds, Map scales, Coord coord)
 * - A gsvg SvgElement (Rect, Circle, etc.) that will be cloned into position
 * - A String containing raw SVG markup that will be parsed and inserted
 *
 * Position parameters (xmin, xmax, ymin, ymax) are specified in DATA coordinates:
 * - Values in your data's units (e.g., if data ranges from 0-100, use those values)
 * - -Inf/Inf (default) fill the entire plot panel
 * - These are automatically transformed to pixel coordinates for rendering
 *
 * The bounds map passed to closures contains PIXEL coordinates ready for SVG rendering.
 * The custom annotation does not affect scale training or axis limits.
 *
 * Usage examples:
 * <pre>{@code
 * // Closure-based grob - bounds contains PIXEL coordinates
 * def chart = ggplot(data, aes('x', 'y')) +
 *   geom_point() +
 *   annotation_custom(
 *     grob: { G g, Map b ->
 *       g.addRect()
 *         .x(b.xmin as int)      // b.xmin is already in pixels
 *         .y(b.ymin as int)      // b.ymin is already in pixels
 *         .width((b.xmax - b.xmin) as int)
 *         .height((b.ymax - b.ymin) as int)
 *         .fill('red')
 *         .addAttribute('opacity', 0.2)
 *     },
 *     xmin: 1, xmax: 3, ymin: 5, ymax: 10  // DATA coordinates
 *   )
 *
 * // SVG string grob
 * def chart2 = ggplot(data, aes('x', 'y')) +
 *   geom_point() +
 *   annotation_custom(
 *     grob: '<rect width="100" height="50" fill="blue" opacity="0.3"/>',
 *     xmin: 2, xmax: 4, ymin: 3, ymax: 7  // DATA coordinates
 *   )
 * }</pre>
 *
 * Parameters:
 * - grob: (required) Closure, SvgElement, or String containing SVG markup
 * - xmin: minimum x position in DATA coordinates (default: -Inf, fills from left edge)
 * - xmax: maximum x position in DATA coordinates (default: Inf, fills to right edge)
 * - ymin: minimum y position in DATA coordinates (default: -Inf, fills from lower values)
 * - ymax: maximum y position in DATA coordinates (default: Inf, fills to higher values)
 *
 * Important: Position parameters (xmin, xmax, ymin, ymax) are specified in DATA coordinates.
 * The bounds map passed to closures contains the corresponding PIXEL coordinates after transformation.
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
    BigDecimal xmin = AnnotationConstants.handleInfValue(params.xmin, true)
    BigDecimal xmax = AnnotationConstants.handleInfValue(params.xmax, false)
    BigDecimal ymin = AnnotationConstants.handleInfValue(params.ymin, true)
    BigDecimal ymax = AnnotationConstants.handleInfValue(params.ymax, false)

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
}
