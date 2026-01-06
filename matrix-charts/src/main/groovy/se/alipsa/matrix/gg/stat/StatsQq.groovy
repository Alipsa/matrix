package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_qq configuration.
 */
class StatsQq extends Stats {

  /**
   * Create a stat_qq specification.
   *
   * @param params optional stat parameters (e.g. distribution)
   */
  StatsQq(Map params = [:]) {
    super(StatType.QQ, params)
  }
}
