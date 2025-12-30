package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

@CompileStatic
class ScaleColorManual extends Scale {

  Map mappings

  ScaleColorManual(Map mappings) {
    this.mappings = mappings
  }
}
