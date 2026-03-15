package spreadsheet

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetFormatProvider
import se.alipsa.matrix.spreadsheet.SpreadsheetReadOptions
import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.SpreadsheetWriteOptions

import java.lang.reflect.Method
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class SpreadsheetFormatProviderTest {

  @TempDir
  Path tempDir

  @Test
  void testOptionDescriptions() {
    String readOptions = Matrix.listReadOptions('xlsx')
    assertTrue(readOptions.contains('sheet'))
    assertTrue(readOptions.contains('sheetName'))
    assertTrue(readOptions.contains('startColumn'))
    assertTrue(readOptions.contains('endColumn'))
    assertTrue(!readOptions.contains('startColumnName'))
    assertTrue(Matrix.listWriteOptions('xlsx').contains('startPosition'))
    assertTrue(SpreadsheetReadOptions.describe().contains('endRow'))
    assertTrue(SpreadsheetWriteOptions.describe().contains('sheetName'))
  }

  @Test
  void testSpiReadAutoDetectsBounds() {
    URL resource = getClass().getResource('/Book1.xlsx')
    Matrix matrix = Matrix.read(new File(resource.toURI()))

    assertEquals(['id', 'foo', 'bar', 'baz'], matrix.columnNames())
    assertEquals(11, matrix.rowCount())
    assertEquals(17.4, matrix['baz'][matrix.rowCount() - 1])
  }

  @Test
  void testSpiWriteAndReadWithOffset() {
    Matrix source = Matrix.builder('sales')
        .columns(
            id: [1, 2],
            name: ['A', 'B']
        )
        .types([Integer, String])
        .build()

    File file = tempDir.resolve('sales.xlsx').toFile()
    source.write([sheetName: 'Data', startPosition: 'B3'], file)

    Matrix matrix = Matrix.read([sheetName: 'Data', startRow: 3, endRow: 5, startColumn: 'B', endColumn: 'C'], file)
    assertEquals(source.columnNames(), matrix.columnNames())
    assertEquals(source.rowCount(), matrix.rowCount())
    assertEquals('B', matrix[1, 'name'])
  }

  @Test
  void testProviderMetadata() {
    def provider = new SpreadsheetFormatProvider()
    assertEquals(['xlsx', 'ods'] as Set, provider.supportedExtensions())
    assertEquals('Spreadsheet', provider.formatName())
  }

  @Test
  void testResolveLastRowKeepsDetectedBoundWhenStartRowIsPastData() {
    SpreadsheetReadOptions options = new SpreadsheetReadOptions().startRow(20)

    int resolved = invokeResolveLastRow(new StubSpreadsheetReader(12, 4), options)

    assertEquals(12, resolved)
  }

  @Test
  void testResolveLastColumnKeepsDetectedBoundWhenStartColumnIsPastData() {
    SpreadsheetReadOptions options = new SpreadsheetReadOptions().startColumn('F')

    int resolved = invokeResolveLastColumn(new StubSpreadsheetReader(12, 4), options)

    assertEquals(4, resolved)
  }

  private static int invokeResolveLastRow(SpreadsheetReader reader, SpreadsheetReadOptions options) {
    Method method = SpreadsheetFormatProvider.getDeclaredMethod('resolveLastRow', SpreadsheetReader, SpreadsheetReadOptions)
    method.accessible = true
    method.invoke(null, reader, options) as int
  }

  private static int invokeResolveLastColumn(SpreadsheetReader reader, SpreadsheetReadOptions options) {
    Method method = SpreadsheetFormatProvider.getDeclaredMethod('resolveLastColumn', SpreadsheetReader, SpreadsheetReadOptions)
    method.accessible = true
    method.invoke(null, reader, options) as int
  }

  private static class StubSpreadsheetReader implements SpreadsheetReader {

    private final int lastRow
    private final int lastColumn

    StubSpreadsheetReader(int lastRow, int lastColumn) {
      this.lastRow = lastRow
      this.lastColumn = lastColumn
    }

    @Override
    int findRowNum(int sheetNumber, int colNumber, String content) {
      throw new UnsupportedOperationException('not used')
    }

    @Override
    int findRowNum(int sheetNumber, String colName, String content) {
      throw new UnsupportedOperationException('not used')
    }

    @Override
    int findRowNum(String sheetName, String colName, String content) {
      throw new UnsupportedOperationException('not used')
    }

    @Override
    int findRowNum(String sheetName, int colNumber, String content) {
      throw new UnsupportedOperationException('not used')
    }

    @Override
    int findColNum(int sheetNumber, int rowNumber, String content) {
      throw new UnsupportedOperationException('not used')
    }

    @Override
    int findColNum(String sheetName, int rowNumber, String content) {
      throw new UnsupportedOperationException('not used')
    }

    @Override
    int findLastRow(int sheetNum) {
      lastRow
    }

    @Override
    int findLastRow(String sheetName) {
      lastRow
    }

    @Override
    int findLastCol(int sheetNum) {
      lastColumn
    }

    @Override
    int findLastCol(String sheetName) {
      lastColumn
    }

    @Override
    List<String> getSheetNames() {
      ['Sheet1']
    }

    @Override
    void close() {
    }
  }
}
