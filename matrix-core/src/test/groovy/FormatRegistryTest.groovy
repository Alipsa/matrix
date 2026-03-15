import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.FormatRegistry
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*

class FormatRegistryTest {

  @Test
  void testExtractExtension() {
    assertEquals('csv', FormatRegistry.extractExtension('data.csv'))
    assertEquals('json', FormatRegistry.extractExtension('my.data.json'))
    assertEquals('parquet', FormatRegistry.extractExtension('/path/to/file.parquet'))
    assertEquals('csv', FormatRegistry.extractExtension('/releases/v1.2/file.csv'))
    assertEquals('', FormatRegistry.extractExtension('/releases/v1.2/file'))
    assertEquals('json', FormatRegistry.extractExtension('C:\\releases\\v1.2\\file.json'))
    assertEquals('', FormatRegistry.extractExtension('noextension'))
    assertEquals('', FormatRegistry.extractExtension(''))
    assertEquals('', FormatRegistry.extractExtension(null))
    assertEquals('', FormatRegistry.extractExtension('trailingdot.'))
  }

  @Test
  void testRegistryReturnsNullForUnknown() {
    assertNull(FormatRegistry.instance.getProvider('xyz123unknown'))
  }

  @Test
  void testOptionDescriptorDescribeTable() {
    def descriptors = [
        new OptionDescriptor('delimiter', Character, ',', 'The character used to separate values', false),
        new OptionDescriptor('quote', Character, '"', 'The quoting character', false),
        new OptionDescriptor('firstRowAsHeader', Boolean, true, 'Whether the first row contains column names', false),
        new OptionDescriptor('header', List, null, 'Column header names', true),
    ]
    String table = OptionDescriptor.describe(descriptors)
    assertNotNull(table)
    assertTrue(table.contains('delimiter'))
    assertTrue(table.contains('Character'))
    assertTrue(table.contains('quote'))
    assertTrue(table.contains('firstRowAsHeader'))
    assertTrue(table.contains('true'))
    assertTrue(table.contains('header'))
    assertTrue(table.contains('yes'))  // required column
    assertTrue(table.contains('no'))   // not required
  }

  @Test
  void testOptionDescriptorDescribeEmpty() {
    assertEquals('No options available.', OptionDescriptor.describe([]))
    assertEquals('No options available.', OptionDescriptor.describe(null))
  }

  @Test
  void testDescribeWithNoProviders() {
    // At least verify describe() does not throw when no providers are loaded
    String desc = FormatRegistry.instance.describe()
    assertNotNull(desc)
  }

  @Test
  void testListReadOptionsUnknown() {
    String result = FormatRegistry.instance.listReadOptions('xyz123unknown')
    assertTrue(result.contains('No provider found'))
  }

  @Test
  void testListWriteOptionsUnknown() {
    String result = FormatRegistry.instance.listWriteOptions('xyz123unknown')
    assertTrue(result.contains('No provider found'))
  }

  @Test
  void testMatrixReadRejectsNullFile() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Matrix.read([:], (File) null)
    }
    assertEquals('file cannot be null', exception.message)
  }

  @Test
  void testMatrixWriteRejectsNullFile() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Matrix.builder('test').build().write([:], (File) null)
    }
    assertEquals('file cannot be null', exception.message)
  }

  @Test
  void testNormalizeOptionKeys() {
    Map<String, Object> normalized = OptionMaps.normalizeKeys([Charset: 'UTF-8', StartRow: 2])
    assertEquals([charset: 'UTF-8', startrow: 2], normalized)
    assertEquals([:], OptionMaps.normalizeKeys(null))
  }

  @Test
  void testAbstractFormatProviderDefaults() {
    RecordingProvider provider = new RecordingProvider()
    File file = Files.createTempFile('format-provider', '.txt').toFile()
    file.text = 'hello'

    Matrix fromPath = provider.read(file.toPath(), [:])
    assertEquals('file', fromPath.matrixName)
    assertEquals(file.absolutePath, provider.lastReadFile)

    Matrix fromUrl = provider.read(file.toURI().toURL(), [:])
    assertEquals('stream', fromUrl.matrixName)
    assertEquals('hello', provider.lastInputStreamText)

    Path target = Files.createTempFile('format-provider-write', '.txt')
    provider.write(Matrix.builder('written').build(), target, [:])
    assertEquals(target.toFile().absolutePath, provider.lastWrittenFile)

    assertEquals([], provider.readOptionDescriptors())
    assertEquals([], provider.writeOptionDescriptors())
  }

  @Test
  void testAbstractFormatProviderUnsupportedInputStreamRead() {
    UnsupportedReadProvider provider = new UnsupportedReadProvider()
    UnsupportedOperationException exception = assertThrows(UnsupportedOperationException) {
      provider.read(new ByteArrayInputStream('abc'.bytes), [:])
    }
    assertTrue(exception.message.contains('does not support reading from InputStream'))
  }

  private static class RecordingProvider extends AbstractFormatProvider {

    String lastReadFile
    String lastInputStreamText
    String lastWrittenFile

    @Override
    Set<String> supportedExtensions() {
      ['test'] as Set
    }

    @Override
    String formatName() {
      'Recording'
    }

    @Override
    boolean canRead() {
      true
    }

    @Override
    boolean canWrite() {
      true
    }

    @Override
    Matrix read(File file, Map<String, ?> options) {
      lastReadFile = file.absolutePath
      Matrix.builder('file').build()
    }

    @Override
    Matrix read(InputStream is, Map<String, ?> options) {
      lastInputStreamText = is.getText('UTF-8')
      Matrix.builder('stream').build()
    }

    @Override
    void write(Matrix matrix, File file, Map<String, ?> options) {
      lastWrittenFile = file.absolutePath
    }
  }

  private static class UnsupportedReadProvider extends AbstractFormatProvider {

    @Override
    Set<String> supportedExtensions() {
      ['test'] as Set
    }

    @Override
    String formatName() {
      'Unsupported'
    }

    @Override
    boolean canRead() {
      true
    }

    @Override
    boolean canWrite() {
      false
    }

    @Override
    Matrix read(File file, Map<String, ?> options) {
      Matrix.builder('unsupported').build()
    }

    @Override
    void write(Matrix matrix, File file, Map<String, ?> options) {
      throw new UnsupportedOperationException('not used')
    }
  }
}
