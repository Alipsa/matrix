package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.gg.layer.StatType

/**
 * Annotation layer for logarithmic tick marks.
 * Automatically generates tick marks at appropriate positions for log-scaled axes.
 *
 * Usage:
 * - annotation_logticks() - default bottom and left sides
 * - annotation_logticks(sides: 't') - top side only
 * - annotation_logticks(sides: 'trbl') - all four sides
 * - annotation_logticks(base: 2) - base-2 logarithmic ticks
 *
 * Tick types (for base >= 4):
 * - Major: powers of base (e.g., 1, 10, 100, ... for base 10)
 * - Intermediate: multiples at 2 and 5 (e.g., 2, 5, 20, 50, ... for base 10)
 *   Note: For base 10, both 2 and 5 are intermediate. For other bases >= 4,
 *   only mult==2 is intermediate, and mult==5 is minor (if base > 5).
 * - Minor: other integer multiples (e.g., 3, 4, 6, 7, 8, 9, ... for base 10)
 *
 * For bases 2 and 3, only major ticks are generated (no intermediate/minor ticks).
 */
@CompileStatic
class GeomLogticks extends Geom {

  /** Logarithmic base (default: 10) */
  int base = 10

  /** Which sides to draw ticks: 't' (top), 'r' (right), 'b' (bottom), 'l' (left) (default: 'bl') */
  String sides = 'bl'

  /** Whether ticks extend outside plot area (default: false) */
  boolean outside = false

  /** Whether data is already log-transformed (default: true) */
  boolean scaled = true

  /** Length of minor tick marks in pixels (default: 1.5) */
  BigDecimal shortLength = 1.5

  /** Length of intermediate tick marks in pixels (default: 2.25) */
  BigDecimal midLength = 2.25

  /** Length of major tick marks in pixels (default: 4.5) */
  BigDecimal longLength = 4.5

  /** Tick color */
  String colour = 'black'

  /** Tick line width */
  BigDecimal linewidth = 0.5

  /** Tick line type */
  String linetype = 'solid'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  GeomLogticks() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [:] as Map<String, Object>
  }

  GeomLogticks(Map params) {
    this()
    if (params.base != null) this.base = params.base as int
    if (params.sides) this.sides = params.sides as String
    if (params.outside != null) this.outside = params.outside as boolean
    if (params.scaled != null) this.scaled = params.scaled as boolean
    if (params.short != null) this.shortLength = params.short as BigDecimal
    if (params.mid != null) this.midLength = params.mid as BigDecimal
    if (params.long != null) this.longLength = params.long as BigDecimal
    if (params.colour) this.colour = ColorUtil.normalizeColor(params.colour as String)
    if (params.color) this.colour = ColorUtil.normalizeColor(params.color as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as BigDecimal
    if (params.size != null) this.linewidth = params.size as BigDecimal
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    this.params = params
  }

}
