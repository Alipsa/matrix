package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

class StatSum extends Stat {

  StatSum(Map params = [:]) {
    super(StatType.SUMMARY, params)
  }
}
