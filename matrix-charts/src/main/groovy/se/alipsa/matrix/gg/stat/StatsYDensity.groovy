package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_ydensity configuration.
 */
class StatsYDensity extends Stats {

  /**
   * Create a stat_ydensity specification.
   *
   * @param params optional stat parameters (e.g. adjust, kernel)
   */
  StatsYDensity(Map params = [:]) {
    super(StatType.YDENSITY, params)
  }
}
