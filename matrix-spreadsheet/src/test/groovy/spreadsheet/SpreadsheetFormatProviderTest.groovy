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
import static org.junit.jupiter.api.Assertions.assertThrows
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

  @Test
  void testSheetNumberMustBeOneBased() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      new SpreadsheetReadOptions().sheetNumber(0)
    }

    assertEquals('sheetNumber must be >= 1', exception.message)
  }

  @Test
  void testStartRowMustBeOneBased() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      new SpreadsheetReadOptions().startRow(0)
    }

    assertEquals('startRow must be >= 1', exception.message)
  }

  @Test
  void testNumericStartColumnMustBeOneBased() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      new SpreadsheetReadOptions().startColumn(0)
    }

    assertEquals('startColumn must be >= 1', exception.message)
  }

  @Test
  void testSheetNameNullDoesNotClearSheetNumber() {
    SpreadsheetReadOptions options = new SpreadsheetReadOptions().sheetNumber(2)

    options.sheetName(null)

    assertEquals(2, options.sheetNumber)
    assertEquals(null, options.sheetName)
  }

  @Test
  void testBlankSheetNameIsRejected() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      new SpreadsheetReadOptions().sheetName('   ')
    }

    assertEquals('sheetName must not be blank', exception.message)
  }

  @Test
  void testFromMapIgnoresNullSheetAndColumnSelectors() {
    SpreadsheetReadOptions options = SpreadsheetReadOptions.fromMap([sheetName: null, startColumn: null, endColumn: null])

    assertEquals(1, options.sheetNumber)
    assertEquals(null, options.sheetName)
    assertEquals(1, options.startColumnNumber)
    assertEquals(null, options.startColumnName)
    assertEquals(null, options.endColumnNumber)
    assertEquals(null, options.endColumnName)
  }

  @Test
  void testBlankColumnSelectorsAreRejected() {
    IllegalArgumentException startColumnException = assertThrows(IllegalArgumentException) {
      new SpreadsheetReadOptions().startColumn(' ')
    }
    IllegalArgumentException endColumnException = assertThrows(IllegalArgumentException) {
      new SpreadsheetReadOptions().endColumn('\t')
    }

    assertEquals('startColumn must not be blank', startColumnException.message)
    assertEquals('endColumn must not be blank', endColumnException.message)
  }

  @Test
  void testWriteOptionsIgnoreNullValues() {
    SpreadsheetWriteOptions options = SpreadsheetWriteOptions.fromMap([sheetName: null, startPosition: null])

    assertEquals(null, options.sheetName)
    assertEquals('A1', options.startPosition)
  }

  @Test
  void testBlankStartPositionIsRejected() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      new SpreadsheetWriteOptions().startPosition(' ')
    }

    assertEquals('startPosition must not be blank', exception.message)
  }

  @Test
  void testBlankWriteSheetNameIsRejected() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      new SpreadsheetWriteOptions().sheetName(' ')
    }

    assertEquals('sheetName must not be blank', exception.message)
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
