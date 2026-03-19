package test.alipsa.matrix.arff

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.arff.ArffFormatProvider
import se.alipsa.matrix.arff.ArffReadOptions
import se.alipsa.matrix.arff.ArffTypeDecl
import se.alipsa.matrix.arff.ArffWriteOptions
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class ArffFormatProviderTest {

  @TempDir
  Path tempDir

  @Test
  void testOptionDescriptions() {
    assertTrue(Matrix.listReadOptions('arff').contains('fallbackMatrixName'))
    assertTrue(Matrix.listReadOptions('arff').contains('strict'))
    assertTrue(Matrix.listReadOptions('arff').contains('failOnUnknownAttributeType'))
    assertTrue(Matrix.listReadOptions('arff').contains('failOnRowLengthMismatch'))
    assertTrue(Matrix.listWriteOptions('arff').contains('nominalMappings'))
    assertTrue(Matrix.listWriteOptions('arff').contains('inferNominals'))
    assertTrue(Matrix.listWriteOptions('arff').contains('attributeTypesByColumn'))
    assertTrue(ArffReadOptions.describe().contains('fallbackMatrixName'))
    assertTrue(ArffReadOptions.describe().contains('strict'))
    assertTrue(ArffReadOptions.describe().contains('failOnUnknownAttributeType'))
    assertTrue(ArffWriteOptions.describe().contains('nominalMappings'))
    assertTrue(ArffWriteOptions.describe().contains('dateFormatsByColumn'))
  }

  @Test
  void testSpiReadWithFallbackName() {
    File file = tempDir.resolve('fallback.arff').toFile()
    file.text = '''@ATTRIBUTE value NUMERIC
@DATA
1
2
'''

    Matrix matrix = Matrix.read([fallbackMatrixName: 'fallback'], file)
    assertEquals('fallback', matrix.matrixName)
    assertEquals([new BigDecimal('1'), new BigDecimal('2')], matrix.value)
  }

  @Test
  void testSpiReadPrefersRelationNameOverFallbackName() {
    File file = tempDir.resolve('relation-name.arff').toFile()
    file.text = '''@RELATION actualName
@ATTRIBUTE value NUMERIC
@DATA
1
'''

    Matrix matrix = Matrix.read([fallbackMatrixName: 'fallback'], file)

    assertEquals('actualName', matrix.matrixName)
  }

  @Test
  void testSpiWriteAndReadRoundTrip() {
    Matrix source = Matrix.builder('flowers')
        .columns(
            id: [1, 2, 3],
            species: ['setosa', 'versicolor', 'setosa']
        )
        .types([Integer, String])
        .build()

    File file = tempDir.resolve('flowers.arff').toFile()
    source.write([nominalMappings: [species: ['setosa', 'versicolor']]], file)
    String content = file.getText('UTF-8')
    assertTrue(content.contains('{setosa,versicolor}'))

    Matrix matrix = Matrix.read(file)
    assertEquals(source.columnNames(), matrix.columnNames())
    assertEquals(source.rowCount(), matrix.rowCount())
    assertEquals('setosa', matrix[0, 'species'])
  }

  @Test
  void testProviderMetadata() {
    def provider = new ArffFormatProvider()
    assertEquals(['arff'] as Set, provider.supportedExtensions())
    assertEquals('ARFF', provider.formatName())
  }

  @Test
  void testSpiWriteOptionsControlSchema() {
    Matrix source = Matrix.builder('events')
        .columns(
            category: ['A', 'B', 'A'],
            value: [1, 2, 3]
        )
        .types([String, Integer])
        .build()

    File file = tempDir.resolve('events.arff').toFile()
    source.write([
        inferNominals        : false,
        attributeTypesByColumn: [category: 'STRING']
    ], file)

    String content = file.getText('UTF-8')
    assertTrue(content.contains('@ATTRIBUTE category STRING'))
  }

  @Test
  void testReadOptionsIgnoreNullFallbackMatrixName() {
    ArffReadOptions options = ArffReadOptions.fromMap([fallbackMatrixName: null])

    assertEquals(null, options.fallbackMatrixName)
  }

  @Test
  void testReadOptionsRoundTripFromMapToMap() {
    ArffReadOptions options = ArffReadOptions.fromMap([
        fallbackMatrixName        : 'fallback',
        strict                    : true,
        failOnUnknownAttributeType: false,
        failOnRowLengthMismatch   : true
    ])

    Map<String, ?> roundTrip = options.toMap()

    assertEquals('fallback', roundTrip.fallbackMatrixName)
    assertEquals(true, roundTrip.strict)
    assertEquals(false, roundTrip.failOnUnknownAttributeType)
    assertEquals(true, roundTrip.failOnRowLengthMismatch)
  }

  @Test
  void testSpiReadStrictUnknownAttributeType() {
    File file = tempDir.resolve('strict-unknown-type.arff').toFile()
    file.text = '''@RELATION strict_unknown
@ATTRIBUTE note CUSTOMTYPE
@DATA
value
'''

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Matrix.read([strict: true], file)
    }

    assertTrue(exception.message.contains("Unknown @ATTRIBUTE type 'CUSTOMTYPE'"))
    assertTrue(exception.message.contains('line 2'))
  }

  @Test
  void testSpiReadStrictRowLengthMismatch() {
    File file = tempDir.resolve('strict-row-length.arff').toFile()
    file.text = '''@RELATION strict_rows
@ATTRIBUTE first STRING
@ATTRIBUTE second NUMERIC
@DATA
'value'
'''

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Matrix.read([strict: true], file)
    }

    assertTrue(exception.message.contains('Row length mismatch'))
    assertTrue(exception.message.contains('line 5'))
  }

  @Test
  void testDirectTypedReadMatchesSpiRead() {
    File file = tempDir.resolve('typed-direct-read.arff').toFile()
    file.text = '''@ATTRIBUTE value NUMERIC
@DATA
1
2
'''

    Matrix direct = MatrixArffReader.read(file, new ArffReadOptions().fallbackMatrixName('typedDirect'))
    Matrix viaSpi = Matrix.read([fallbackMatrixName: 'typedDirect'], file)

    assertEquals(direct.matrixName, viaSpi.matrixName)
    assertEquals(direct.columnNames(), viaSpi.columnNames())
    assertEquals(direct.value, viaSpi.value)
  }

  @Test
  void testDirectTypedWriteMatchesSpiWrite() {
    Matrix source = Matrix.builder('typed-write')
        .columns(
            category: ['A', 'B', 'A'],
            value: [1, 2, 3]
        )
        .types([String, Integer])
        .build()

    File directFile = tempDir.resolve('typed-direct-write.arff').toFile()
    File spiFile = tempDir.resolve('typed-spi-write.arff').toFile()

    ArffWriteOptions writeOptions = new ArffWriteOptions()
        .inferNominals(false)
        .attributeTypesByColumn([category: ArffTypeDecl.STRING])

    MatrixArffWriter.write(source, directFile, writeOptions)
    source.write([
        inferNominals        : false,
        attributeTypesByColumn: [category: 'STRING']
    ], spiFile)

    assertEquals(directFile.getText('UTF-8'), spiFile.getText('UTF-8'))
  }

  @Test
  void testWriteOptionsRoundTripFromMapToMap() {
    ArffWriteOptions options = ArffWriteOptions.fromMap([
        nominalMappings      : [severity: ['high', 'medium', 'low']],
        inferNominals        : false,
        nominalThreshold     : 12,
        nominalColumns       : ['severity'],
        stringColumns        : ['notes'],
        attributeTypesByColumn: [createdAt: 'DATE', notes: 'STRING'],
        dateFormat           : 'yyyy-MM-dd',
        dateFormatsByColumn  : [createdAt: 'yyyy/MM/dd HH:mm']
    ])

    Map<String, ?> roundTrip = options.toMap()

    assertEquals([severity: ['high', 'medium', 'low']], roundTrip.nominalMappings)
    assertEquals(false, roundTrip.inferNominals)
    assertEquals(12, roundTrip.nominalThreshold)
    assertEquals(['severity'] as Set, roundTrip.nominalColumns as Set)
    assertEquals(['notes'] as Set, roundTrip.stringColumns as Set)
    assertEquals('DATE', String.valueOf((roundTrip.attributeTypesByColumn as Map).createdAt))
    assertEquals('STRING', String.valueOf((roundTrip.attributeTypesByColumn as Map).notes))
    assertEquals('yyyy-MM-dd', roundTrip.dateFormat)
    assertEquals([createdAt: 'yyyy/MM/dd HH:mm'], roundTrip.dateFormatsByColumn)
  }
}
