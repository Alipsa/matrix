package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import org.knowm.xchart.VectorGraphicsEncoder
import org.knowm.xchart.style.AxesChartStyler
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.xchart.AreaChart
import se.alipsa.matrix.xchart.LineChart

import static org.junit.jupiter.api.Assertions.assertTrue
import static org.knowm.xchart.XYSeries.XYSeriesRenderStyle.*

class XyChartTest {

  @Test
  void testLineChart() {
    Matrix matrix = new MatrixBuilder()
        .data(
            X1: [0.0, 1.0, 2.0],
            Y1: [2.0, 1.0, 0.0],
            X2: [1.8, 1.5, 0.5],
            Y2: [0.0, 1.0, 1.5],
        ).types([Double] * 4)
        .matrixName("Lines")
        .build()

    LineChart chart = LineChart.create(matrix, 600, 500)
        // add a series with a different series name
        .addSeries('First', 'X1', 'Y1')
        // add a series by specifying the column names
        .addSeries('X2', 'Y2')
    // Write to a png file
    File file = new File("./build/testLineChart.png")
    chart.exportPng(file)
    assertTrue(file.exists())

    // Write it to an output stream
    File file2 = new File("./build/testLineChart2.png")
    try (FileOutputStream fos = new FileOutputStream(file2)) {
      // add an additional series to the chart with an optional series name
      // instead of specifying the column name, we provide the columns themselves
      chart.addSeries('Third', matrix.X1, matrix.Y2).exportPng(fos)
    }
    assertTrue(file2.exists())

    VectorGraphicsEncoder.saveVectorGraphic(chart.xchart,
        "./build/testLineChart3.pdf",
        VectorGraphicsEncoder.VectorGraphicsFormat.PDF)

    chart.exportSvg(new File("./build/testLineChart4.svg"))
  }

  @Test
  void testAreaChart() {
    Matrix matrix = new MatrixBuilder()
        .data(
            ax: [0, 3, 5, 7, 9],
            ay: [-3, 5, 9, 6, 5],
            bx: [0, 2, 4, 6, 9],
            by: [-1, 6, 4, 0, 4],
            cx: [0, 1, 3, 8, 9],
            cy: [-2, -1, 1, 0, 1]
        ).types([Double] * 6)
        .matrixName("Areas")
        .build()

    def ac = AreaChart.create(matrix, 800, 600)
    .addSeries('a', matrix.ax, matrix.ay)
    .addSeries('b', matrix.bx, matrix.by)
    .addSeries('c', 'cx', 'cy')
    ac.style.legendPosition = Styler.LegendPosition.InsideNE
    ac.style.axisTitlesVisible = false
    File file = new File("build/testAreaChart.png")
    ac.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testAreaAndLineCombo() {
    Matrix matrix = new MatrixBuilder()
        .data(
            ages: [60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76,
                    77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93,
                    94, 95, 96, 97, 98, 99, 100],
            liability: [672234, 691729, 711789, 732431, 753671, 775528, 798018, 821160,
                        844974, 869478, 907735, 887139, 865486, 843023, 819621, 795398,
                        770426, 744749, 719011, 693176, 667342, 641609, 616078, 590846,
                        565385, 540002, 514620, 489380, 465149, 441817, 419513, 398465,
                        377991, 358784, 340920, 323724, 308114, 293097, 279356, 267008,
                        254873 ],
            percentile75th: [800000, 878736, 945583, 1004209, 1083964, 1156332, 1248041,
                             1340801, 1440138, 1550005, 1647728, 1705046, 1705032, 1710672,
                             1700847, 1683418, 1686522, 1674901, 1680456, 1679164, 1668514,
                             1672860, 1673988, 1646597, 1641842, 1653758, 1636317, 1620725,
                             1589985, 1586451, 1559507, 1544234, 1529700, 1507496, 1474907,
                             1422169, 1415079, 1346929, 1311689, 1256114, 1221034],
            percentile50th: [800000, 835286, 873456, 927048, 969305, 1030749, 1101102, 1171396,
                             1246486, 1329076, 1424666, 1424173, 1421853, 1397093, 1381882,
                             1364562, 1360050, 1336885, 1340431, 1312217, 1288274, 1271615,
                             1262682, 1237287, 1211335, 1191953, 1159689, 1117412, 1078875,
                             1021020, 974933, 910189, 869154, 798476, 744934, 674501, 609237,
                             524516, 442234, 343960, 257025],
            percentile25th: [00000, 791439, 809744, 837020, 871166, 914836, 958257, 1002955,
                             1054094, 1118934, 1194071, 1185041, 1175401, 1156578, 1132121,
                             1094879, 1066202, 1054411, 1028619, 987730, 944977, 914929, 880687,
                             809330, 783318, 739751, 696201, 638242, 565197, 496959, 421280,
                             358113, 276518, 195571, 109514, 13876, 29, 0, 0, 0, 0]
        ).types([Double] * 5)
        .matrixName("Area and Lines")
        .build()

    def ac = AreaChart.create(matrix, 800, 600)
        .addSeries("ages", "liability")
        .addSeries("75th Percentile", "ages", "percentile75th", Line)
        .addSeries("50th Percentile", "ages", "percentile50th", Line)
        .addSeries("25th Percentile", "ages", "percentile25th", Line)

    ac.style.setLegendPosition(Styler.LegendPosition.InsideNW)
    ac.style.setYAxisLabelAlignment(AxesChartStyler.TextAlignment.Right)
    ac.style.setYAxisDecimalPattern('$ #,###.##')
    ac.style.setPlotMargin(0)
    ac.style.setPlotContentSize(.95)

    ac.series.ages.setMarker(SeriesMarkers.DIAMOND)

    File file = new File("build/testAreaAndLineCombo.svg")
    ac.exportSvg(file)
    assertTrue(file.exists())
  }
}