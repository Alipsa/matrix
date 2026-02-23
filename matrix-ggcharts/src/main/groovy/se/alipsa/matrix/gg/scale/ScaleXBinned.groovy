package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned position scale for the x-axis.
 * Divides continuous data into equal-width bins and maps values to bin centers.
 * <p>
 * This scale is useful for creating histogram-like visualizations with discrete geoms
 * or for discretizing continuous position data. Values within the same bin are mapped
 * to the same position (the bin center), creating distinct groupings.
 * <p>
 * Example usage:
 * <pre>
 * def chart = ggplot(data, aes(x: 'value', y: 'count')) +
 *   geom_point() +
 *   scale_x_binned(bins: 10)
 * </pre>
 *
 * @see ScaleYBinned
 * @see ScaleBinned
 */
@CompileStatic
class ScaleXBinned extends ScaleBinned {

  ScaleXBinned() {
    aesthetic = 'x'
    position = 'bottom'
  }

  ScaleXBinned(Map params) {
    aesthetic = 'x'
    position = 'bottom'
    applyBinnedParams(params)
  }

  /**
   * Apply coordinate transformation for x-axis if configured.
   *
   * @param value the value to transform
   * @return the transformed value, or null if transformation failed
   */
  @Override
  protected BigDecimal applyCoordTransform(BigDecimal value) {
    if (coordTrans != null && coordTrans.hasXTransformation()) {
      return coordTrans.transformX(value)
    }
    return value
  }
}
