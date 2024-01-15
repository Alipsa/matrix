package se.alipsa.groovy.charts.charm

import javafx.geometry.Side
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane

class CharmChartFx extends BorderPane {

  ChartPane titleArea = new ChartPane()
  ChartPane legendArea = new ChartPane()
  StackPane plotArea = new StackPane()

  CharmChartFx() {
    setCenter(titleArea)
    titleArea.setCenter(legendArea)
    legendArea.setCenter(plotArea)
  }

  CharmChartFx addTitle(String title, Side side) {
    TitlePane titlePane = new TitlePane()
    Label label = new Label(title)
    titlePane.getChildren().add(label)
    add(titlePane, side)
  }

  CharmChartFx add(TitlePane titlePane, Side side) {
    titleArea.add(titlePane, side)
    this
  }

  CharmChartFx add(LegendPane legendPane, Side side) {
    legendArea.add(legendPane, side)
    this
  }


}
