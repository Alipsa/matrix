package se.alipsa.matrix.gg.coord

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Transformed coordinate system.
 * Applies transformations to x and/or y coordinates independently.
 *
 * Coordinate transformations are applied to data values BEFORE scale training,
 * so scales work in transformed space. This ensures:
 * - Grid lines are evenly spaced in transformed space
 * - Axis breaks are computed in transformed space
 * - Axis labels show original (inverse-transformed) values
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
 * - "asn" or "asin" - arcsine square root (for proportions)
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
  }

  /**
   * Transform a data value using the x-axis transformation.
   * This should be applied to data BEFORE scale training.
   * @param x the data value to transform
   * @return the transformed value, or null if transformation fails
   */
  BigDecimal transformX(Number x) {
    return xTrans.transform(x)
  }

  /**
   * Transform a data value using the y-axis transformation.
   * This should be applied to data BEFORE scale training.
   * @param y the data value to transform
   * @return the transformed value, or null if transformation fails
   */
  BigDecimal transformY(Number y) {
    return yTrans.transform(y)
  }

  /**
   * Inverse transform a value using the x-axis transformation.
   * Used to convert transformed break values back to original data space for labels.
   * @param x the transformed value
   * @return the original data value
   */
  BigDecimal inverseX(Number x) {
    return xTrans.inverse(x)
  }

  /**
   * Inverse transform a value using the y-axis transformation.
   * Used to convert transformed break values back to original data space for labels.
   * @param y the transformed value
   * @return the original data value
   */
  BigDecimal inverseY(Number y) {
    return yTrans.inverse(y)
  }

  /**
   * Check if the x-axis has a non-identity transformation.
   */
  boolean hasXTransformation() {
    return !(xTrans instanceof Transformations.IdentityTrans)
  }

  /**
   * Check if the y-axis has a non-identity transformation.
   */
  boolean hasYTransformation() {
    return !(yTrans instanceof Transformations.IdentityTrans)
  }

  /**
   * Get custom breaks for x-axis if the transformation defines them.
   * @param limits the data limits [min, max]
   * @param n approximate number of breaks
   * @return list of break points, or null to use default breaks
   */
  List<BigDecimal> getXBreaks(List<Number> limits, int n) {
    return xTrans.breaks(limits, n)
  }

  /**
   * Get custom breaks for y-axis if the transformation defines them.
   * @param limits the data limits [min, max]
   * @param n approximate number of breaks
   * @return list of break points, or null to use default breaks
   */
  List<BigDecimal> getYBreaks(List<Number> limits, int n) {
    return yTrans.breaks(limits, n)
  }

  @CompileDynamic
  @Override
  List<Number> transform(Number x, Number y, Map<String, ?> scales) {
    // Scales have been trained in transformed coordinate space and will apply
    // the coordinate transformation internally when transform() is called.
    // Here we map original data values through the scales to pixel coordinates.
    def xScale = scales['x']
    def yScale = scales['y']

    Number xPx = xScale ? xScale.transform(x) : x
    Number yPx = yScale ? yScale.transform(y) : y

    return [xPx, yPx]
  }

  @CompileDynamic
  @Override
  List<Number> inverse(Number xPx, Number yPx, Map<String, ?> scales) {
    def xScale = scales['x']
    def yScale = scales['y']

    // Inverse scale transformation (pixels -> transformed data)
    Number xTransformed = xScale ? xScale.inverse(xPx) : xPx
    Number yTransformed = yScale ? yScale.inverse(yPx) : yPx

    // Inverse coordinate transformation (transformed data -> original data)
    Number x = xTrans.inverse(xTransformed)
    Number y = yTrans.inverse(yTransformed)

    return [x, y]
  }
}
