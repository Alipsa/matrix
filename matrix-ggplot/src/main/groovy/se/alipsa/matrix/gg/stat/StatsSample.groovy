package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_sample configuration.
 * Subsamples large datasets before rendering.
 */
class StatsSample extends Stats {

  /**
   * Create a stat_sample specification.
   *
   * @param params optional stat parameters (e.g. n, seed, method)
   */
  StatsSample(Map params = [:]) {
    super(StatType.SAMPLE, params)
  }
}
