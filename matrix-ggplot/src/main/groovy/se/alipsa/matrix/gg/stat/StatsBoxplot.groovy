package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

@SuppressWarnings('ClassJavadoc')
class StatsBoxplot extends Stats {

  StatsBoxplot(Map params = [:]) {
    super(StatType.BOXPLOT, params)
  }
}
