package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.xchart.PieChart

import static org.junit.jupiter.api.Assertions.assertTrue

class PieChartTest {

  Matrix matrix = new MatrixBuilder().data(
      metal: ['Gold', 'Silver', 'Platinum', 'Copper', 'Zinc'],
      ratio: [24, 21, 39, 17, 40]
  ).matrixName('Metal ratio')
      .types(String, Number)
      .build()

  @Test
  void testPieChart() {
    File file = new File("build/testPieChart.png")
    def pc = PieChart.create(matrix)
        .addSeries(matrix.metal, matrix.ratio)
    
    pc.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testDonutChart() {
    def dc = PieChart.createDonut(matrix)
        .addSeries(matrix['metal'], matrix['ratio'])
    File file = new File("build/testDonutChart.png")
    dc.exportPng(file)
    assertTrue(file.exists())
  }
}
