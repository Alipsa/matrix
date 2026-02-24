package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_qq_line configuration.
 */
class StatsQqLine extends Stats {

  /**
   * Create a stat_qq_line specification.
   *
   * @param params optional stat parameters (e.g. distribution)
   */
  StatsQqLine(Map params = [:]) {
    super(StatType.QQ_LINE, params)
  }
}
