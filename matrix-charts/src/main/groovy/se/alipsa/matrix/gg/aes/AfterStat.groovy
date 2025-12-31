package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic

/**
 * Wrapper for referencing computed statistics in aesthetic mappings.
 * Used with after_stat() to map computed variables from stat transformations.
 *
 * In ggplot2, after_stat(count) references the 'count' variable computed by
 * stat transformations like stat_bin or stat_count.
 *
 * Common computed variables:
 * - count: number of observations in each bin/group
 * - density: density of observations (count / total / binwidth)
 * - ncount: count normalized to maximum of 1
 * - ndensity: density normalized to maximum of 1
 *
 * Usage:
 * aes(x: 'displ', y: after_stat('count'))
 */
@CompileStatic
class AfterStat {

  /** The name of the computed statistic to reference */
  final String stat

  AfterStat(String stat) {
    if (!stat) {
      throw new IllegalArgumentException("stat name cannot be null or empty")
    }
    this.stat = stat
  }

  /**
   * Get the computed statistic name.
   */
  String getStat() {
    return stat
  }

  @Override
  String toString() {
    return "after_stat(${stat})"
  }

  @Override
  boolean equals(Object obj) {
    if (obj instanceof AfterStat) {
      return stat == ((AfterStat) obj).stat
    }
    return false
  }

  @Override
  int hashCode() {
    return stat.hashCode()
  }
}
