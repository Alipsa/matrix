package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Transformed coordinate system.
 * Applies transformations to x and/or y coordinates independently.
 *
 * Unlike scale transformations which happen before mapping to pixels,
 * coordinate transformations happen in the coordinate space and affect
 * things like grid lines and aspect ratios.
 *
 * Usage:
 * - coord_trans(x: "log10") - log10 transform on x-axis
 * - coord_trans(y: "sqrt") - square root transform on y-axis
 * - coord_trans(x: "log10", y: "sqrt") - both axes transformed
 * - coord_trans(x: [forward: {...}, inverse: {...}]) - custom transformation
 *
 * Available transformations:
 * - "identity" - no transformation (default)
 * - "log" - natural logarithm
 * - "log10" - base-10 logarithm
 * - "sqrt" - square root
 * - "reverse" - reverse axis direction
 * - "reciprocal" or "inverse" - 1/x transformation
 * - "power" - power transformation (specify exponent parameter)
 */
@CompileStatic
class CoordTrans extends Coord {

  /** X-axis transformation */
  Trans xTrans

  /** Y-axis transformation */
  Trans yTrans

  /** X-axis limits [min, max] - null means auto-detect from data */
  List<Number> xlim

  /** Y-axis limits [min, max] - null means auto-detect from data */
  List<Number> ylim

  /** Whether to expand the axis limits slightly beyond the data range */
  boolean expand = true

  /** Expansion multiplier (default 5% on each side) */
  double expandMult = 0.05

  CoordTrans() {
    this.xTrans = new Transformations.IdentityTrans()
    this.yTrans = new Transformations.IdentityTrans()
  }

  CoordTrans(Map params) {
    // Handle x transformation
    if (params.x) {
      if (params.x instanceof String) {
        this.xTrans = Transformations.getTrans(params.x as String)
      } else if (params.x instanceof Map) {
        Map xMap = params.x as Map
        if (xMap.forward && xMap.inverse) {
          this.xTrans = Transformations.fromClosures(
              xMap.forward as Closure<Number>,
              xMap.inverse as Closure<Number>
          )
        } else if (xMap.name) {
          this.xTrans = Transformations.getTrans(xMap.name as String, xMap)
        } else {
          throw new IllegalArgumentException("Custom x transformation must have 'forward' and 'inverse' closures")
        }
      } else if (params.x instanceof Trans) {
        this.xTrans = params.x as Trans
      } else {
        throw new IllegalArgumentException("x parameter must be String, Map, or Trans")
      }
    } else {
      this.xTrans = new Transformations.IdentityTrans()
    }

    // Handle y transformation
    if (params.y) {
      if (params.y instanceof String) {
        this.yTrans = Transformations.getTrans(params.y as String)
      } else if (params.y instanceof Map) {
        Map yMap = params.y as Map
        if (yMap.forward && yMap.inverse) {
          this.yTrans = Transformations.fromClosures(
              yMap.forward as Closure<Number>,
              yMap.inverse as Closure<Number>
          )
        } else if (yMap.name) {
          this.yTrans = Transformations.getTrans(yMap.name as String, yMap)
        } else {
          throw new IllegalArgumentException("Custom y transformation must have 'forward' and 'inverse' closures")
        }
      } else if (params.y instanceof Trans) {
        this.yTrans = params.y as Trans
      } else {
        throw new IllegalArgumentException("y parameter must be String, Map, or Trans")
      }
    } else {
      this.yTrans = new Transformations.IdentityTrans()
    }

    // Handle other parameters
    if (params.xlim) this.xlim = params.xlim as List<Number>
    if (params.ylim) this.ylim = params.ylim as List<Number>
    if (params.containsKey('expand')) this.expand = params.expand
    if (params.expandMult) this.expandMult = params.expandMult as double
  }

  @CompileDynamic
  @Override
  List<Number> transform(Number x, Number y, Map<String, ?> scales) {
    // Get x and y scales
    def xScale = scales['x']
    def yScale = scales['y']

    // First apply the scale transformation (data -> pixels in linear space)
    Number xPx = xScale ? xScale.transform(x) : x
    Number yPx = yScale ? yScale.transform(y) : y

    // Then apply the coordinate transformation
    // Note: This is a simplified approach. In a full implementation,
    // transformations would be applied to the data space before scales,
    // and the scales would work in transformed space.
    // For now, we'll apply transformation directly.
    Number xTransformed = xTrans.transform(xPx)
    Number yTransformed = yTrans.transform(yPx)

    return [xTransformed, yTransformed]
  }

  @CompileDynamic
  @Override
  List<Number> inverse(Number xPx, Number yPx, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']

    // First inverse the coordinate transformation
    Number xLinear = xTrans.inverse(xPx)
    Number yLinear = yTrans.inverse(yPx)

    // Then inverse the scale transformation
    Number x = xScale ? xScale.inverse(xLinear) : xLinear
    Number y = yScale ? yScale.inverse(yLinear) : yLinear

    return [x, y]
  }
}
