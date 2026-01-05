package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_bin configuration.
 */
class StatsBin extends Stats {

  /**
   * Create a stat_bin specification.
   *
   * @param params optional stat parameters (e.g. bins, binwidth)
   */
  StatsBin(Map params = [:]) {
    super(StatType.BIN, params)
  }
}
