import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.ValueConverter

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

import static org.junit.jupiter.api.Assertions.*

class ValueConverterTest {

    @Test
    void testAsBigDecimal() {
        assertEquals(.00007594000000032963G, ValueConverter.asBigDecimal('7.594000000032963e-05'))
        assertEquals(-12.3G, ValueConverter.asBigDecimal('-0.123E+2'))
    }

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
    void testAsLong() {
        assertEquals(2001251L, ValueConverter.asLong('2001251.0'))
        assertEquals(2001251L, ValueConverter.convert('2001251.0', Long))
        assertEquals(2001251L, ValueConverter.convert(2001251, Long))
        assertEquals(2001251L, ValueConverter.convert(2001251.9, Long))
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

    @Test
    void testAsDouble() {
        assertEquals(.00007594000000032963d, ValueConverter.asDouble('7.594000000032963e-05'))
        assertEquals(-12.3d, ValueConverter.asDouble('-0.123E+2'))
    }

    @Test
    void testAsFloat() {
        assertEquals(.00007594000000032963f, ValueConverter.asFloat('7.594000000032963e-05'))
        assertEquals(-12.3f, ValueConverter.asFloat('-0.123E+2'))
    }

    @Test
    void testAsInteger() {
        assertEquals(485160, ValueConverter.asInteger("485160.00"))
        assertEquals(485161, ValueConverter.asInteger("485161"))
        assertEquals(485162, ValueConverter.asInteger(485162G))
        assertEquals(485163, ValueConverter.asInteger(485163.01))
        assertEquals(485164, ValueConverter.asInteger(485164.999))
        assertEquals(1, ValueConverter.asInteger(true))
        assertEquals(0, ValueConverter.asInteger(false))
        assertEquals(1, ValueConverter.asInteger('TRUE'))
        assertEquals(0, ValueConverter.asInteger('false'))
    }

    @Test
    void testAsIntegerRound() {
        assertEquals(485160, ValueConverter.asIntegerRound("485160.00"))
        assertEquals(485161, ValueConverter.asIntegerRound("485160.7"))
        assertEquals(485162, ValueConverter.asIntegerRound(485161.5G))
        assertEquals(485163, ValueConverter.asIntegerRound(485163.01))
        assertEquals(485164, ValueConverter.asIntegerRound(485163.999))
        assertEquals(485165, ValueConverter.asIntegerRound("485165"))
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
    @SuppressWarnings("deprecation")
    @SuppressWarnings("removal")
    void testAsYearMonth() {
        def expected = YearMonth.of(2023, 5)
        assertEquals(expected, ValueConverter.asYearMonth(new Date(2023 - 1900, 4, 10)))
        assertEquals(expected, ValueConverter.asYearMonth(new GregorianCalendar(2023, 4, 10)))
        assertEquals(expected, ValueConverter.asYearMonth(LocalDate.of(2023, 5, 10)))
        assertEquals(expected, ValueConverter.asYearMonth("2023-05"))
        assertEquals(expected, ValueConverter.asYearMonth(202305.0))
    }

    @Test
    void testConvertWithNullFallback() {
        assertEquals((int)1, ValueConverter.convert(1, int, null, null, 0))
        assertEquals((int)0, ValueConverter.convert(null, int, null, null, 0))
        def d = LocalDate.of(2024,10,27)
        assertEquals(d, ValueConverter.convert(null, LocalDate, null, null, d))
    }
}