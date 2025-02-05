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

  ChartBuilder addLegend(Legend legend) {
    this
  }

  ChartBuilder addGridLines(GridLines gridLines) {
    this
  }

  ChartBuilder addTitle(Title title) {
    this
  }

  ChartBuilder addSubTitle(SubTitle subTitle) {
    this
  }

  ChartBuilder addCoordinateSystem(CoordinateSystem coordinateSystem) {
    this
  }

}
