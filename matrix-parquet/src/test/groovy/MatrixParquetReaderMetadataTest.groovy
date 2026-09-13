import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path as HadoopPath
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Types
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.nio.file.Path
import java.sql.Time
import java.time.LocalTime

class MatrixParquetReaderMetadataTest {

  @TempDir
  Path tempDir

  @Test
  void testExternalTimeFieldsAreInferredWithMillisecondPrecision() {
    long millis = (10L * 60L * 60L + 30L * 60L + 45L) * 1_000L + 123L
    [LogicalTypeAnnotation.TimeUnit.MILLIS, LogicalTypeAnnotation.TimeUnit.MICROS,
     LogicalTypeAnnotation.TimeUnit.NANOS].each { LogicalTypeAnnotation.TimeUnit unit ->
      [true, false].each { boolean adjustedToUtc ->
        File file = tempDir.resolve("external_time_${unit}_${adjustedToUtc}.parquet").toFile()
        PrimitiveType.PrimitiveTypeName primitive = unit == LogicalTypeAnnotation.TimeUnit.MILLIS ?
            PrimitiveType.PrimitiveTypeName.INT32 : PrimitiveType.PrimitiveTypeName.INT64
        MessageType schema = Types.buildMessage()
            .optional(primitive)
            .as(LogicalTypeAnnotation.timeType(adjustedToUtc, unit))
            .named('time_of_day')
            .named('ExternalTime')
        long value = unit == LogicalTypeAnnotation.TimeUnit.MILLIS ? millis :
            unit == LogicalTypeAnnotation.TimeUnit.MICROS ? millis * 1_000L : millis * 1_000_000L
        def writer = ExampleParquetWriter.builder(new HadoopPath(file.toURI()))
            .withConf(new Configuration())
            .withType(schema)
            .build()
        writer.withCloseable { parquetWriter ->
          def group = new SimpleGroupFactory(schema).newGroup()
          if (primitive == PrimitiveType.PrimitiveTypeName.INT32) {
            group.append('time_of_day', (int) value)
          } else {
            group.append('time_of_day', value)
          }
          parquetWriter.write(group)
        }

        Matrix matrix = MatrixParquetReader.read(file)
        assertEquals(Time, matrix.types()[0])
        Time expected = Time.valueOf(LocalTime.of(10, 30, 45))
        expected.setTime(expected.time + 123L)
        assertEquals(expected.time, matrix.time_of_day[0].time)
        if (unit != LogicalTypeAnnotation.TimeUnit.MILLIS) {
          File rewritten = tempDir.resolve("rewritten_time_${unit}_${adjustedToUtc}.parquet").toFile()
          MatrixParquetWriter.write(matrix, rewritten)
          def footer = ParquetFileReader.readFooter(new Configuration(), new HadoopPath(rewritten.toURI()))
          assertEquals(LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MILLIS),
              footer.fileMetaData.schema.getType('time_of_day').logicalTypeAnnotation)
        }
      }
    }
  }

  @Test
  void testExternalTimeOutsideOneDayReportsTheFieldName() {
    MessageType schema = Types.buildMessage()
        .optional(PrimitiveType.PrimitiveTypeName.INT32)
        .as(LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MILLIS))
        .named('time_of_day')
        .named('InvalidTime')
    File file = tempDir.resolve('invalid_time.parquet').toFile()
    def writer = ExampleParquetWriter.builder(new HadoopPath(file.toURI()))
        .withConf(new Configuration())
        .withType(schema)
        .build()
    writer.withCloseable { parquetWriter ->
      def group = new SimpleGroupFactory(schema).newGroup()
      group.append('time_of_day', 86_400_000)
      parquetWriter.write(group)
    }

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixParquetReader.read(file)
    }
    assertTrue(exception.message.contains('time_of_day'))
  }

  @Test
  void testByteArrayReadUsesSchemaNameWhenNoMatrixNameProvided() {
    def data = Matrix.builder('schemaNamedMatrix').data(id: [1, 2]).types([Integer]).build()

    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    Matrix matrix = MatrixParquetReader.read(bytes)

    assertEquals('schemaNamedMatrix', matrix.matrixName)
  }

  @Test
  void testInputStreamReadUsesExplicitMatrixNameWhenProvided() {
    def data = Matrix.builder('schemaName').data(id: [1]).types([Integer]).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(data)

    Matrix matrix = MatrixParquetReader.read(new ByteArrayInputStream(bytes), 'explicitName')

    assertEquals('explicitName', matrix.matrixName)
  }

  @Test
  void testIndexColumnNameWithCommaRoundTrip() {
    def data = Matrix.builder('commaIndex').data(
        'country,region': ['US,West', 'SE,Stockholm'],
        quarter: ['Q1', 'Q2'],
        sales: [100, 200]
    ).types([String, String, Integer]).build()

    data.createIndex('country,region')
    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    Matrix matrix = MatrixParquetReader.read(bytes)

    assertTrue(matrix.hasIndex())
    assertEquals(['country,region'], matrix.indexedColumns())
    assertEquals(1, matrix.lookup('US,West').rowCount())
  }

  @Test
  void testLegacyCommaDelimitedIndexMetadataStillReads() {
    MessageType schema = Types.buildMessage()
        .optional(PrimitiveType.PrimitiveTypeName.BINARY).named('country')
        .optional(PrimitiveType.PrimitiveTypeName.BINARY).named('quarter')
        .optional(PrimitiveType.PrimitiveTypeName.INT32).named('sales')
        .named('LegacyIndex')

    File file = tempDir.resolve('legacy_index_metadata.parquet').toFile()
    Map<String, String> extraMeta = [
        (MatrixParquetReader.METADATA_COLUMN_TYPES): 'java.lang.String,java.lang.String,java.lang.Integer',
        (MatrixParquetReader.METADATA_INDEX_COLUMNS): 'country,quarter'
    ]

    def writer = ExampleParquetWriter.builder(new HadoopPath(file.toURI()))
        .withConf(new Configuration())
        .withType(schema)
        .withExtraMetaData(extraMeta)
        .build()
    writer.withCloseable { parquetWriter ->
      def group = new SimpleGroupFactory(schema).newGroup()
      group.append('country', 'SE')
      group.append('quarter', 'Q1')
      group.append('sales', 100)
      parquetWriter.write(group)
    }

    Matrix matrix = MatrixParquetReader.read(file)
    assertTrue(matrix.hasIndex())
    assertEquals(['country', 'quarter'], matrix.indexedColumns())
  }

  @Test
  void testLegacySingleIndexColumnStartingWithBracketStillReads() {
    MessageType schema = Types.buildMessage()
        .optional(PrimitiveType.PrimitiveTypeName.BINARY).named('[2024]')
        .optional(PrimitiveType.PrimitiveTypeName.INT32).named('sales')
        .named('LegacyBracketIndex')

    File file = tempDir.resolve('legacy_bracket_index_metadata.parquet').toFile()
    Map<String, String> extraMeta = [
        (MatrixParquetReader.METADATA_COLUMN_TYPES): 'java.lang.String,java.lang.Integer',
        (MatrixParquetReader.METADATA_INDEX_COLUMNS): '[2024]'
    ]

    def writer = ExampleParquetWriter.builder(new HadoopPath(file.toURI()))
        .withConf(new Configuration())
        .withType(schema)
        .withExtraMetaData(extraMeta)
        .build()
    writer.withCloseable { parquetWriter ->
      def group = new SimpleGroupFactory(schema).newGroup()
      group.append('[2024]', 'FY')
      group.append('sales', 100)
      parquetWriter.write(group)
    }

    Matrix matrix = MatrixParquetReader.read(file)
    assertTrue(matrix.hasIndex())
    assertEquals(['[2024]'], matrix.indexedColumns())
    assertEquals(1, matrix.lookup('FY').rowCount())
  }

  @Test
  void testJsonIndexMetadataEscapesGenericControlCharacters() {
    String controlColumn = 'control\u0001column'
    def data = Matrix.builder('controlIndex').data(
        (controlColumn): ['A'],
        sales: [100]
    ).types([String, Integer]).build()
    data.createIndex(controlColumn)

    File file = tempDir.resolve('control_index_metadata.parquet').toFile()
    MatrixParquetWriter.write(data, file)

    def footer = ParquetFileReader.readFooter(new Configuration(), new HadoopPath(file.toURI()))
    String indexMetadata = footer.fileMetaData.keyValueMetaData[MatrixParquetWriter.METADATA_INDEX_COLUMNS]

    assertTrue(indexMetadata.contains('\\u0001'))
    assertFalse(indexMetadata.contains('\u0001'))

    Matrix matrix = MatrixParquetReader.read(file)
    assertTrue(matrix.hasIndex())
    assertEquals([controlColumn], matrix.indexedColumns())
  }
}
