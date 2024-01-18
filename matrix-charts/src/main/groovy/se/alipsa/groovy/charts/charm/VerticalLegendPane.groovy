package se.alipsa.groovy.charts.charm

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Shape

import static se.alipsa.groovy.charts.util.ColorUtil.asHexString
import static se.alipsa.groovy.charts.util.StyleUtil.addStyle

class VerticalLegendPane extends VBox implements LegendPane {

    VerticalLegendPane(){
        setSpacing(2)
        setPadding(new Insets(2, 3, 2, 3))
    }


}
