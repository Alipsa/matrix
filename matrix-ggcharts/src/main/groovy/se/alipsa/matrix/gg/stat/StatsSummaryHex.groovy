package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_summary_hex configuration.
 * Performs hexagonal binning and computes summary statistics for a z variable in each hex bin.
 */
@CompileStatic
class StatsSummaryHex extends Stats {

  /**
   * Create a stat_summary_hex specification.
   *
   * @param params stat parameters: bins, binwidth, fun, fun.data, etc.
   */
  StatsSummaryHex(Map params = [:]) {
    super(StatType.SUMMARY_HEX, params)
  }
}
