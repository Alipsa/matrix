package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_smooth configuration.
 */
class StatsSmooth extends Stats {

  /**
   * Create a stat_smooth specification.
   *
   * @param params optional stat parameters (e.g. method, n, se)
   */
  StatsSmooth(Map params = [:]) {
    super(StatType.SMOOTH, params)
  }
}
