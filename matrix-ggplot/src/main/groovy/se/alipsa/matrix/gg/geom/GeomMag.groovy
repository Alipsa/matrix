package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil

/**
 * Magnitude geometry for vector field visualization.
 * Renders vector magnitudes at specified (x,y) locations, typically using
 * size and/or color to encode the magnitude value.
 *
 * Common use cases include:
 * - Physics: Electric fields, magnetic fields, force fields
 * - Fluid dynamics: Velocity fields, pressure gradients
 * - Meteorology: Wind speed visualization
 * - Engineering: Stress fields, displacement fields
 */
@CompileStatic
class GeomMag extends Geom {

  /** Default point color */
  String color = 'steelblue'

  /** Default point fill */
  String fill = 'steelblue'

  /** Default base size (radius) */
  BigDecimal size = 3

  /** Default point shape */
  String shape = 'circle'

  /** Default alpha (transparency) */
  BigDecimal alpha = 1.0

  /** Whether to automatically scale size/color to magnitude (default: true) */
  boolean scaleToMagnitude = true

  /** Size multiplier for magnitude scaling */
  BigDecimal sizeMultiplier = 1.0

  GeomMag() {
    requiredAes = ['x', 'y']
    defaultAes = [color: 'steelblue', size: 3, alpha: 1.0] as Map<String, Object>
  }

  GeomMag(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    if (params.size != null) this.size = params.size as BigDecimal
    this.shape = params.shape as String ?: this.shape
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.scaleToMagnitude != null) this.scaleToMagnitude = params.scaleToMagnitude as boolean
    if (params.sizeMultiplier != null) this.sizeMultiplier = params.sizeMultiplier as BigDecimal
    this.params = params
  }

}
