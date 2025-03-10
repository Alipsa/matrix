package test.alipsa.matrix.xchart

import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.OHLCChart
import org.knowm.xchart.OHLCChartBuilder
import org.knowm.xchart.OHLCSeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvImporter
import se.alipsa.matrix.xchart.OhlcChart

import java.awt.Color
import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertTrue

class OhlcChartTest {

  @Test
  void testXChartOhlcChart() {
    def url = this.getClass().getResource('/gspc.csv')
    CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
    Matrix gspc = CsvImporter.importCsv(url, format)
    .convert([
        Date: Date,
        Open: Number,
        High: Number,
        Low: Number,
        Close: Number,
        Volume: Number,
        Adjusted: Number,
    ])
    OHLCChart chart = new OHLCChartBuilder().width(800).height(600).title("GSPC Oct 2020").build()
    chart.addSeries("GSPC", gspc.Date, gspc.Open, gspc.High, gspc.Low, gspc.Close)
    def file = new File("build/testXChartOhlcChart.png")
    BitmapEncoder.saveBitmap(chart, file.absolutePath, BitmapEncoder.BitmapFormat.PNG)
    assertTrue(file.exists())

    def ohlcChart = OhlcChart.create(gspc)
     .addSeries("GSPC", gspc.Date, gspc.Open, gspc.High, gspc.Low, gspc.Close)
    def file2 = new File("build/testXChartOhlcChart2.png")
    ohlcChart.exportPng(file2)
    assertTrue(file2.exists())
  }
}
