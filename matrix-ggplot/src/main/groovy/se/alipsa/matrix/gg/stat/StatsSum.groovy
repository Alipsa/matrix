package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

@SuppressWarnings('ClassJavadoc')
class StatsSum extends Stats {

  StatsSum(Map params = [:]) {
    super(StatType.SUMMARY, params)
  }
}
