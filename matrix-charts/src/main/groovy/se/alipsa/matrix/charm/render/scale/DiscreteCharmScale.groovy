package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic

/**
 * Trained discrete scale for categorical data.
 *
 * Maps categorical levels to evenly spaced band positions within the output range.
 * Each level is centered within its band.
 */
@CompileStatic
class DiscreteCharmScale extends CharmScale {

  /** Ordered list of discrete levels. */
  List<String> levels = []

  @Override
  BigDecimal transform(Object value) {
    if (value == null || levels.isEmpty()) return null

    int idx = levels.indexOf(value.toString())
    if (idx < 0) return null

    BigDecimal step = (rangeEnd - rangeStart) / levels.size()
    rangeStart + step * idx + step / 2
  }

  @Override
  List<Object> ticks(int count) {
    List<String> configuredBreaks = resolveConfiguredBreaks()
    if (!configuredBreaks.isEmpty()) {
      List<String> filtered = configuredBreaks.findAll { String level -> levels.contains(level) }
      if (!filtered.isEmpty()) {
        return new ArrayList<Object>(filtered)
      }
    }
    new ArrayList<Object>(levels)
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
        labels << tick?.toString()
      }
      return labels
    }
    tickValues.collect { Object tick -> tick?.toString() ?: '' }
  }

  @Override
  boolean isDiscrete() {
    true
  }

  /**
   * Returns the width of each category band.
   *
   * @return band width in pixels
   */
  BigDecimal getBandwidth() {
    if (levels.isEmpty()) return 0
    (rangeEnd - rangeStart).abs() / levels.size()
  }

  private List<String> resolveConfiguredBreaks() {
    List configured = scaleSpec?.breaks
    if (configured == null || configured.isEmpty()) {
      return []
    }
    configured.findResults { Object value ->
      value?.toString()
    } as List<String>
  }
}
