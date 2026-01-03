package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

@CompileStatic
class Stats {

  final StatType statType
  final Map params

  Stats(StatType statType, Map params = [:]) {
    this.statType = statType
    this.params = params ?: [:]
  }
}
