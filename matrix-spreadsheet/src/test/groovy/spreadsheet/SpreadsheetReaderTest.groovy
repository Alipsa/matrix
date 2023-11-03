package spreadsheet

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.groovy.spreadsheet.SpreadsheetReader

class SpreadsheetReaderTest {

  @Test
  void testFind() {
    checkBook1("Book1.xlsx")
    checkBook1("Book1.ods")
  }

  static def checkBook1(String spreadsheetName) {
    try (SpreadsheetReader reader = SpreadsheetReader.Factory.create(spreadsheetName)) {
      def lastRow = reader.findLastRow(1)
      assertEquals(12, lastRow, "$spreadsheetName: Last row")
      def endCol = reader.findLastCol(1)
      assertEquals(4, endCol, "$spreadsheetName: End column")
      def firstRow = reader.findRowNum(1, 'B', 'gång')
      assertEquals(5, firstRow, "$spreadsheetName: find first row matching gång")
    }
  }
}
