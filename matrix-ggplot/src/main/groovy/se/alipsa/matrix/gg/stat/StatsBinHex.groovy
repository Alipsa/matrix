package se.alipsa.matrix.gg.stat


import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_bin_hex configuration.
 * Performs hexagonal binning and computes counts for each hex bin.
 */
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
