import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.ValueConverter

import java.time.LocalDate
import java.time.YearMonth

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
}