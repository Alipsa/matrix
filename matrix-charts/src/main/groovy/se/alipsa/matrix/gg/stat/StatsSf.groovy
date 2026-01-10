package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_sf configuration.
 */
class StatsSf extends Stats {

  /**
   * Create a stat_sf specification.
   *
   * @param params optional stat parameters (e.g. geometry column override)
   */
  StatsSf(Map params = [:]) {
    super(StatType.SF, params)
  }
}
