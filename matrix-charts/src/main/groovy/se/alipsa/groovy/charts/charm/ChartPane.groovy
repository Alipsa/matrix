package se.alipsa.groovy.charts.charm

import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane


class ChartPane extends BorderPane {

  FlowPane topPane = new FlowPane()
  FlowPane rightPane = new FlowPane()
  FlowPane bottomPane = new FlowPane()
  FlowPane leftPane = new FlowPane()

  ChartPane() {
    setTop(topPane)
    setRight(rightPane)
    setBottom(bottomPane)
    setLeft(leftPane)
  }

  ChartPane add(Node node, Side side) {
    switch(side) {
      case Side.TOP -> topPane.getChildren().add(node)
      case Side.RIGHT -> rightPane.getChildren().add(node)
      case Side.BOTTOM -> bottomPane.getChildren().add(node)
      case Side.LEFT -> leftPane.getChildren().add(node)
    }
    this
  }

}
