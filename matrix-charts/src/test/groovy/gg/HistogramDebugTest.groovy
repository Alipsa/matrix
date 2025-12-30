package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.stat.GgStat

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class HistogramDebugTest {
  @Test
  void testDebugHistogram() {
    def rows = (1..20).collect { [it] }
    def data = Matrix.builder()
        .columnNames('x')
        .rows(rows)
        .types(Integer)
        .build()

    println "Original data:"
    println "  Columns: ${data.columnNames()}"
    println "  Rows: ${data.rowCount()}"
    println "  Data column x: ${data['x']}"

    // Test stat_bin directly
    try {
      def aes = new Aes(x: 'x')
      println "\nCalling GgStat.bin with aes.xColName=${aes.xColName}"
      def binned = GgStat.bin(data, aes, [bins: 5])
      println "\nBinned data:"
      println "  Columns: ${binned.columnNames()}"
      println "  Rows: ${binned.rowCount()}"
    } catch (Exception e) {
      println "\nException during stat_bin:"
      e.printStackTrace()
      throw e
    }
  }
}
