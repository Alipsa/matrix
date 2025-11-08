package se.alipsa.matrix.charts.charmfx

import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Shape

import static se.alipsa.matrix.charts.util.ColorUtil.asHexString
import static se.alipsa.matrix.charts.util.StyleUtil.addStyle

trait LegendPane {

  abstract ObservableList<Node> getChildren()

  abstract void setBorder(Border border)

  void addItem(Shape shape, String text) {
    Label label = new Label(text)
    label.setPadding(new Insets(0, 4, 0, 2))
    HBox itemBox = new HBox()
    itemBox.setAlignment(Pos.CENTER_LEFT)
    itemBox.getChildren().addAll(shape, label)
    getChildren().add(itemBox)
  }

  void addItems(Map<String, Color> items) {
    items.each {
      Circle circle = new Circle(6, it.value)
      addItem(circle, it.key)
    }
  }

  LegendPane setBackground(Color color) {
    addStyle((Node) this, "-fx-background-color: ${asHexString(color)};")
    this
  }

  LegendPane setBorder(Color color = Color.DARKGRAY, BorderStrokeStyle borderStrokeStyle = BorderStrokeStyle.SOLID,
      CornerRadii cornerRadii = CornerRadii.EMPTY, BorderWidths borderWidths = BorderWidths.DEFAULT) {
    Border border = new Border(new BorderStroke(color, borderStrokeStyle, cornerRadii, borderWidths))
    setBorder(border)
    this
  }
}
