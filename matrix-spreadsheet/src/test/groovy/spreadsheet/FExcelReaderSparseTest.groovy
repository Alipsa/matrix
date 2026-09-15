package spreadsheet

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Sheet
import org.junit.jupiter.api.Test

import se.alipsa.matrix.spreadsheet.SpreadsheetReader
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelUtil
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelValueExtractor

import java.time.LocalDate
import java.time.LocalDateTime

class FExcelReaderSparseTest {

  private static File sparse() {
    XlsxTestUtil.createXlsx { ws ->
      ws.value(0, 0, 'a'); ws.value(0, 2, 'target')
      ws.value(1, 0, 'only-a')
      ws.value(3, 0, 'x'); ws.value(3, 2, 'needle')
    }
  }

  @Test
  void sparseCellAccessAndRowLookupAreSafe() {
    File file = sparse()
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      assertEquals(4, reader.findRowNum(1, 3, 'needle'))
      assertEquals(3, reader.findColNum(1, 4, 'needle'))
      assertEquals(3, reader.findColNum('Sheet1', 4, 'needle'))
      assertEquals(-1, reader.findColNum(1, 3, 'needle'))
      assertEquals(-1, reader.findRowNum(1, 5, 'needle'))
    }
    try (ReadableWorkbook workbook = new ReadableWorkbook(file, FExcelImporter.OPTIONS)) {
      Sheet sheet = workbook.getSheet(0).get()
      assertEquals('x', FExcelUtil.getRow(sheet, 3).getCell(0).rawValue)
      assertNull(FExcelUtil.getRow(sheet, 2))
      assertNull(FExcelUtil.getRow(sheet, 99))
      FExcelValueExtractor extractor = new FExcelValueExtractor(sheet, workbook.isDate1904())
      assertNull(extractor.getString(1, 7))
      assertNull(extractor.getBigDecimal(1, 7))
      assertNull(extractor.getInteger(1, 7))
    }
  }

  @Test
  void dateAccessorsAcceptDatesAndDateTimes() {
    File file = XlsxTestUtil.createXlsx { ws ->
      ws.value(0, 0, LocalDateTime.of(2024, 1, 2, 3, 4, 5)); ws.style(0, 0).format('yyyy-MM-dd HH:mm:ss').set()
      ws.value(1, 0, LocalDate.of(2024, 1, 2)); ws.style(1, 0).format('yyyy-MM-dd').set()
    }
    try (ReadableWorkbook workbook = new ReadableWorkbook(file, FExcelImporter.OPTIONS)) {
      Sheet sheet = workbook.getSheet(0).get()
      FExcelValueExtractor extractor = new FExcelValueExtractor(sheet, workbook.isDate1904())
      assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4, 5), extractor.getLocalDateTime(FExcelUtil.getRow(sheet, 0), 0))
      assertEquals(LocalDateTime.of(2024, 1, 2, 0, 0), extractor.getLocalDateTime(FExcelUtil.getRow(sheet, 1), 0))
    }
  }

}
