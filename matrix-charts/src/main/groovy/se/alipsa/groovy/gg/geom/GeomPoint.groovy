package se.alipsa.groovy.gg.geom

class GeomPoint extends Geom {

    String color

    GeomPoint() {

    }

    GeomPoint(Map params) {
        color = params.color
    }
}
