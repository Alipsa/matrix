package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

@CompileStatic
class StatBin2d extends Stat {

  StatBin2d(Map params = [:]) {
    super(StatType.BIN2D, params)
  }
}
