package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_density configuration.
 */
class StatsDensity extends Stats {

  /**
   * Create a stat_density specification.
   *
   * @param params optional stat parameters (e.g. adjust, kernel)
   */
  StatsDensity(Map params = [:]) {
    super(StatType.DENSITY, params)
  }
}
