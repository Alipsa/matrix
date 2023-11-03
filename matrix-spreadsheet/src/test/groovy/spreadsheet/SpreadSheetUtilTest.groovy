package spreadsheet

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import se.alipsa.groovy.spreadsheet.SpreadsheetUtil

class SpreadSheetUtilTest {

    @Test
    void testLetterToNumber() {
        assertEquals(1, SpreadsheetUtil.asColumnNumber('A'))
        assertEquals(40, SpreadsheetUtil.asColumnNumber('AN'))
        assertEquals(59, SpreadsheetUtil.asColumnNumber('BG'))
    }

    @Test
    void testNumberToLetter() {
        assertEquals('A', SpreadsheetUtil.asColumnName(1))
        assertEquals('AN', SpreadsheetUtil.asColumnName(40))
        assertEquals('BG', SpreadsheetUtil.asColumnName(59))
    }
}
