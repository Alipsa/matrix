package se.alipsa.matrix.parquet
import se.alipsa.matrix.core.Matrix

/**
 * This class uses Groovy Grape to load Carpet and Hadoop at runtime and thus avoid having them
 * as compile time dependencies. However, this also means that the code is
 * significantly slower than using MatrixParquetReader and MatrixParquetWriter directly.
 * It is only here for backwards compatibility and to trouble shoot issues with MatrixParquetReader
 * and MatrixParquetWriter.
 *
 * @deprecated Use MatrixParquetReader and MatrixParquetWriter instead
 */
@Deprecated
class MatrixParquetIO {

  /**
   * Carpet and hadoop must be loaded from grape so they are part of the same classloader
   * and can thus understand the dynamically created record.
   * If Carpet or hadoop is part of the bootstrap classpath then the following error will occur:
   * java.lang.ClassNotFoundException: MtcarsRow
   * at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641)
   * at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
   *
   * @param data the Matrix to write
   * @param outputStream the stream to write to, should be closed by the user of the method
   */
  static void write(Matrix data, OutputStream outputStream, Map config = [:]) {
    String script = '''
    @Grab('com.jerolba:carpet-record:0.2.1')
    @Grab('org.apache.hadoop:hadoop-client:3.4.1')
    import com.jerolba.carpet.*
    import se.alipsa.matrix.parquet.RecordBuilder
    import org.apache.parquet.hadoop.ParquetFileWriter.Mode
    
    def rb = new RecordBuilder(cl)
    def records = rb.createRecordRows(data, config)   
    
    try (CarpetWriter<? extends Record> writer = new CarpetWriter.Builder(outputStream, records.recordClass as Class<Record>)
        .withWriteMode(Mode.OVERWRITE)
        .withDefaultDecimal(records.precision as int, records.scale as int).build()) {
      writer.write(records.rows)
    }
    '''

    GroovyClassLoader cl = new GroovyClassLoader()
    def vars = new Binding()
    vars.setProperty('outputStream', outputStream)
    vars.setProperty('config', config)
    vars.setProperty('data', data)
    vars.setProperty('cl', cl)
    def shell = new GroovyShell(cl, vars)
    shell.evaluate(script)
  }

  static void write(Matrix data, File file, Map config = [:]) {
    try (OutputStream outputStream = new FileOutputStream(file)) {
      write(data, outputStream, config)
    }
  }

  static Matrix read(File file, String... matrixName) {
    String script = '''
      @Grab('com.jerolba:carpet-record:0.2.1')
      @Grab('org.apache.hadoop:hadoop-client:3.4.1')
      import com.jerolba.carpet.*
      List<Map> data = new CarpetReader<>(file, Map.class).toList()
      return data
    '''
    def vars = new Binding()
    vars.setProperty('file', file)
    def shell = new GroovyShell(vars)
    List<Map> data = shell.evaluate(script) as List<Map>
    String name = matrixName.length > 0 ? matrixName[0] : file.getName()
    Matrix.builder(name).mapList(data).build()
  }

}
