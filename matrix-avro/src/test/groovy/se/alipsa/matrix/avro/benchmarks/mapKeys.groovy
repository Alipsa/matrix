import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

// Run with groovy.ui.GroovyMain and the module's test runtime classpath.
// No timing assertions: compare medians from separate baseline/updated JVMs.
int rows = 30_000
['record', 'map', 'sparse'].each { String kind ->
  def values = (0..<rows).collect { int i ->
    kind == 'map' ? (i % 2 == 0 ? [a: i, b: i] : [a: i, c: i]) : [a: i, b: i]
  }
  def matrix = Matrix.builder('Keys').columns([value: values]).types(Map).build()
  def fields = [a: AvroSchemaDecl.type(Integer), b: AvroSchemaDecl.type(Integer)]
  (0..<30).each { fields['optional' + it] = AvroSchemaDecl.type(Integer) }
  def options = kind == 'sparse'
      ? new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.record(fields)])
      : AvroWriteOptions.defaults()
  5.times { MatrixAvroWriter.writeBytes(matrix, options) }
  def times = (0..<7).collect {
    long start = System.nanoTime()
    MatrixAvroWriter.writeBytes(matrix, options)
    (System.nanoTime() - start) / 1e6
  }
  println "$kind rows=$rows warmups=5 runs=7 ms=$times median=${times.sort()[3]}"
}
