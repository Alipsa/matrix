package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

class StatBoxplot extends Stat {

  StatBoxplot(Map params = [:]) {
    super(StatType.BOXPLOT, params)
  }
}
