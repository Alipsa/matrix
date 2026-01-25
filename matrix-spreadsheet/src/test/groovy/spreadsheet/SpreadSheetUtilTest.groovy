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

    @Test
    void testCreateValidSheetName() {
        // Test basic sanitization
        assertEquals('Sheet1', SpreadsheetUtil.createValidSheetName('Sheet1'))

        // Test invalid characters replaced with space
        assertEquals('A B', SpreadsheetUtil.createValidSheetName('A/B'))
        assertEquals('A B', SpreadsheetUtil.createValidSheetName('A?B'))
        assertEquals('A B', SpreadsheetUtil.createValidSheetName('A*B'))
        assertEquals('A B', SpreadsheetUtil.createValidSheetName('A:B'))
        assertEquals('A B', SpreadsheetUtil.createValidSheetName('A\\B'))
        assertEquals('A B', SpreadsheetUtil.createValidSheetName('A[B'))
        assertEquals('A B', SpreadsheetUtil.createValidSheetName('A]B'))

        // Test max length (31 chars)
        String longName = 'A' * 50
        assertEquals(31, SpreadsheetUtil.createValidSheetName(longName).length())

        // Test null and empty handling
        assertEquals('null', SpreadsheetUtil.createValidSheetName(null))
        assertEquals('empty', SpreadsheetUtil.createValidSheetName(''))
    }

    @Test
    void testCreateUniqueSheetNames() {
        // Test no collisions
        def names = SpreadsheetUtil.createUniqueSheetNames(['Sheet1', 'Sheet2', 'Sheet3'])
        assertEquals(['Sheet1', 'Sheet2', 'Sheet3'], names)

        // Test collision after sanitization (A/B and A?B both become "A B")
        def colliding = SpreadsheetUtil.createUniqueSheetNames(['A/B', 'A?B', 'A*B'])
        assertEquals(3, colliding.size())
        assertEquals('A B', colliding[0])
        assertEquals('A B1', colliding[1])
        assertEquals('A B2', colliding[2])

        // Test duplicate names
        def duplicates = SpreadsheetUtil.createUniqueSheetNames(['Data', 'Data', 'Data'])
        assertEquals(['Data', 'Data1', 'Data2'], duplicates)

        // Test mixed collision scenarios
        def mixed = SpreadsheetUtil.createUniqueSheetNames(['Sheet1', 'Sheet/1', 'Sheet2'])
        assertEquals(3, mixed.size())
        assertTrue(mixed[0] != mixed[1], "Sanitized names should be unique")
    }

    @Test
    void testCreateUniqueSheetNamesMaxLength() {
        // Test that numeric suffix respects 31-char limit
        String longName = 'A' * 31  // Max length name
        def names = SpreadsheetUtil.createUniqueSheetNames([longName, longName])
        assertEquals(2, names.size())
        assertTrue(names[0].length() <= 31, "First name should be max 31 chars")
        assertTrue(names[1].length() <= 31, "Suffixed name should be max 31 chars")
        assertNotEquals(names[0], names[1], "Names should be unique")
    }

    @Test
    void testParseCellPosition() {
        def pos = SpreadsheetUtil.parseCellPosition('A1')
        assertEquals(1, pos.row)
        assertEquals(1, pos.column)

        pos = SpreadsheetUtil.parseCellPosition('B3')
        assertEquals(3, pos.row)
        assertEquals(2, pos.column)

        pos = SpreadsheetUtil.parseCellPosition('AA100')
        assertEquals(100, pos.row)
        assertEquals(27, pos.column)
    }

    @Test
    void testParseCellPositionInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            SpreadsheetUtil.parseCellPosition(null)
        })

        assertThrows(IllegalArgumentException.class, () -> {
            SpreadsheetUtil.parseCellPosition('')
        })

        assertThrows(IllegalArgumentException.class, () -> {
            SpreadsheetUtil.parseCellPosition('1A')  // Wrong order
        })

        assertThrows(IllegalArgumentException.class, () -> {
            SpreadsheetUtil.parseCellPosition('ABC')  // Missing row
        })
    }

    @Test
    void testEnsureXlsx() {
        // Should not throw for xlsx
        SpreadsheetUtil.ensureXlsx('test.xlsx')
        SpreadsheetUtil.ensureXlsx('TEST.XLSX')
        SpreadsheetUtil.ensureXlsx((String) null)  // null is allowed

        // Should throw for xls
        assertThrows(IllegalArgumentException.class, () -> {
            SpreadsheetUtil.ensureXlsx('test.xls')
        })

        assertThrows(IllegalArgumentException.class, () -> {
            SpreadsheetUtil.ensureXlsx('TEST.XLS')
        })
    }
}
