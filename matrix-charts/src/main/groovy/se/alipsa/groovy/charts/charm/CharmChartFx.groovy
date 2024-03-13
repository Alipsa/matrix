package se.alipsa.groovy.charts.charm

import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color

class CharmChartFx extends BorderPane {

  ChartPane titleArea
  ChartPane legendArea
  StackPane plotArea

  CharmChartFx(Insets padding = new Insets(3)) {
    titleArea = new ChartPane(padding)
    legendArea = new ChartPane(padding)
    plotArea = new StackPane()
    plotArea.setPadding(padding)
    plotArea.setStyle("-fx-border-color: yellow")
    setPadding(padding)
    setCenter(titleArea)
    titleArea.setCenter(legendArea)

    legendArea.setCenter(plotArea)
  }

  TitlePane addTitle(String title, Position position) {
    TitlePane titlePane = new TitlePane()
    Label label = new Label(title)
    titlePane.getChildren().add(label)
    add(titlePane, position)
    titlePane
  }

  CharmChartFx add(TitlePane titlePane, Position position) {
    titlePane.setAlignment(position.alignment)
    titleArea.add(titlePane, position)
    this
  }

  CharmChartFx add(LegendPane legendPane, Position position) {
    //legendPane.setAlignment(position.alignment)
    //if (position.side in [Side.LEFT, Side.RIGHT]) {
    //  legendPane.setOrientation(Orientation.VERTICAL)
    //}
    legendArea.add(legendPane, position)
    this
  }

  LegendPane addLegend(Map<String,Color> content, Position position) {
    LegendPane legend = position.side in [Side.LEFT, Side.RIGHT] ? new VerticalLegendPane() : new HorizontalLegendPane()
    legend.addItems(content)
    add(legend, position)
    legend
  }

  CharmChartFx add(PlotPane plotPane) {
    var plots = plotArea.getChildren()
    if (plots.size() > 0) {
      plotPane.setStyle('-fx-background-color: null')
      //plotPane.getGraphicsContext2D().setFill(Color.TRANSPARENT) // not sure if we ned this or if it wrecks the whole plot
    }
    plots.add(plotPane)
    this
  }
}
