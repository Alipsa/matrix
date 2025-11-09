package spreadsheet

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.spreadsheet.SpreadsheetReader

class SpreadsheetReaderTest {

  private static final List<String> BOOK1_FILES = ["Book1.xlsx", "Book1.ods"]

  @Test
  void testFindBySheetNumber() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(12, reader.findLastRow(1), "$spreadsheetName: Last row by number")
      assertEquals(4, reader.findLastCol(1), "$spreadsheetName: Last column by number")
      assertEquals(5, reader.findRowNum(1, 'B', 'gång'),
          "$spreadsheetName: find row by number and column name")
      assertEquals(4, reader.findColNum(1, 8, '20.9'),
          "$spreadsheetName: find column by number")
    }
  }

  @Test
  void testFindRowBySheetAndColumnNumber() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(5, reader.findRowNum(1, 2, 'gång'),
          "$spreadsheetName: find row by number and column number")
    }
  }

  @Test
  void testFindRowBySheetNameAndColumnName() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(5, reader.findRowNum('Sheet1', 'B', 'gång'),
          "$spreadsheetName: find row by sheet name and column name")
    }
  }

  @Test
  void testFindRowBySheetNameAndColumnNumber() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(5, reader.findRowNum('Sheet1', 2, 'gång'),
          "$spreadsheetName: find row by sheet name and column number")
    }
  }

  @Test
  void testFindColumnBySheetName() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(4, reader.findColNum('Sheet1', 8, '20.9'),
          "$spreadsheetName: find column by sheet name")
    }
  }

  @Test
  void testFindLastRowBySheetName() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(12, reader.findLastRow('Sheet1'),
          "$spreadsheetName: find last row by sheet name")
    }
  }

  @Test
  void testFindLastColumnBySheetName() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(4, reader.findLastCol('Sheet1'),
          "$spreadsheetName: find last column by sheet name")
    }
  }

  @Test
  void testGetSheetNames() {
    withBook1Reader { SpreadsheetReader reader, String spreadsheetName ->
      assertEquals(['Sheet1'], reader.getSheetNames(),
          "$spreadsheetName: sheet names")
    }
  }

  private static void withBook1Reader(Closure action) {
    BOOK1_FILES.each { String spreadsheetName ->
      try (SpreadsheetReader reader = SpreadsheetReader.Factory.create(spreadsheetName)) {
        action(reader, spreadsheetName)
      }
    }
  }
}
