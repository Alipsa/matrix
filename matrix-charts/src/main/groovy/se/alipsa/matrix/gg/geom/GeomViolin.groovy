package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.aes.Aes

@CompileStatic
class GeomViolin extends Geom {

    Aes aes

    GeomViolin(Aes aes) {
        this.aes = aes
    }
}
