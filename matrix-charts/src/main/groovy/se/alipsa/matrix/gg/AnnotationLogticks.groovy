package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.geom.GeomLogticks
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType

/**
 * Annotation for logarithmic tick marks.
 * Automatically adds tick marks at appropriate positions for log-scaled axes.
 *
 * This annotation detects log-scaled axes and generates tick marks at:
 * - Major positions: powers of the base (1, 10, 100, ...)
 * - Intermediate positions: 2 and 5 multiples between major ticks (2, 5, 20, 50, ...)
 * - Minor positions: other integer multiples between major ticks (3, 4, 6, 7, 8, 9, ...)
 *
 * Usage:
 * <pre>{@code
 * def chart = ggplot(data, aes('x', 'y')) +
 *   geom_point() +
 *   scale_x_log10() +
 *   annotation_logticks(sides: 'b')
 * }</pre>
 *
 * Parameters:
 * - base: Logarithmic base (default: 10)
 * - sides: Which sides to draw ticks on: 't' (top), 'r' (right), 'b' (bottom), 'l' (left) (default: 'bl')
 * - outside: Whether ticks extend outside plot area (default: false)
 * - scaled: Whether data is already log-transformed (default: true)
 * - short: Length of minor tick marks in pixels (default: 1.5)
 * - mid: Length of intermediate tick marks in pixels (default: 2.25)
 * - long: Length of major tick marks in pixels (default: 4.5)
 * - colour/color: Tick color (default: 'black')
 * - linewidth: Tick line width (default: 0.5)
 * - linetype: Tick line type (default: 'solid')
 * - alpha: Transparency 0-1 (default: 1.0)
 */
@CompileStatic
class AnnotationLogticks {

  /** The geom that renders the log ticks */
  GeomLogticks geom

  /**
   * Create annotation with parameters.
   * @param params Map of parameters (base, sides, outside, scaled, short, mid, long, colour, linewidth, linetype, alpha)
   */
  AnnotationLogticks(Map params = [:]) {
    this.geom = new GeomLogticks(params)
  }

  /**
   * Convert this annotation to a Layer for the rendering pipeline.
   * @return Layer with GeomLogticks and no data (reads scales directly)
   */
  Layer toLayer() {
    return new Layer(
        geom: this.geom,
        data: null,  // No data needed - geom reads scales directly during render
        aes: null,
        stat: StatType.IDENTITY,
        position: PositionType.IDENTITY,
        params: [:],
        inheritAes: false  // Don't inherit global aesthetics
    )
  }
}
