package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

class StatsSum extends Stats {

  StatsSum(Map params = [:]) {
    super(StatType.SUMMARY, params)
  }
}
