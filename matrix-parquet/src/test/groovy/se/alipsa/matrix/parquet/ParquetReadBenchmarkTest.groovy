package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

import se.alipsa.matrix.core.Matrix

import java.lang.management.ManagementFactory
import java.time.LocalDate

/** Opt-in comparison against the pre-change Group loop using shared schema conversions. */
@EnabledIfSystemProperty(named = 'parquet.baseline', matches = 'true')
class ParquetReadBenchmarkTest {
  @Test
  @SuppressWarnings('ExplicitGarbageCollection') // Opt-in retained-heap benchmark, not production behavior.
  void compare() {
    int rows = Integer.parseInt(System.getProperty('parquet.benchmarkRows', '1000000'))
    ['wide', 'categorical', 'mixed'].each { workload ->
      def columns = [id: (0..<rows).toList()]
      (0..<6).each { int c ->
        columns["text$c".toString()] = (0..<rows).collect { "text-$c-${it % (workload == 'categorical' ? 12 : 1000)}".toString() }
      }
      if (workload == 'mixed') {
        columns.date = [LocalDate.of(2024, 1, 1)] * rows
        columns.nested = (0..<rows).collect { [label: "class-${it % 10}".toString(), score: new BigDecimal(it).movePointLeft(2)] }
      }
      Matrix source = Matrix.builder('benchmark').columns(columns).build()
      File file = new File("build/benchmark-${workload}.parquet")
      MatrixParquetWriter.write(source, file)
      def types = MatrixParquetReader.read(file).types()
      [false, true].each { columnar ->
        (0..<4).each { int run ->
          System.gc()
          long beforeHeap = ManagementFactory.memoryMXBean.heapMemoryUsage.used
          def bean = (com.sun.management.ThreadMXBean) ManagementFactory.threadMXBean
          long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId())
          def sampler = new LiveHeapSampler()
          long start = System.nanoTime()
          Matrix result = columnar ? MatrixParquetReader.read(file) : legacy(file, types)
          long elapsed = System.nanoTime() - start
          allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated
          sampler.finish()
          long retained = ManagementFactory.memoryMXBean.heapMemoryUsage.used - beforeHeap
          assert result.rowCount() == rows
          assert result[rows - 1, 'id'] == rows - 1
          println "BENCH workload=$workload columnar=$columnar run=$run rows=$rows bytes=${file.length()} millis=${elapsed.intdiv(1_000_000)} allocated=$allocated retained=$retained peakLiveAfterGc=${sampler.peak.get()}"
        }
      }
    }
  }

  private static Matrix legacy(File file, List<Class> types) {
    Path path = new Path(file.toURI())
    def schema = ParquetFileReader.readFooter(new Configuration(), path).fileMetaData.schema
    def names = schema.fields*.name
    ParquetReader.builder(new GroupReadSupport(), path).build().withCloseable { reader ->
      def builder = Matrix.builder('benchmark').columnNames(names).types(types)
      Group row = reader.read()
      while (row != null) {
        def values = []
        names.eachWithIndex { String name, int index -> values << MatrixParquetReader.readValue(row, name, schema.getType(name), types[index]) }
        builder.addRow(values as Object[])
        row = reader.read()
      }
      builder.build()
    }
  }
}
