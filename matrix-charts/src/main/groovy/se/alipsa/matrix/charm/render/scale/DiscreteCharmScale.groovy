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
    new ArrayList<Object>(levels)
  }

  @Override
  List<String> tickLabels(int count) {
    new ArrayList<>(levels)
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
}
