import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

import se.alipsa.matrix.core.Matrix

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Reproduce historical fixtures with the original writer isolated from current classes. */
@EnabledIfSystemProperty(named = 'parquet.captureLegacy', matches = 'true')
class ParquetLegacyCaptureTest {
  private static final String REVISION = 'b2095159664b34ce426685c387adf0bccdb33e01'

  @Test
  void captureOriginalWriter() {
    def configuration = new CompilerConfiguration()
    configuration.addCompilationCustomizers(new ASTTransformationCustomizer(CompileStatic))
    new GroovyClassLoader(getClass().classLoader, configuration).withCloseable { loader ->
      List<String> sources = ['ParquetValueWriter', 'MatrixParquetWriter'].collect { name ->
        String path = "matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/${name}.groovy"
        def process = new ProcessBuilder('git', 'show', "$REVISION:$path").start()
        String source = process.inputStream.getText('UTF-8')
        assertEquals(0, process.waitFor())
        source
      }
      Set<String> imports = []
      List<String> bodies = sources.collect { source ->
        source.readLines().findAll { line ->
          if (line.startsWith('import ')) { imports.add(line); return false }
          !line.startsWith('package ')
        }.join('\n')
      }
      loader.parseClass('package se.alipsa.matrix.parquet\n' + imports.join('\n') + '\n' + bodies.join('\n'), 'LegacyWriter.groovy')
      Class writer = loader.loadClass('se.alipsa.matrix.parquet.MatrixParquetWriter')
      def write = writer.getMethod('write', Matrix, File, boolean)
      File root = new File('src/test/resources/optimized')
      def dates = Matrix.builder('legacyDates').columns(value: [new Date(1000L), new Timestamp(2000L)]).types(Date).build()
      write.invoke(null, dates, new File(root, 'matrix-0.7-date-units.parquet'), true)
      def metadata = Matrix.builder('legacyMetadata').columns(amount: [1.25G, -2.50G],
          local: [LocalDateTime.of(2024, 1, 1, 0, 0), null]).types(BigDecimal, LocalDateTime).build()
      writer.getMethod('write', Matrix, File, ZoneId).invoke(null, metadata, new File(root, 'matrix-0.7-metadata.parquet'), ZoneOffset.UTC)
      def failure = assertThrows(java.lang.reflect.InvocationTargetException) {
        write.invoke(null, Matrix.builder().columns(value: [[0, -1] as byte[]]).types(byte[]).build(),
            new File('build/legacy-invalid-bytes.parquet'), true)
      }
      assertEquals('org.apache.parquet.schema.InvalidSchemaException', failure.cause.getClass().name)
    }
  }
}
