package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_ellipse configuration.
 * Computes confidence ellipse for bivariate normal data.
 */
class StatsEllipse extends Stats {

  /**
   * Create a stat_ellipse specification.
   *
   * @param params stat parameters: level, type, segments, etc.
   */
  StatsEllipse(Map params = [:]) {
    super(StatType.ELLIPSE, params)
  }
}
