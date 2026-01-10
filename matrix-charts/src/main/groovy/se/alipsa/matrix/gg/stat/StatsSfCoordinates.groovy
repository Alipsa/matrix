package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

/**
 * Wrapper for stat_sf_coordinates configuration.
 */
class StatsSfCoordinates extends Stats {

  /**
   * Create a stat_sf_coordinates specification.
   *
   * @param params optional stat parameters (e.g. geometry column override)
   */
  StatsSfCoordinates(Map params = [:]) {
    super(StatType.SF_COORDINATES, params)
  }
}
