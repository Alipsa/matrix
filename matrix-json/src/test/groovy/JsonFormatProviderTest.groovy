import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonFormatProvider
import se.alipsa.matrix.json.JsonReadOptions
import se.alipsa.matrix.json.JsonWriteOptions

import java.nio.file.Path
import java.time.LocalDate

class JsonFormatProviderTest {

  @TempDir
  Path tempDir

  @Test
  void testSpiReadOptionsAndWriteOptions() {
    assertTrue(Matrix.listReadOptions('json').contains('charset'))
    assertTrue(Matrix.listWriteOptions('json').contains('indent'))
    assertTrue(JsonReadOptions.describe().contains('charset'))
    assertTrue(JsonWriteOptions.describe().contains('dateFormat'))
  }

  @Test
  void testSpiReadAndWrite() {
    Matrix source = Matrix.builder('people')
        .columns(
            id: [1, 2],
            name: ['Alice', 'Bob'],
            birthday: [LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 4)]
        )
        .types([Integer, String, LocalDate])
        .build()

    File file = tempDir.resolve('people.json').toFile()
    source.write([indent: true, dateFormat: 'yyyy/MM/dd'], file)

    String json = file.getText('UTF-8')
    assertTrue(json.contains('\n'))
    assertTrue(json.contains('2024/01/02'))

    Matrix matrix = Matrix.read(file)
    assertEquals(['id', 'name', 'birthday'], matrix.columnNames())
    assertEquals(2, matrix.rowCount())
    assertEquals('2024/01/02', matrix[0, 'birthday'])
  }

  @Test
  void testProviderMetadata() {
    def provider = new JsonFormatProvider()
    assertEquals(['json'] as Set, provider.supportedExtensions())
    assertEquals('JSON', provider.formatName())
  }

  @Test
  void testWriteOptionsIgnoreNullDateFormatAndColumnFormatters() {
    JsonWriteOptions options = JsonWriteOptions.fromMap([dateFormat: null, columnFormatters: null])

    assertEquals('yyyy-MM-dd', options.dateFormat)
    assertTrue(options.columnFormatters.isEmpty())
  }

  @Test
  void testReadOptionsIgnoreNullCharset() {
    JsonReadOptions options = JsonReadOptions.fromMap([charset: null])

    assertEquals('UTF-8', options.charset.name())
  }

  @Test
  void testReadWithTypeConversion() {
    File file = tempDir.resolve('typed.json').toFile()
    file.text = '[{"id":1,"name":"Alice","birthday":"2024-01-15"},{"id":2,"name":"Bob","birthday":"2024-03-20"}]'

    Matrix matrix = Matrix.read([types: [Integer, String, LocalDate], dateTimeFormat: 'yyyy-MM-dd'], file)

    assertEquals(Integer, matrix.type('id'))
    assertEquals(String, matrix.type('name'))
    assertEquals(LocalDate, matrix.type('birthday'))
    assertEquals(LocalDate.of(2024, 1, 15), matrix[0, 'birthday'])
  }

  @Test
  void testReadWithMatrixNameOption() {
    File file = tempDir.resolve('data.json').toFile()
    file.text = '[{"x":1}]'

    Matrix matrix = Matrix.read([matrixName: 'custom_name'], file)

    assertEquals('custom_name', matrix.matrixName, "matrixName option should override file-derived name")
  }
}
