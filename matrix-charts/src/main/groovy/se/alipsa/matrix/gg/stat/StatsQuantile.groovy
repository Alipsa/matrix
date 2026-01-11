package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_quantile configuration.
 *
 * Quantile regression fits lines that estimate conditional quantiles
 * rather than the conditional mean (as in ordinary least squares).
 *
 * Example:
 * <pre>
 * // Fit 25th, 50th, and 75th percentile lines
 * chart + stat_quantile(quantiles: [0.25, 0.5, 0.75])
 * </pre>
 */
class StatsQuantile extends Stats {

  /**
   * Create a stat_quantile specification.
   *
   * @param params Optional stat parameters:
   *   - quantiles: List of quantiles to fit (default: [0.25, 0.5, 0.75])
   *   - n: Number of fitted points (default: 80)
   */
  StatsQuantile(Map params = [:]) {
    super(StatType.QUANTILE, params)
  }
}
