package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

@CompileStatic
class Stat {

  final StatType statType
  final Map params

  Stat(StatType statType, Map params = [:]) {
    this.statType = statType
    this.params = params ?: [:]
  }
}
