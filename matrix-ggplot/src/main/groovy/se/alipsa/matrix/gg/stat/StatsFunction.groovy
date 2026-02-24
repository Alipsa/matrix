package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_function configuration.
 * Computes y values from function of x.
 */
class StatsFunction extends Stats {

  /**
   * Create a stat_function specification.
   *
   * @param params stat parameters: fun (Closure), xlim, n, etc.
   */
  StatsFunction(Map params = [:]) {
    super(StatType.FUNCTION, params)
  }
}
