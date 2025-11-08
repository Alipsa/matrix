package se.alipsa.matrix.gg


import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.facet.Facet
import se.alipsa.matrix.gg.geom.Geom
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.stat.Stat
import se.alipsa.matrix.gg.theme.Theme

class GgChart {

  GgChart(Matrix data, Aes aes) {
  }

  GgChart plus(Geom geom) {
    return this
  }

  GgChart plus(Theme theme) {
    return this
  }

  GgChart plus(Stat stat) {
    return this
  }

  GgChart plus(Facet facet) {
    return this
  }

  GgChart plus(Coord coord) {
    return this
  }

  GgChart plus(Scale scale) {
    return this
  }
}
