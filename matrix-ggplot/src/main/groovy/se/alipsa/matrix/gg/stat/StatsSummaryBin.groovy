package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_summary_bin configuration.
 * Bins x and computes summary statistics for y in each bin.
 */
class StatsSummaryBin extends Stats {

  /**
   * Create a stat_summary_bin specification.
   *
   * @param params stat parameters: bins, binwidth, fun, fun.data, etc.
   */
  StatsSummaryBin(Map params = [:]) {
    super(StatType.SUMMARY_BIN, params)
  }
}
