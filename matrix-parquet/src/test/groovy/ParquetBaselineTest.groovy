import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

/** Opt-in baseline and legacy fixture capture, run before changing the writer. */
@EnabledIfSystemProperty(named = 'parquet.baseline', matches = 'true')
class ParquetBaselineTest {
  @Test
  void capture() {
    int rows = 1_000_000
    def columns = [id: (0..<rows).toList()]
    (0..<6).each { c -> columns["text$c".toString()] = (0..<rows).collect { "text-$c-${it % 1000}".toString() } }
    Matrix source = Matrix.builder('wide').columns(columns).types([Integer] + [String] * 6).build()
    File file = new File('build/baseline-wide.parquet')
    MatrixParquetWriter.write(source, file)
    (0..<4).each { run ->
      long start = System.nanoTime()
      def result = MatrixParquetReader.read(file)
      long millis = (System.nanoTime() - start).intdiv(1_000_000)
      assert result.rowCount() == rows
      println "baseline wide run=$run rows=$rows fileBytes=${file.length()} millis=$millis"
    }
  }
}
