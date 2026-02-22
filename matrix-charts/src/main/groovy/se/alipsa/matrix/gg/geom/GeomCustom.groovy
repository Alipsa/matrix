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

}
