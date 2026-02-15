package se.alipsa.matrix.gg.adapter

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Chart

/**
 * Result of adapting a gg chart to a Charm chart.
 */
@CompileStatic
class GgCharmAdaptation {

  final boolean delegated
  final Chart charmChart
  final List<String> reasons

  /**
   * Creates a new adaptation result.
   *
   * @param delegated true when render should be delegated to Charm
   * @param charmChart adapted Charm chart (when delegated)
   * @param reasons fallback reasons (when not delegated)
   */
  GgCharmAdaptation(boolean delegated, Chart charmChart, List<String> reasons = []) {
    this.delegated = delegated
    this.charmChart = charmChart
    this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons ?: []))
  }

  /**
   * Creates a delegated result.
   *
   * @param chart adapted chart
   * @return delegated result
   */
  static GgCharmAdaptation delegated(Chart chart) {
    new GgCharmAdaptation(true, chart, [])
  }

  /**
   * Creates a fallback result.
   *
   * @param reasons fallback reasons
   * @return fallback result
   */
  static GgCharmAdaptation fallback(List<String> reasons) {
    new GgCharmAdaptation(false, null, reasons)
  }
}
