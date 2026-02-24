package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_bin_hex configuration.
 * Performs hexagonal binning and computes counts for each hex bin.
 */
@CompileStatic
class StatsBinHex extends Stats {

  /**
   * Create a stat_bin_hex specification.
   *
   * @param params stat parameters: bins, binwidth, etc.
   */
  StatsBinHex(Map params = [:]) {
    super(StatType.BIN_HEX, params)
  }
}
