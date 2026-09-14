package spreadsheet

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.SpreadsheetReader

class FExcelImporterSparseTest {

  @Test
  void blankHeaderCellsAndShortHeadersGetGeneratedNames() {
    File gapped = XlsxTestUtil.createXlsx { ws ->
      ws.value(0, 0, 'a'); ws.value(0, 2, 'c')
      ws.value(1, 0, 1); ws.value(1, 1, 2); ws.value(1, 2, 3)
    }
    Matrix gappedMatrix = SpreadsheetImporter.importSpreadsheet(gapped.absolutePath, 1, 1, 2, 1, 3, true)
    assertEquals(['a', 'c2', 'c'], gappedMatrix.columnNames())
    assertEquals(['1', '2', '3'], gappedMatrix.row(0).collect { it.toString() })

    File shortHeader = XlsxTestUtil.createXlsx { ws ->
      ws.value(0, 0, 'a')
      ws.value(1, 0, 1); ws.value(1, 1, 2); ws.value(1, 2, 3)
    }
    Matrix shortMatrix = SpreadsheetImporter.importSpreadsheet(shortHeader.absolutePath, 1, 1, 2, 1, 3, true)
    assertEquals(['a', 'c2', 'c3'], shortMatrix.columnNames())
  }

  @Test
  void sparseRowsUseTheirSheetNumbersAndPreserveInteriorGaps() {
    File file = XlsxTestUtil.createXlsx { ws ->
      ws.value(0, 0, 'a'); ws.value(1, 0, 1); ws.value(3, 0, 4); ws.value(4, 0, 5)
    }
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      assertEquals(5, reader.findLastRow(1))
      assertEquals(5, reader.findLastRow('Sheet1'))
    }
    Matrix matrix = SpreadsheetImporter.importSpreadsheet(file)
    assertEquals(['1', null, '4', '5'], matrix.column('a').collect { it?.toString() })
  }

  @Test
  void blankFirstRowIsRetainedWhenItIsNotTheHeader() {
    File file = XlsxTestUtil.createXlsx { ws ->
      ws.value(1, 0, 'x'); ws.value(1, 1, 'y')
    }
    Matrix matrix = SpreadsheetImporter.importSpreadsheet(file.absolutePath, 1, 1, 2, 1, 2, false)
    assertEquals(['c1', 'c2'], matrix.columnNames())
    assertEquals([[null, null], ['x', 'y']], matrix.rows())
  }

}
