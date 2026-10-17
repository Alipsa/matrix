package se.alipsa.matrix.parquet

import org.apache.parquet.io.LocalInputFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/** Opt-in full/projection/filter measurements against externally generated identical layouts. */
@EnabledIfSystemProperty(named = 'parquet.layoutBenchmark', matches = 'true')
class ParquetLayoutBenchmarkTest {
  @Test
  @SuppressWarnings('ExplicitGarbageCollection')
  void compareLayouts() {
    ['ordinary', 'indexed', 'cdc-indexed'].each { layout ->
      File file = new File("build/layout-benchmarks/${layout}.parquet")
      assert file.isFile() : 'Run src/test/python/generate_benchmarks.py first'
      ['full', 'projection', 'selective'].each { mode ->
        (0..<4).each { int run ->
          System.gc()
          def options = new ParquetReadOptions()
          if (mode != 'full') { options.columns = ['id'] }
          if (mode == 'selective') { options.filter = Predicates.ge('id', 999900L) }
          def input = new CountingInput(new LocalInputFile(file.toPath()))
          def sampler = new LiveHeapSampler()
          long start = System.nanoTime()
          def result = MatrixParquetReader.readInput(input, options, null)
          long elapsed = System.nanoTime() - start
          sampler.finish()
          assert result.rowCount() == (mode == 'selective' ? 100 : 1000000)
          assert result[result.rowCount() - 1, 'id'] == 999999L
          println "LAYOUT layout=$layout mode=$mode run=$run rows=${result.rowCount()} fileBytes=${file.length()} readBytes=${input.totalBytes} millis=${elapsed.intdiv(1000000)} peakLiveAfterGc=${sampler.peak.get()}"
        }
      }
    }
  }
}
