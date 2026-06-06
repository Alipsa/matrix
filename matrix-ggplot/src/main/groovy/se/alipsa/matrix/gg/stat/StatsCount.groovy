package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

@SuppressWarnings('ClassJavadoc')
class StatsCount extends Stats {

  StatsCount(Map params = [:]) {
    super(StatType.COUNT, params)
  }
}
