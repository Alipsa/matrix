package se.alipsa.matrix.charm.render.geom

import se.alipsa.groovy.svg.Marker
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.matrix.charm.ArrowEnds
import se.alipsa.matrix.charm.ArrowSpec
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.RenderContext

/**
 * SVG marker support for line-like geoms with arrowheads.
 */
class ArrowMarkerSupport {

  private static final String MARKER_START = 'marker-start'
  private static final String MARKER_END = 'marker-end'

  /**
   * Applies an arrow marker to an SVG line/path element when the layer has an {@link ArrowSpec}.
   *
   * @param element SVG line or path element
   * @param context render context
   * @param layer layer spec
   * @param stroke stroke colour used for the arrow
   * @param elementIndex rendered element index within the layer
   */
  static void applyArrowMarker(
      SvgElement<? extends SvgElement> element,
      RenderContext context,
      LayerSpec layer,
      String stroke,
      int elementIndex
  ) {
    if (!(layer.params.arrow instanceof ArrowSpec) || context.defs == null) {
      return
    }
    ArrowSpec arrow = layer.params.arrow as ArrowSpec

    String markerId = "charm-arrow-${context.layerIndex}-${elementIndex}"
    createMarker(context, markerId, arrow, stroke)
    String markerUrl = "url(#${markerId})"
    if (arrow.ends == ArrowEnds.START || arrow.ends == ArrowEnds.BOTH) {
      element.addAttribute(MARKER_START, markerUrl)
    }
    if (arrow.ends == ArrowEnds.END || arrow.ends == ArrowEnds.BOTH) {
      element.addAttribute(MARKER_END, markerUrl)
    }
  }

  private static void createMarker(RenderContext context, String markerId, ArrowSpec arrow, String stroke) {
    BigDecimal mid = arrow.width / 2
    Marker marker = context.defs.addMarker(markerId)
        .markerWidth(arrow.length)
        .markerHeight(arrow.width)
        .refX(arrow.length)
        .refY(mid)
        .orient('auto-start-reverse')
        .markerUnits('userSpaceOnUse')
    if (arrow.closed) {
      marker.addPolygon("0,0 ${arrow.length},${mid} 0,${arrow.width}")
          .fill(stroke)
          .stroke(stroke)
    } else {
      marker.addPath()
          .d("M 0 0 L ${arrow.length} ${mid} L 0 ${arrow.width}")
          .fill('none')
          .stroke(stroke)
    }
  }

}
