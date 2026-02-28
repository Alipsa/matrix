package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.ValueConverter

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
    if (value == null || binBoundaries.isEmpty()) {
      return null
    }

    BigDecimal numeric
    if (TemporalScaleUtil.isTemporalTransform(scaleSpec?.transformStrategy)) {
      numeric = TemporalScaleUtil.toCanonicalValue(value, scaleSpec?.transformStrategy, scaleSpec?.params ?: [:])
    } else {
      numeric = ValueConverter.asBigDecimal(value)
    }
    if (numeric == null) {
      return null
    }

    BigDecimal center = findBinCenter(numeric)
    if (center == null) return null

    ScaleUtils.linearTransform(center, domainMin, domainMax, rangeStart, rangeEnd)
  }

  @Override
  List<Object> ticks(int count) {
    List<Object> configured = resolveConfiguredBreaks()
    if (!configured.isEmpty()) {
      return configured
    }
    new ArrayList<Object>(binBoundaries)
  }

  @Override
  List<String> tickLabels(int count) {
    List<Object> tickValues = ticks(count)
    List<String> configured = scaleSpec?.labels
    if (configured != null && !configured.isEmpty()) {
      List<String> labels = []
      tickValues.eachWithIndex { Object tick, int idx ->
        if (idx < configured.size() && configured[idx] != null) {
          labels << configured[idx]
          return
        }
        labels << defaultTickLabel(tick)
      }
      return labels
    }
    tickValues.collect { Object tick ->
      defaultTickLabel(tick)
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

  private List<Object> resolveConfiguredBreaks() {
    List configured = scaleSpec?.breaks
    if (configured == null || configured.isEmpty()) {
      return []
    }
    if (TemporalScaleUtil.isTemporalTransform(scaleSpec?.transformStrategy)) {
      return ScaleUtils.coerceConfiguredBreaksOrThrow(
          configured,
          { Object value ->
            TemporalScaleUtil.toCanonicalValue(value, scaleSpec?.transformStrategy, scaleSpec?.params ?: [:])
          },
          'temporal'
      )
    }
    ScaleUtils.coerceConfiguredBreaksOrThrow(
        configured,
        { Object value -> ValueConverter.asBigDecimal(value) },
        'numeric'
    )
  }

  private String defaultTickLabel(Object tick) {
    if (tick == null) {
      return ''
    }
    if (TemporalScaleUtil.isTemporalTransform(scaleSpec?.transformStrategy)) {
      BigDecimal numeric = ValueConverter.asBigDecimal(tick)
      return TemporalScaleUtil.formatTick(
          numeric,
          scaleSpec?.transformStrategy,
          scaleSpec?.params ?: [:]
      )
    }
    BigDecimal numeric = ValueConverter.asBigDecimal(tick)
    if (numeric == null) {
      return tick.toString()
    }
    numeric.stripTrailingZeros().toPlainString()
  }
}
