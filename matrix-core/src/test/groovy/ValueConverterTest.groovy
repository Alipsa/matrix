import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.ValueConverter

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

import static org.junit.jupiter.api.Assertions.*

class ValueConverterTest {

    @Test
    void testAsBoolean() {
        assertEquals(true, ValueConverter.asBoolean(1))
        assertEquals(true, ValueConverter.asBoolean(1.0))
        assertEquals(true, ValueConverter.asBoolean('YES'))
        assertEquals(true, ValueConverter.asBoolean('on'))
        assertEquals(true, ValueConverter.asBoolean('True'))

        assertEquals(false, ValueConverter.asBoolean(0))
        assertEquals(false, ValueConverter.asBoolean(-1.0))
        assertEquals(false, ValueConverter.asBoolean('NO'))
        assertEquals(false, ValueConverter.asBoolean('off'))
        assertEquals(false, ValueConverter.asBoolean('False'))
    }

    @Test
    @SuppressWarnings("deprecation")
    @SuppressWarnings("removal")
    void testAsYearMonth() {
        def expected = YearMonth.of(2023, 5)
        assertEquals(expected, ValueConverter.asYearMonth(new Date(2023 - 1900, 4, 10)))
        assertEquals(expected, ValueConverter.asYearMonth(new GregorianCalendar(2023, 4, 10)))
        assertEquals(expected, ValueConverter.asYearMonth(LocalDate.of(2023, 5, 10)))
        assertEquals(expected, ValueConverter.asYearMonth("2023-05"))
    }

    @Test
    void testAsLong() {
        assertEquals(2001251L, ValueConverter.asLong('2001251.0'))
        assertEquals(2001251L, ValueConverter.convert('2001251.0', Long))
        assertEquals(2001251L, ValueConverter.convert(2001251, Long))
        assertEquals(2001251L, ValueConverter.convert(2001251.9, Long))
    }

    @Test
    void testIsNumeric() {
        NumberFormat enFormat = NumberFormat.getInstance(Locale.ENGLISH)
        NumberFormat swFormat = NumberFormat.getInstance(new Locale("sv","SE"))
        assertTrue(ValueConverter.isNumeric('123'))
        assertTrue(ValueConverter.isNumeric('123.4', enFormat))
        assertTrue(ValueConverter.isNumeric('123,4', swFormat))
        assertTrue(ValueConverter.isNumeric('-123'))
        assertTrue(ValueConverter.isNumeric(-123))
        assertTrue(ValueConverter.isNumeric(123_234.5))
        assertFalse(ValueConverter.isNumeric('12ab3'))
        assertFalse(ValueConverter.isNumeric('abc'))
        assertFalse(ValueConverter.isNumeric(LocalDate.now()))
        assertFalse(ValueConverter.isNumeric(null))
    }

    @Test
    void testAsDate() {
        assertEquals(new Date(1728667826640), ValueConverter.asDate(1728667826640))
        assertEquals(java.sql.Date.valueOf("2024-10-11"), ValueConverter.asDate(20241011))
        assertEquals(java.sql.Date.valueOf("2024-10-11"), ValueConverter.asDate('20241011'))
        assertEquals(java.sql.Date.valueOf("2024-10-11"), ValueConverter.asDate("2024-10-11"))
        assertEquals(java.sql.Date.valueOf("2024-10-11"), ValueConverter.asDate("2024/10/11", "yy/MM/dd"))
        def ld = LocalDate.of(2024, 10, 11)
        assertEquals(
            ld.toString(),
            new SimpleDateFormat("yyyy-MM-dd").format(ValueConverter.asDate(ld))
        )
        def ldt = LocalDateTime.now()
        assertEquals(
            ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ValueConverter.asDate(ldt).getTime()
        )
    }
}