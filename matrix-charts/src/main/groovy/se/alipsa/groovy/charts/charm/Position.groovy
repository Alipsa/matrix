package se.alipsa.groovy.charts.charm

import javafx.geometry.Pos
import javafx.geometry.Side

enum Position {

    TOP_LEFT(Side.TOP, Pos.CENTER_LEFT),
    TOP_CENTER(Side.TOP, Pos.CENTER),
    TOP_RIGHT(Side.TOP, Pos.CENTER_RIGHT),
    RIGHT_TOP(Side.RIGHT, Pos.TOP_CENTER),
    RIGHT_CENTER(Side.RIGHT, Pos.CENTER),
    RIGHT_BOTTOM(Side.RIGHT, Pos.BOTTOM_CENTER),
    BOTTOM_RIGHT(Side.BOTTOM, Pos.CENTER_RIGHT),
    BOTTOM_CENTER(Side.BOTTOM, Pos.CENTER),
    BOTTOM_LEFT(Side.BOTTOM, Pos.CENTER_LEFT),
    LEFT_BOTTOM(Side.LEFT, Pos.BOTTOM_CENTER),
    LEFT_CENTER(Side.LEFT, Pos.CENTER),
    LEFT_TOP(Side.LEFT, Pos.TOP_CENTER)

    Side side
    Pos alignment

    Position(Side side, Pos alignment) {
        this.side = side
        this.alignment = alignment
    }

}