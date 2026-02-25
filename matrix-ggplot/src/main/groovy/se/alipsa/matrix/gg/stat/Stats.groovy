package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.gg.layer.StatType

@CompileStatic
class Stats {

  final StatType statType
  final Map params

  Stats(StatType statType, Map params = [:]) {
    this.statType = statType
    this.params = params ?: [:]
  }

  /**
   * Converts this gg stat wrapper to a charm StatSpec.
   *
   * @return equivalent charm StatSpec with type and params
   */
  StatSpec toCharmStatSpec() {
    CharmStatType charmType = CharmStatType.valueOf(statType.name())
    StatSpec.of(charmType, params as Map<String, Object>)
  }
}
