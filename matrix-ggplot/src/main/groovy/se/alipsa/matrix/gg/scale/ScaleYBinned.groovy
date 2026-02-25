package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Binned position scale for the y-axis.
 * Divides continuous data into equal-width bins and maps values to bin centers.
 * <p>
 * This scale is useful for creating histogram-like visualizations with discrete geoms
 * or for discretizing continuous position data. Values within the same bin are mapped
 * to the same position (the bin center), creating distinct groupings.
 * <p>
 * Example usage:
 * <pre>
 * def chart = ggplot(data, aes(x: 'category', y: 'value')) +
 *   geom_point() +
 *   scale_y_binned(bins: 10)
 * </pre>
 *
 * @see ScaleXBinned
 * @see ScaleBinned
 */
@CompileStatic
class ScaleYBinned extends ScaleBinned {

  ScaleYBinned() {
    aesthetic = 'y'
    position = 'left'
  }

  ScaleYBinned(Map params) {
    aesthetic = 'y'
    position = 'left'
    applyBinnedParams(params)
  }

  /**
   * Apply coordinate transformation for y-axis if configured.
   *
   * @param value the value to transform
   * @return the transformed value, or null if transformation failed
   */
  @Override
  protected BigDecimal applyCoordTransform(BigDecimal value) {
    if (coordTrans != null && coordTrans.hasYTransformation()) {
      return coordTrans.transformY(value)
    }
    return value
  }
}
