package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_unique configuration.
 * Removes duplicate observations.
 */
class StatsUnique extends Stats {

  /**
   * Create a stat_unique specification.
   *
   * @param params optional stat parameters (e.g. columns)
   */
  StatsUnique(Map params = [:]) {
    super(StatType.UNIQUE, params)
  }
}
