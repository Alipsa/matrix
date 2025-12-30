package se.alipsa.matrix.charm

/**
 * This is the chart drawing class.
 * Each element of the chart is drawn on top of the previous.
 */
class ChartBuilder {
  int width
  int height

  ChartBuilder(int width, int height) {
    this.width = width
    this.height = height
  }

  ChartBuilder addGraph(Graph chart) {
    this
  }

  ChartBuilder setLegend(Legend legend) {
    this
  }

  ChartBuilder setGridLines(GridLines gridLines) {
    this
  }

  ChartBuilder setTitle(Title title) {
    this
  }

  ChartBuilder setSubTitle(SubTitle subTitle) {
    this
  }

  ChartBuilder setCoordinateSystem(CoordinateSystem coordinateSystem) {
    this
  }

  ChartBuilder setStyle(Style style) {
    this
  }

}
