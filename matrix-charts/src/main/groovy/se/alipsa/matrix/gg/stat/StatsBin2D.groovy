package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

@CompileStatic
class StatsBin2D extends Stats {

  StatsBin2D(Map params = [:]) {
    super(StatType.BIN2D, params)
  }
}
