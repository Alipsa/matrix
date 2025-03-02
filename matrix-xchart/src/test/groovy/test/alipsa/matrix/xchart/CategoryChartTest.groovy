package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.xchart.BarChart
import se.alipsa.matrix.xchart.StickChart

import static org.junit.jupiter.api.Assertions.assertTrue

class CategoryChartTest {

  @Test
  void testStickChart() {
    Matrix matrix = new MatrixBuilder().data(
        xData: -3..24,
        yData: -3..24
    ).types(Integer, Integer)
    .build()

    def sc = StickChart.create(matrix, 800, 600)
        .setTitle("Stick")
        .addSeries("data", matrix["xData"], matrix["yData"])

    sc.getSeries("data").marker = SeriesMarkers.CIRCLE
    //sc.style.seriesMarkers = [SeriesMarkers.CIRCLE] // another way to do the same thing
    sc.style.setLegendPosition(Styler.LegendPosition.InsideNW)

    File file = new File("build/testStickChart.svg")
    sc.exportSvg(file)
    assertTrue(file.exists())
  }

  @Test
  void testBarChart() {
    Matrix matrix = new MatrixBuilder().data(
        name: ['Per', 'Karin', 'Tage', 'Sixten', 'Ulrik'],
        score: [4, 5, 9, 6, 5]
    ).types(String, Number)
        .build()
    def bc = BarChart.create(matrix, 800, 600)
      .addSeries(matrix['name'], matrix['score'])

    File file = new File("build/testBarChart.png")
    bc.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testBarChartStacked() {
    Matrix matrix = new MatrixBuilder().data(
        name: ['Per', 'Karin', 'Tage', 'Sixten', 'Ulrik'],
        score: [4, 5, 9, 6, 5],
        addon: [4, 3, 2, 3, 4]
    ).types(String, Number, Number)
        .build()
    def bc = BarChart.createStacked(matrix, 800, 600)
        .addSeries(matrix['name'], matrix['score'])
        .addSeries(matrix['name'], matrix['addon'])

    File file = new File("build/testBarChartStacked.png")
    bc.exportPng(file)
    assertTrue(file.exists())
  }
}
