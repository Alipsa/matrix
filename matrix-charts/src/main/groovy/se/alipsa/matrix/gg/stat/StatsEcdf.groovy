package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_ecdf configuration.
 */
class StatsEcdf extends Stats {

  /**
   * Create a stat_ecdf specification.
   *
   * @param params optional stat parameters (e.g. pad)
   */
  StatsEcdf(Map params = [:]) {
    super(StatType.ECDF, params)
  }
}
