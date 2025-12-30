package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

class StatCount extends Stat {

  StatCount(Map params = [:]) {
    super(StatType.COUNT, params)
  }
}
