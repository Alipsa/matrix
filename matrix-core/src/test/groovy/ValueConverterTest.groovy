import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.ValueConverter

import java.sql.Time
import java.sql.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.*

import static org.junit.jupiter.api.Assertions.*

class ValueConverterTest {

  @Test
  void testAsBigDecimal() {
    assertEquals(.00007594000000032963G, ValueConverter.asBigDecimal('7.594000000032963e-05'))
    assertEquals(-12.3G, ValueConverter.asBigDecimal('-0.123E+2'))
    assertEquals(-9540000.0G, ValueConverter.asBigDecimal('-9540000.0'))
    assertEquals(-9540000.0G, ValueConverter.convert('-9540000.0', BigDecimal))
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
    NumberFormat swFormat = NumberFormat.getInstance(Locale.of("sv", "SE"))
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
    assertEquals((int) 1, ValueConverter.convert(1, int, null, null, 0))
    assertEquals((int) 0, ValueConverter.convert(null, int, null, null, 0))
    def d = LocalDate.of(2024, 10, 27)
    assertEquals(d, ValueConverter.convert(null, LocalDate, null, null, d))
  }

  @Test
  void testAsByte() {
    assertEquals((byte) 127, ValueConverter.asByte("127"))
    assertEquals((byte) -128, ValueConverter.asByte("-128"))
    assertEquals((byte) 0, ValueConverter.asByte("0"))
    assertNull(ValueConverter.asByte(null))
  }

  @Test
  void testAsShort() {
    assertEquals((short) 32767, ValueConverter.asShort("32767"))
    assertEquals((short) -32768, ValueConverter.asShort("-32768"))
    assertEquals((short) 0, ValueConverter.asShort("0"))
    assertNull(ValueConverter.asShort(null))
  }

  @Test
  void testAsBigInteger() {
    assertEquals(new BigInteger("12345678901234567890"), ValueConverter.asBigInteger("12345678901234567890"))
    assertEquals(new BigInteger("-12345678901234567890"), ValueConverter.asBigInteger("-12345678901234567890"))
    assertNull(ValueConverter.asBigInteger(null))
  }

  @Test
  void testAsLocalTime() {
    assertEquals(LocalTime.of(12, 34, 56), ValueConverter.asLocalTime("12:34:56"))
    assertEquals(LocalTime.of(0, 0, 0), ValueConverter.asLocalTime("00:00:00"))
    assertNull(ValueConverter.asLocalTime(null))
  }

  @Test
  void testAsTimestamp() {
    assertEquals(Timestamp.valueOf("2024-10-11 12:34:56"), ValueConverter.asTimestamp("2024-10-11 12:34:56"))
    assertEquals(Timestamp.valueOf("2024-10-11 00:00:00"), ValueConverter.asTimestamp(LocalDate.parse("2024-10-11")))
    assertNull(ValueConverter.asTimestamp(null))
  }

  @Test
  void testAsSqlTime() {
    assertEquals(Time.valueOf("12:34:56"), ValueConverter.asSqlTIme("12:34:56"))
    assertEquals(Time.valueOf("00:00:00"), ValueConverter.asSqlTIme("00:00:00"))
    assertNull(ValueConverter.asSqlTIme(null))
  }

  @Test
  void testAsNumber() {
    assert 123 == ValueConverter.asNumber("123")
    assert 123.45 == ValueConverter.asNumber("123.45")
    assertNull(ValueConverter.asNumber(null))
  }

  @Test
  void testAsBigDecimalNaNAndInfinity() {
    // Cast to Object to ensure dispatch through asBigDecimal(Object) which handles NaN/Infinity
    assertNull(ValueConverter.asBigDecimal((Object) Double.NaN))
    assertNull(ValueConverter.asBigDecimal((Object) Double.POSITIVE_INFINITY))
    assertNull(ValueConverter.asBigDecimal((Object) Double.NEGATIVE_INFINITY))
    assertNull(ValueConverter.asBigDecimal((Object) Float.NaN))
    assertNull(ValueConverter.asBigDecimal((Object) Float.POSITIVE_INFINITY))
    assertNull(ValueConverter.asBigDecimal((Object) Float.NEGATIVE_INFINITY))
  }

  @Test
  void testAsBigDecimalSpecialStrings() {
    assertNull(ValueConverter.asBigDecimal('NA'))
    assertNull(ValueConverter.asBigDecimal('NaN'))
    assertNull(ValueConverter.asBigDecimal('null'))
    assertNull(ValueConverter.asBigDecimal(''))
    assertNull(ValueConverter.asBigDecimal('   '))
    assertNull(ValueConverter.asBigDecimal((String) null))
  }

  @Test
  void testAsBigDecimalUnparseableStrings() {
    assertNull(ValueConverter.asBigDecimal('abc'))
    assertNull(ValueConverter.asBigDecimal('hello world'))
    assertNull(ValueConverter.asBigDecimal('not-a-number'))
  }

  @Test
  void testAsBigDecimalTemporalTypes() {
    def date = LocalDate.of(2024, 1, 15)
    assertEquals(date.toEpochDay() as BigDecimal, ValueConverter.asBigDecimal((Object) date))

    def dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
    def expectedMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() as BigDecimal
    assertEquals(expectedMillis, ValueConverter.asBigDecimal((Object) dateTime))

    def zonedDt = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of('UTC'))
    assertEquals(zonedDt.toInstant().toEpochMilli() as BigDecimal, ValueConverter.asBigDecimal((Object) zonedDt))

    def offsetDt = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC)
    assertEquals(offsetDt.toInstant().toEpochMilli() as BigDecimal, ValueConverter.asBigDecimal((Object) offsetDt))

    def instant = Instant.ofEpochMilli(1705312200000L)
    assertEquals(1705312200000 as BigDecimal, ValueConverter.asBigDecimal((Object) instant))

    def time = LocalTime.of(10, 30, 45)
    assertEquals(time.toSecondOfDay() as BigDecimal, ValueConverter.asBigDecimal((Object) time))
  }

}