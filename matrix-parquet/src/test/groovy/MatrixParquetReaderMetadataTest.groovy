import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path as HadoopPath
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Types
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.nio.file.Path

class MatrixParquetReaderMetadataTest {

  @TempDir
  Path tempDir

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
    String controlColumn = "control\u0001column"
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
