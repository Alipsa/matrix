package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
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
}
