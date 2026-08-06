package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers XLSX round trips, sheet selection, ranges, and multi-sheet output. */
@Tag('spreadsheet')
class SpreadsheetApiIT implements ApiItSupport {

  @Test
  void xlsxAndMultiSheetRoundTrip(@TempDir Path directory) {
    Matrix data = mtcars().top(5)
    File file = directory.resolve('matrix.xlsx').toFile()
    SpreadsheetWriter.write(data, file, 'mtcars', 'A1')
    Matrix restored = SpreadsheetImporter.importSpreadsheet(
        file: file, sheet: 'mtcars', endRow: 6, endCol: 'L', firstRowAsColNames: true)
    assertEquals(data.rowCount(), restored.rowCount())
    assertEquals(data.columnNames(), restored.columnNames())
    File multi = directory.resolve('multi.xlsx').toFile()
    SpreadsheetWriter.writeSheets([data, data], multi, ['first', 'second'])
    assertTrue(multi.isFile() && multi.length() > 0)
  }
}
