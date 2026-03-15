import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonFormatProvider
import se.alipsa.matrix.json.JsonReadOptions
import se.alipsa.matrix.json.JsonWriteOptions

import java.nio.file.Path
import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

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
}
