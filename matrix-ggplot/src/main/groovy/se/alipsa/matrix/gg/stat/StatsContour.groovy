package se.alipsa.matrix.gg.stat

import se.alipsa.matrix.gg.layer.StatType

@SuppressWarnings('ClassJavadoc')
class StatsContour extends Stats {

  StatsContour(Map params = [:]) {
    super(StatType.CONTOUR, params)
  }
}
