package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

class StatsCount extends Stats {

  StatsCount(Map params = [:]) {
    super(StatType.COUNT, params)
  }
}
