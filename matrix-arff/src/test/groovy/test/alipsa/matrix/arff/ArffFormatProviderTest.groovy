package test.alipsa.matrix.arff

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.arff.ArffFormatProvider
import se.alipsa.matrix.arff.ArffReadOptions
import se.alipsa.matrix.arff.ArffWriteOptions
import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class ArffFormatProviderTest {

  @TempDir
  Path tempDir

  @Test
  void testOptionDescriptions() {
    assertTrue(Matrix.listReadOptions('arff').contains('matrixName'))
    assertTrue(Matrix.listWriteOptions('arff').contains('nominalMappings'))
    assertTrue(ArffReadOptions.describe().contains('matrixName'))
    assertTrue(ArffWriteOptions.describe().contains('nominalMappings'))
  }

  @Test
  void testSpiReadWithFallbackName() {
    File file = tempDir.resolve('fallback.arff').toFile()
    file.text = '''@ATTRIBUTE value NUMERIC
@DATA
1
2
'''

    Matrix matrix = Matrix.read([matrixName: 'fallback'], file)
    assertEquals('fallback', matrix.matrixName)
    assertEquals([new BigDecimal('1'), new BigDecimal('2')], matrix.value)
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
  void testReadOptionsIgnoreNullMatrixName() {
    ArffReadOptions options = ArffReadOptions.fromMap([matrixName: null])

    assertEquals(null, options.matrixName)
  }
}
