package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomRasterAnn
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType

/**
 * Annotation for raster images.
 * Renders a pre-colored raster (2D grid of colors) at fixed positions without affecting scales.
 *
 * This is a specialized annotation optimized for displaying bitmap images or pre-computed
 * color grids. Unlike geom_raster() which maps data values to colors through scales,
 * annotation_raster() expects colors to already be present in the raster data.
 *
 * The raster is positioned using xmin, xmax, ymin, ymax in DATA coordinates.
 * Use -Inf/Inf (or null) to fill the entire plot panel.
 *
 * Row 0 of the raster is rendered at the top (ymax), consistent with image conventions.
 *
 * Usage examples:
 * <pre>{@code
 * // Simple 2x3 raster
 * def raster = [
 *   ['red', 'green', 'blue'],
 *   ['yellow', 'purple', 'orange']
 * ]
 *
 * def chart = ggplot(data, aes('x', 'y')) +
 *   geom_point() +
 *   annotation_raster(
 *     raster: raster,
 *     xmin: 0, xmax: 10,
 *     ymin: 0, ymax: 5
 *   )
 *
 * // Full-panel background raster
 * def chart2 = ggplot(data, aes('x', 'y')) +
 *   annotation_raster(raster: gradientRaster) +  // Fills entire panel
 *   geom_point()
 *
 * // With interpolation hint
 * def chart3 = ggplot(data, aes('x', 'y')) +
 *   annotation_raster(
 *     raster: lowResRaster,
 *     xmin: 1, xmax: 9,
 *     ymin: 1, ymax: 9,
 *     interpolate: true  // Hint for smoother rendering
 *   ) +
 *   geom_point()
 * }</pre>
 *
 * Parameters:
 * - raster: (required) 2D list/array of color values (strings like 'red', '#FF0000', 'rgb(255,0,0)')
 * - xmin: minimum x position in DATA coordinates (default: -Inf, fills from left edge)
 * - xmax: maximum x position in DATA coordinates (default: Inf, fills to right edge)
 * - ymin: minimum y position in DATA coordinates (default: -Inf, fills from bottom)
 * - ymax: maximum y position in DATA coordinates (default: Inf, fills to top)
 * - interpolate: whether to smooth between pixels (default: false; limited browser support)
 */
@CompileStatic
class AnnotationRaster {

  /** The geom that renders the raster */
  GeomRasterAnn geom

  /** Data matrix containing position bounds */
  Matrix data

  /**
   * Create annotation with raster and position bounds.
   * @param params Map with required 'raster' and optional xmin, xmax, ymin, ymax, interpolate
   */
  AnnotationRaster(Map params) {
    if (params.raster == null) {
      throw new IllegalArgumentException("annotation_raster requires 'raster' parameter")
    }

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

    // Create the geom with raster and interpolate settings
    this.geom = new GeomRasterAnn(
        raster: params.raster,
        interpolate: params.interpolate ?: false
    )
  }

  /**
   * Convert this annotation to a Layer for the rendering pipeline.
   * @return Layer with GeomRasterAnn and position data
   */
  Layer toLayer() {
    return new Layer(
        geom: this.geom,
        data: this.data,
        aes: null,
        stat: StatType.IDENTITY,
        position: PositionType.IDENTITY,
        params: [:],
        inheritAes: false  // Annotations never inherit global aesthetics
    )
  }

  /**
   * Handle infinite position values.
   * Converts null, 'Inf', '-Inf' to appropriate infinity constants.
   *
   * @param value the input value
   * @param isMin whether this is a minimum bound (affects default for null)
   * @return BigDecimal representing the position or infinity marker
   */
  private static BigDecimal handleInfValue(Object value, boolean isMin) {
    if (value == null) {
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
