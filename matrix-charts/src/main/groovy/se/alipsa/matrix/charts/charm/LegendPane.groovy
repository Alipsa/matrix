package se.alipsa.matrix.charts.charm

import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Shape

import static se.alipsa.matrix.charts.util.ColorUtil.asHexString
import static se.alipsa.matrix.charts.util.StyleUtil.addStyle

interface LegendPane {


    ObservableList<Node> getChildren()
    void setBorder(Border border)

    default void addItem(Shape shape, String text) {
        Label label = new Label(text)
        label.setPadding(new Insets(0,4,0,2))
        HBox itemBox = new HBox()
        itemBox.setAlignment(Pos.CENTER_LEFT)
        itemBox.getChildren().addAll(shape, label)
        getChildren().add(itemBox)
    }

    default void addItems(Map<String, Color> items) {
        items.each {
            Circle circle = new Circle(6, it.value)
            addItem(circle, it.key)
        }

    }

    default LegendPane setBackground(Color color) {
        addStyle((Node)this, "-fx-background-color: ${asHexString(color)};")
        this
    }

    default LegendPane setBorder(Color color = Color.DARKGRAY, BorderStrokeStyle borderStrokeStyle = BorderStrokeStyle.SOLID,
                         CornerRadii cornerRadii = CornerRadii.EMPTY, BorderWidths borderWidths = BorderWidths.DEFAULT) {
        Border border = new Border(new BorderStroke(color, borderStrokeStyle, cornerRadii, borderWidths))
        setBorder(border)
        this
    }
}
