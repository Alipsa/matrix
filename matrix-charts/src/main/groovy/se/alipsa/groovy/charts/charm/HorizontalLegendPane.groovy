package se.alipsa.groovy.charts.charm

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Shape

import static se.alipsa.groovy.charts.util.ColorUtil.asHexString
import static se.alipsa.groovy.charts.util.StyleUtil.addStyle

class HorizontalLegendPane extends HBox implements LegendPane {

    HorizontalLegendPane(){
        setSpacing(2)
        setPadding(new Insets(2, 3, 2, 3))
    }
}
