package se.alipsa.matrix.parquet

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

import se.alipsa.matrix.core.Matrix

/** Opt-in bounded shard-decoding measurements, without timing assertions. */
@EnabledIfSystemProperty(named = 'parquet.datasetBenchmark', matches = 'true')
class ParquetDatasetBenchmarkTest {
  @Test
  @SuppressWarnings('ExplicitGarbageCollection')
  void compareWorkers() {
    File directory = new File('build/dataset-benchmark')
    directory.mkdirs()
    List<File> files = (0..<4).collect { int shard ->
      File file = new File(directory, "shard-${shard}.parquet")
      int start = shard * 250000
      MatrixParquetWriter.write(Matrix.builder().columns(id: (start..<start + 250000).toList(),
          text: (0..<250000).collect { "label-${it % 1000}".toString() }).build(), file)
      file
    }
    [1, 4].each { workers ->
      (0..<4).each { run ->
        System.gc()
        def sampler = new LiveHeapSampler()
        long start = System.nanoTime()
        def result = MatrixParquetReader.builder().parallelism(workers).maxBatchRows(65536).read(files)
        long millis = (System.nanoTime() - start).intdiv(1_000_000)
        sampler.finish()
        assert result.rowCount() == 1_000_000
        assert result[0, 'id'] == 0
        assert result[999999, 'id'] == 999999
        println "DATASET workers=$workers run=$run rows=${result.rowCount()} millis=$millis peakLiveAfterGc=${sampler.peak.get()}"
      }
    }
  }
}
