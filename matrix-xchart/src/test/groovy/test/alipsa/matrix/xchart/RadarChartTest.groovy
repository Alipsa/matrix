package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.RadarChart
import org.knowm.xchart.RadarChartBuilder
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Normalize

import static org.junit.jupiter.api.Assertions.assertTrue

class RadarChartTest {

  @Test
  void testSimpleRadarChart() {
    RadarChart chart =
        new RadarChartBuilder().width(800).height(600).title(getClass().getSimpleName()).build();
    chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);

    // Series
    chart.setRadiiLabels(
        new String[] {
            "Sales",
            "Marketing",
            "Development",
            "Customer Support",
            "Information Technology",
            "Administration"
        });
    chart.addSeries(
        "Old System",
        new double[] {0.78, 0.85, 0.80, 0.82, 0.93, 0.92},
        new String[] {"Lowest variable 78%", "85%", null, null, null, null});
    chart.addSeries("New System", new double[] {0.67, 0.73, 0.97, 0.95, 0.93, 0.73});
    chart.addSeries("Experimental System", new double[] {0.37, 0.93, 0.57, 0.55, 0.33, 0.73});
    def file = new File("build/testSimpleRadarChart.png")
    BitmapEncoder.saveBitmap(chart, file.absolutePath, BitmapEncoder.BitmapFormat.PNG)
    assertTrue(file.exists())

    Matrix m = Matrix.builder()
        .columnNames('System', 'Sales', 'Marketing', 'Development',
        'Customer Support', 'Information Technology', 'Administration')
        .rows([
            ['Old System', 0.78, 0.85, 0.80, 0.82, 0.93, 0.92],
            ['New System', 0.67, 0.73, 0.97, 0.95, 0.93, 0.73],
            ['Experimental System', 0.37, 0.93, 0.57, 0.55, 0.33, 0.73]
        ]).types([String] + [Number]*6 as List<Class>).build()
    def rc = se.alipsa.matrix.xchart.RadarChart.create(m, 800, 600)
    rc.addSeries('System')
    def file2 = new File("build/testSimpleRadarChart2.png")
    rc.exportPng(file2)
    assertTrue(file2.exists())
  }

  @Test
  void testTutorialExample() {
    // Create a Matrix with data for the radar chart
    Matrix matrix = Matrix.builder()
        .columnNames("Player", "Speed", "Power", "Agility", "Endurance", "Accuracy")
        .rows([
            ['Player1', 8.0, 2.0, 9.0, 6.0, 8.0],
            ['Player2', 6.0, 9.0, 7.0, 3.0, 7.0],
            ['Player3', 3.0, 3.5, 5.2, 9.1, 5.9],
            ['Player4', 6.2, 4.5, 4.2, 7.1, 4.3],
            ['Player5', 5.1, 6.7, 5.7, 3.6, 2.9]
        ]).types([String] + [BigDecimal]*5)
        .matrixName("Radar")
        .build()

    def normalizedMatrix = Normalize.minMaxNorm(matrix, 3)
    println normalizedMatrix.content()
    // Create a radar chart
    def rc = se.alipsa.matrix.xchart.RadarChart.create(normalizedMatrix)
    // Add series using variable and value columns
        .addSeries("Player")
  }
}
