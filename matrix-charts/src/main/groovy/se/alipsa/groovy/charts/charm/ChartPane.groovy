package se.alipsa.groovy.charts.charm

import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox


class ChartPane extends BorderPane {

  HBox topPane = new HBox()
  VBox rightPane = new VBox()
  HBox bottomPane = new HBox()
  VBox leftPane = new VBox()

  ChartPane(Insets padding = new Insets(2)) {
    setStyle("-fx-border-color: navy")
    setPadding(padding)
    setTop(topPane)
    setRight(rightPane)
    setBottom(bottomPane)
    setLeft(leftPane)
  }

  ChartPane add(LegendPane node, Position position) {
    add((Node)node, position)
  }

  ChartPane add(Node node, Position position) {
    switch(position.side) {
      case Side.TOP -> {
        topPane.getChildren().add(node)
        topPane.setAlignment(position.alignment)
        setAlignment(topPane, position.alignment)
      }
      case Side.RIGHT -> {
        rightPane.getChildren().add(node)
        rightPane.setAlignment(position.alignment)
        setAlignment(rightPane, position.alignment)
      }
      case Side.BOTTOM -> {
        bottomPane.getChildren().add(node)
        bottomPane.setAlignment(position.alignment)
        setAlignment(bottomPane, position.alignment)
      }
      case Side.LEFT -> {
        leftPane.getChildren().add(node)
        leftPane.setAlignment(position.alignment)
        setAlignment(leftPane, position.alignment)
      }
    }
    this
  }

}
