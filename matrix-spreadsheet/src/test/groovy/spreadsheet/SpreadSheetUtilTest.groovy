package spreadsheet

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

class SpreadSheetUtilTest {

    @Test
    void testLetterToNumber() {
        assertEquals(1, SpreadsheetUtil.asColumnNumber('A'))
        assertEquals(29, SpreadsheetUtil.asColumnNumber('AC'))
        assertEquals(40, SpreadsheetUtil.asColumnNumber('AN'))
        assertEquals(59, SpreadsheetUtil.asColumnNumber('BG'))
        assertEquals(60, SpreadsheetUtil.asColumnNumber('BH'))
    }

    @Test
    void testNumberToLetter() {
        assertEquals('A', SpreadsheetUtil.asColumnName(1))
        assertEquals('AC', SpreadsheetUtil.asColumnName(29))
        assertEquals('AN', SpreadsheetUtil.asColumnName(40))
        assertEquals('BG', SpreadsheetUtil.asColumnName(59))
        assertEquals('BH', SpreadsheetUtil.asColumnName(60))
    }
}
