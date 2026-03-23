import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvFormatProvider
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvWriteOptions

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class CsvFormatProviderTest {

  @Test
  void testReadCsvViaSpi() {
    URL url = getClass().getResource('/basic.csv')
    File file = new File(url.toURI())

    Matrix matrix = Matrix.read(file)
    assertEquals(4, matrix.rowCount(), 'Should have 4 rows')
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), 'Column names should match')
    assertEquals(['1', 'Per', '2023-04-30', '234.12'], matrix.row(0), 'First row')
  }

  @Test
  void testReadCsvWithOptions() {
    URL url = getClass().getResource('/basic.csv')
    File file = new File(url.toURI())

    Matrix matrix = Matrix.read([delimiter: ','], file)
    assertEquals(4, matrix.rowCount(), 'Should have 4 rows')
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), 'Column names should match')
  }

  @Test
  void testReadCsvWithCharsetOption(@TempDir File tempDir) {
    File file = new File(tempDir, 'latin1.csv')
    file.write('id,name\n1,Åsa\n', 'ISO-8859-1')

    Matrix matrix = Matrix.read([charset: 'ISO-8859-1'], file)

    assertEquals(['id', 'name'], matrix.columnNames(), 'Column names should match')
    assertEquals(['1', 'Åsa'], matrix.row(0), 'Row should be decoded with the requested charset')
  }

  @Test
  void testReadCsvWithGStringCharsetOption(@TempDir File tempDir) {
    File file = new File(tempDir, 'latin1.csv')
    file.write('id,name\n1,Åsa\n', 'ISO-8859-1')
    def charsetName = "ISO-${8859}-1"

    Matrix matrix = Matrix.read([charset: charsetName], file)

    assertEquals(['1', 'Åsa'], matrix.row(0), 'Row should be decoded with a GString charset value')
  }

  @Test
  void testWriteThenReadRoundTrip(@TempDir File tempDir) {
    Matrix original = Matrix.builder()
        .matrixName('test')
        .columnNames(['x', 'y', 'z'])
        .rows([
            ['a', 'b', 'c'],
            ['d', 'e', 'f'],
            ['g', 'h', 'i'],
        ])
        .types([String, String, String])
        .build()

    File tempFile = new File(tempDir, 'roundtrip.csv')
    original.write(tempFile)

    Matrix reloaded = Matrix.read(tempFile)
    assertEquals(3, reloaded.rowCount(), 'Should have 3 rows after round-trip')
    assertEquals(['x', 'y', 'z'], reloaded.columnNames(), 'Column names should survive round-trip')
    assertEquals(['a', 'b', 'c'], reloaded.row(0), 'First row should survive round-trip')
    assertEquals(['g', 'h', 'i'], reloaded.row(2), 'Last row should survive round-trip')
  }

  @Test
  void testReadAndWriteHandleNullOptionMaps(@TempDir File tempDir) {
    URL url = getClass().getResource('/basic.csv')
    File source = new File(url.toURI())

    Matrix matrix = Matrix.read((Map) null, source)
    assertEquals(4, matrix.rowCount(), 'Null read options should be treated as empty')

    File target = new File(tempDir, 'null-options.csv')
    matrix.write((Map) null, target)
    assertTrue(target.isFile(), 'Null write options should still produce a file')
  }

  @Test
  void testWriteCsvWithCharsetOption(@TempDir File tempDir) {
    Matrix original = Matrix.builder()
        .matrixName('names')
        .columnNames(['name'])
        .rows([
            ['Åsa'],
            ['Élodie'],
        ])
        .types([String])
        .build()

    File tempFile = new File(tempDir, 'names.csv')
    original.write([charset: 'ISO-8859-1'], tempFile)

    String content = Files.readString(tempFile.toPath(), StandardCharsets.ISO_8859_1)
    assertTrue(content.contains('Åsa'), 'Written content should use the requested charset')
    assertTrue(content.contains('Élodie'), 'Written content should preserve encoded characters')

    Matrix reloaded = Matrix.read([charset: 'ISO-8859-1'], tempFile)
    assertEquals(['Åsa'], reloaded.row(0), 'First row should round-trip with explicit charset')
    assertEquals(['Élodie'], reloaded.row(1), 'Second row should round-trip with explicit charset')
  }

  @Test
  void testProviderHandlesNullOptionMaps(@TempDir File tempDir) {
    CsvFormatProvider provider = new CsvFormatProvider()
    URL url = getClass().getResource('/basic.csv')
    File source = new File(url.toURI())

    Matrix matrix = provider.read(source, null)
    assertEquals(4, matrix.rowCount(), 'Provider should treat null read options as empty')

    File target = new File(tempDir, 'provider-null-options.csv')
    provider.write(matrix, target, null)
    assertTrue(target.isFile(), 'Provider should treat null write options as empty')
  }

  @Test
  void testReadOptionsDescribe() {
    String description = CsvReadOptions.describe()
    assertNotNull(description, 'Description should not be null')
    assertFalse(description.isEmpty(), 'Description should not be empty')
    assertTrue(description.contains('delimiter'), 'Description should mention delimiter')
  }

  @Test
  void testWriteOptionsDescribe() {
    String description = CsvWriteOptions.describe()
    assertNotNull(description, 'Description should not be null')
    assertFalse(description.isEmpty(), 'Description should not be empty')
    assertTrue(description.contains('delimiter'), 'Description should mention delimiter')
  }

  @Test
  void testProviderMetadata() {
    CsvFormatProvider provider = new CsvFormatProvider()
    assertEquals('CSV (Apache Commons CSV)', provider.formatName())
    assertTrue(provider.canRead())
    assertTrue(provider.canWrite())
    assertEquals(['csv', 'tsv', 'tab'] as Set, provider.supportedExtensions())
  }
}
