package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Trained binned scale.
 *
 * Maps continuous values into discrete bins, then transforms bin centers
 * to pixel coordinates via linear interpolation.
 */
@CompileStatic
class BinnedCharmScale extends CharmScale {

  /** Bin boundary values (n+1 values for n bins). */
  List<BigDecimal> binBoundaries = []

  /** Bin center values (n values for n bins). */
  List<BigDecimal> binCenters = []

  /** Domain minimum (first boundary). */
  BigDecimal domainMin

  /** Domain maximum (last boundary). */
  BigDecimal domainMax

  @Override
  BigDecimal transform(Object value) {
    if (value == null || binBoundaries.isEmpty()) return null

    BigDecimal numeric = NumberCoercionUtil.coerceToBigDecimal(value)
    if (numeric == null) return null

    BigDecimal center = findBinCenter(numeric)
    if (center == null) return null

    ScaleUtils.linearTransform(center, domainMin, domainMax, rangeStart, rangeEnd)
  }

  @Override
  List<Object> ticks(int count) {
    new ArrayList<Object>(binBoundaries)
  }

  @Override
  List<String> tickLabels(int count) {
    binBoundaries.collect { BigDecimal b ->
      b.stripTrailingZeros().toPlainString()
    }
  }

  @Override
  boolean isDiscrete() {
    false
  }

  /**
   * Find the bin center for a given value.
   *
   * @param value numeric value
   * @return bin center or null if value is outside all bins
   */
  private BigDecimal findBinCenter(BigDecimal value) {
    if (binBoundaries.size() < 2) return null

    for (int i = 0; i < binBoundaries.size() - 1; i++) {
      if (value >= binBoundaries[i] && value <= binBoundaries[i + 1]) {
        return binCenters.size() > i ? binCenters[i] : (binBoundaries[i] + binBoundaries[i + 1]) / 2
      }
    }

    // Clamp to nearest bin
    if (value < binBoundaries.first()) {
      return binCenters.isEmpty() ? binBoundaries.first() : binCenters.first()
    }
    if (value > binBoundaries.last()) {
      return binCenters.isEmpty() ? binBoundaries.last() : binCenters.last()
    }
    null
  }
}
