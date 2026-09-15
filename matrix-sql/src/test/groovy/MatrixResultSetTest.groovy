import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.sql.MatrixResultSet

import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

class MatrixResultSetTest {

  @Test
  void testSimple() {
    Matrix mtcars = Dataset.mtcars()
    ResultSet rs = new MatrixResultSet(mtcars)
    int i = 0
    while (rs.next()) {
      assertEquals(mtcars[i, 0, String], rs.getString(1))
      assertEquals(mtcars[i, 1, BigDecimal], rs.getBigDecimal(2))
      assertEquals(mtcars[i, 2, int, 0], rs.getInt(3))
      assertEquals(mtcars[i, 0], rs.getObject('model'), "model, row $i")
      assertEquals(mtcars[i, 0, String], rs.getObject('model', String), "model, row $i")
      assertEquals(mtcars[i, 1, BigDecimal], rs.getBigDecimal('mpg'))
      assertEquals(mtcars[i, 2, int, 0], rs.getInt('cyl'), mtcars.row(i).toString())
      i++
    }
    rs.first()
    rs.updateString(1, 'Foo')
    rs.updateRow()
    Matrix m = rs.unwrap(Matrix)
    assertEquals('Foo', m[0, 0])

    def rsmd = rs.getMetaData()
    assertEquals(String.getName(), rsmd.getColumnClassName(1), 'First column should be String')
    assertEquals(mtcars.columnCount(), rsmd.columnCount)
    assertEquals('VARCHAR', rsmd.getColumnTypeName(1))
    assertEquals(Types.VARCHAR, rsmd.getColumnType(1))
  }

  @Test
  void testNullHandlingForPrimitiveGettersAndStreams() {
    Matrix matrix = Matrix.builder('nulls').data([
        flag: [null],
        b: [null],
        s: [null],
        i: [null],
        l: [null],
        f: [null],
        d: [null],
        text: [null]
    ])
    .types(Boolean, Byte, Short, Integer, Long, Float, Double, String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())

    assertFalse(rs.getBoolean(1))
    assertTrue(rs.wasNull())

    assertEquals(0 as byte, rs.getByte(2))
    assertTrue(rs.wasNull())

    assertEquals(0 as short, rs.getShort(3))
    assertTrue(rs.wasNull())

    assertEquals(0, rs.getInt(4))
    assertTrue(rs.wasNull())

    assertEquals(0L, rs.getLong(5))
    assertTrue(rs.wasNull())

    assertEquals(0.0f, rs.getFloat(6))
    assertTrue(rs.wasNull())

    assertEquals(0.0d, rs.getDouble(7))
    assertTrue(rs.wasNull())

    assertNull(rs.getAsciiStream(8))
    assertTrue(rs.wasNull())

    assertNull(rs.getUnicodeStream(8))
    assertTrue(rs.wasNull())

    assertNull(rs.getCharacterStream(8))
    assertTrue(rs.wasNull())
  }

  @Test
  void testInvalidCursorAndColumnAccessThrowsSQLException() {
    Matrix matrix = Matrix.builder('invalidAccess').data([
        name: ['Alice']
    ])
    .types(String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertThrows(SQLException) { rs.getString(1) }
    assertThrows(SQLException) { rs.getString(0) }
    assertThrows(SQLException) { rs.findColumn('missing') }
    assertThrows(SQLException) { rs.getString('missing') }

    assertTrue(rs.next())
    assertThrows(SQLException) { rs.getString(2) }
    assertFalse(rs.next())
    assertThrows(SQLException) { rs.getString(1) }

    rs.close()
    assertThrows(SQLException) { rs.next() }
    assertThrows(SQLException) { rs.getMetaData() }
    assertThrows(SQLException) { rs.wasNull() }
    assertThrows(SQLException) { rs.isBeforeFirst() }
    assertThrows(SQLException) { rs.isAfterLast() }
    assertThrows(SQLException) { rs.isFirst() }
    assertThrows(SQLException) { rs.isLast() }
    assertThrows(SQLException) { rs.first() }
    assertThrows(SQLException) { rs.last() }
    assertThrows(SQLException) { rs.beforeFirst() }
    assertThrows(SQLException) { rs.afterLast() }
    assertThrows(SQLException) { rs.absolute(1) }
    assertThrows(SQLException) { rs.relative(1) }
    assertThrows(SQLException) { rs.updateString(1, 'x') }
  }

  @Test
  void testTemporalLabelAndCalendarGettersUpdateWasNull() {
    Date date = Date.valueOf('2026-04-29')
    Time time = Time.valueOf('12:34:56')
    Timestamp timestamp = Timestamp.valueOf('2026-04-29 12:34:56')
    timestamp.setNanos(123456789)
    Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
    Calendar cetCal = Calendar.getInstance(TimeZone.getTimeZone('Europe/Stockholm'))
    Matrix matrix = Matrix.builder('temporal').data([
        d: [date],
        t: [time],
        ts: [timestamp],
        missingDate: [null]
    ])
    .types(Date, Time, Timestamp, Date)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())

    assertEquals(date, rs.getDate('d'))
    assertFalse(rs.wasNull())
    assertEquals(time, rs.getTime('t'))
    assertEquals(timestamp, rs.getTimestamp('ts'))

    // The Calendar overload reinterprets a Date/Time/Timestamp cell the same way it reinterprets a
    // Number cell: decode its raw epoch-millis value as a UTC-sourced wall clock, then re-encode in
    // the given calendar's zone. Prove this is a genuine reinterpretation (not identity) with two
    // different calendars, each verified independently of the production code via pure java.time
    // arithmetic starting from the cell's actual (ambient-timezone-independent) .getTime() value.
    ZoneId utc = ZoneId.of('UTC')
    ZoneId stockholm = ZoneId.of('Europe/Stockholm')

    LocalDate dateWallClock = Instant.ofEpochMilli(date.getTime()).atZone(utc).toLocalDate()
    assertEquals(new Date(dateWallClock.atStartOfDay(utc).toInstant().toEpochMilli()), rs.getDate('d', utcCal))
    assertEquals(new Date(dateWallClock.atStartOfDay(stockholm).toInstant().toEpochMilli()), rs.getDate('d', cetCal))

    LocalTime timeWallClock = Instant.ofEpochMilli(time.getTime()).atZone(utc).toLocalTime()
    assertEquals(new Time(LocalDateTime.of(1970, 1, 1, timeWallClock.hour, timeWallClock.minute, timeWallClock.second)
        .atZone(utc).toInstant().toEpochMilli()), rs.getTime('t', utcCal))
    assertEquals(new Time(LocalDateTime.of(1970, 1, 1, timeWallClock.hour, timeWallClock.minute, timeWallClock.second)
        .atZone(stockholm).toInstant().toEpochMilli()), rs.getTime('t', cetCal))

    LocalDateTime tsWallClock = Instant.ofEpochMilli(timestamp.getTime()).atZone(utc).toLocalDateTime()
    Timestamp expectedTsUtc = Timestamp.from(tsWallClock.atZone(utc).toInstant())
    expectedTsUtc.setNanos(123456789)
    Timestamp actualTsUtc = rs.getTimestamp('ts', utcCal)
    assertEquals(expectedTsUtc, actualTsUtc)
    assertEquals(123456789, actualTsUtc.getNanos(), 'sub-millisecond Timestamp precision must survive the zone shift')

    Timestamp expectedTsCet = Timestamp.from(tsWallClock.atZone(stockholm).toInstant())
    expectedTsCet.setNanos(123456789)
    assertEquals(expectedTsCet, rs.getTimestamp('ts', cetCal))

    assertNull(rs.getDate('missingDate', utcCal))
    assertTrue(rs.wasNull())
    assertNull(rs.getTime('missingDate', utcCal))
    assertTrue(rs.wasNull())
    assertNull(rs.getTimestamp('missingDate', utcCal))
    assertTrue(rs.wasNull())
  }

  @Test
  void testCalendarGettersConvertStringStoredTemporalValues() {
    Matrix matrix = Matrix.builder('stringTemporal').data([
        d: ['2026-04-29'],
        t: ['12:34:56'],
        ts: ['2026-04-29 12:34:56.123456789']
    ])
    .types(String, String, String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())
    Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
    Calendar cetCal = Calendar.getInstance(TimeZone.getTimeZone('Europe/Stockholm'))
    ZoneId utc = ZoneId.of('UTC')
    ZoneId stockholm = ZoneId.of('Europe/Stockholm')

    // The Calendar overload parses String cells independently of the no-Calendar path
    // (LocalDate/LocalTime/LocalDateTime.parse, not the lenient Date/Time/Timestamp.valueOf) and
    // reinterprets the parsed wall-clock fields in the given calendar's zone - a genuine, zone-safe
    // shift, not identity with the no-Calendar getter's result.
    LocalDate expectedDate = LocalDate.of(2026, 4, 29)
    assertEquals(new Date(expectedDate.atStartOfDay(utc).toInstant().toEpochMilli()), rs.getDate(1, utcCal))
    Date dateCet = rs.getDate(1, cetCal)
    assertEquals(new Date(expectedDate.atStartOfDay(stockholm).toInstant().toEpochMilli()), dateCet)
    assertNotEquals(rs.getDate(1, utcCal), dateCet)

    LocalTime expectedTime = LocalTime.of(12, 34, 56)
    assertEquals(new Time(LocalDateTime.of(1970, 1, 1, 0, 0).with(expectedTime).atZone(utc).toInstant().toEpochMilli()),
        rs.getTime(2, utcCal))
    Time timeCet = rs.getTime(2, cetCal)
    assertEquals(new Time(LocalDateTime.of(1970, 1, 1, 0, 0).with(expectedTime).atZone(stockholm).toInstant().toEpochMilli()),
        timeCet)
    assertNotEquals(rs.getTime(2, utcCal), timeCet)

    LocalDateTime expectedTs = LocalDateTime.of(2026, 4, 29, 12, 34, 56, 123456789)
    Timestamp expectedTsUtc = Timestamp.from(expectedTs.atZone(utc).toInstant())
    Timestamp tsUtc = rs.getTimestamp(3, utcCal)
    assertEquals(expectedTsUtc, tsUtc)
    assertEquals(123456789, tsUtc.getNanos(), 'fractional-second precision from the String cell must be preserved')

    Timestamp expectedTsCet = Timestamp.from(expectedTs.atZone(stockholm).toInstant())
    Timestamp tsCet = rs.getTimestamp(3, cetCal)
    assertEquals(expectedTsCet, tsCet)
    assertNotEquals(tsUtc, tsCet)
  }

  @Test
  void testCalendarGettersRejectTheLenientSyntaxTheNoCalendarGettersAccept() {
    // The Calendar overload intentionally accepts a stricter, zero-padded syntax than the
    // no-Calendar overload for String cells (matrix-sql/req/v2.5.0-fixes.md §2.2) - it does not
    // attempt to reproduce Date.valueOf/Time.valueOf/Timestamp.valueOf's lenient, mutually
    // inconsistent wraparound rules. Each case below must succeed on the no-Calendar getter (as
    // legacy JDBC escape parsing already does) and throw on the Calendar overload.
    Matrix matrix = Matrix.builder('lenientDivergence').data([
        singleDigitDate: ['2024-2-3'],
        singleDigitTime: ['1:2:3'],
        wrapTime: ['24:00:00'],
        singleDigitTs: ['2026-4-9 1:2:3'],
        wrapTs: ['2026-04-29 24:00:00'],
        invalidDate: ['2026-04-31 12:00:00'],
        trailingDot: ['2026-04-29 12:34:56.']
    ])
    .types(String, String, String, String, String, String, String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))

    // Neither overload wraps a conversion failure in SQLException today (the no-Calendar path
    // already lets ValueConverter's IllegalArgumentException/DateTimeParseException propagate
    // unchecked; the Calendar overload matches that existing, pre-established behavior rather
    // than introducing new exception-wrapping as an unrelated change).
    assertEquals(Date.valueOf('2024-02-03'), rs.getDate('singleDigitDate'))
    assertThrows(DateTimeParseException) { rs.getDate('singleDigitDate', cal) }

    assertEquals(Time.valueOf('01:02:03'), rs.getTime('singleDigitTime'))
    assertThrows(DateTimeParseException) { rs.getTime('singleDigitTime', cal) }

    // Time.valueOf('24:00:00') succeeds and prints as '00:00:00', but is NOT the same value as
    // Time.valueOf('00:00:00') - verified empirically it is 24h ahead in getTime() millis (the
    // deprecated Time(h,m,s) constructor rolls hour 24 into the next calendar day internally,
    // even though java.sql.Time's toString() never displays a date part).
    assertEquals(Time.valueOf('24:00:00'), rs.getTime('wrapTime'))
    assertThrows(DateTimeParseException) { rs.getTime('wrapTime', cal) }

    assertEquals(Timestamp.valueOf('2026-04-09 01:02:03'), rs.getTimestamp('singleDigitTs'))
    assertThrows(DateTimeParseException) { rs.getTimestamp('singleDigitTs', cal) }

    assertEquals(Timestamp.valueOf('2026-04-29 24:00:00'), rs.getTimestamp('wrapTs'),
        'legacy Timestamp.valueOf rolls 24:00:00 forward to the next day')
    assertThrows(DateTimeParseException) { rs.getTimestamp('wrapTs', cal) }

    assertEquals(Timestamp.valueOf('2026-04-31 12:00:00'), rs.getTimestamp('invalidDate'),
        'legacy Timestamp.valueOf rolls an invalid date (April 31st) forward into May')
    assertThrows(DateTimeParseException) { rs.getTimestamp('invalidDate', cal) }

    // A trailing '.' with no fraction digits is rejected by Timestamp.valueOf itself, so both
    // overloads must throw here - this is not a divergence case.
    assertThrows(IllegalArgumentException) { rs.getTimestamp('trailingDot') }
    assertThrows(DateTimeParseException) { rs.getTimestamp('trailingDot', cal) }
  }

  @Test
  void testCalendarGettersCharSequenceAcceptanceDiffersPerGetter() {
    // ValueConverter.asSqlDate/asTimestamp guard with `instanceof String` and throw on any other
    // CharSequence; ValueConverter.asSqlTime has no such guard and accepts any CharSequence via
    // String.valueOf(o). The Calendar overloads must match their own no-Calendar sibling exactly:
    // getDate/getTimestamp reject a StringBuilder cell on BOTH overloads, getTime accepts it on
    // BOTH overloads.
    Matrix matrix = Matrix.builder('charSequenceCells').data([
        d: [new StringBuilder('2024-04-29')],
        ts: [new StringBuilder('2024-04-29 12:34:56')],
        t: [new StringBuilder('12:34:56')]
    ])
    .types(CharSequence, CharSequence, CharSequence)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())
    Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
    ZoneId utc = ZoneId.of('UTC')

    // getDate/getTimestamp reject a non-String CharSequence on BOTH overloads (matching
    // ValueConverter.asSqlDate/asTimestamp's `instanceof String` guard, which throws otherwise).
    assertThrows(IllegalArgumentException) { rs.getDate('d') }
    assertThrows(IllegalArgumentException) { rs.getDate('d', utcCal) }

    assertThrows(IllegalArgumentException) { rs.getTimestamp('ts') }
    assertThrows(IllegalArgumentException) { rs.getTimestamp('ts', utcCal) }

    // getTime accepts any CharSequence on BOTH overloads (matching ValueConverter.asSqlTime's
    // unguarded String.valueOf(o) fallback) - the Calendar overload must not become stricter than
    // its own no-Calendar sibling here.
    assertEquals(Time.valueOf('12:34:56'), rs.getTime('t'))
    Time expectedTimeUtc = new Time(
        LocalDateTime.of(1970, 1, 1, 0, 0).with(LocalTime.of(12, 34, 56)).atZone(utc).toInstant().toEpochMilli()
    )
    assertEquals(expectedTimeUtc, rs.getTime('t', utcCal),
        "getTime(_, Calendar) must accept any CharSequence cell, matching ValueConverter.asSqlTime's unguarded fallback")
  }

  @Test
  void testCalendarGettersWithNumberMillisAppliesTimezoneOffset() {
    long epochMillis = Instant.parse('2024-04-29T12:34:56Z').toEpochMilli()
    Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
    Calendar cetCal = Calendar.getInstance(TimeZone.getTimeZone('Europe/Stockholm'))
    ZoneId stockholm = ZoneId.of('Europe/Stockholm')

    Matrix matrix = Matrix.builder('millis').data([
        d: [epochMillis],
        t: [epochMillis],
        ts: [epochMillis]
    ])
    .types(Long, Long, Long)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())

    // UTC calendar: no offset change
    Date dateUtc = rs.getDate(1, utcCal)
    long expectedUtcDate = LocalDate.of(2024, 4, 29).atStartOfDay(ZoneId.of('UTC')).toInstant().toEpochMilli()
    assertEquals(new Date(expectedUtcDate), dateUtc)
    assertFalse(rs.wasNull())

    Time timeUtc = rs.getTime(2, utcCal)
    long expectedUtcTime = LocalDateTime.of(1970, 1, 1, 12, 34, 56).atZone(ZoneId.of('UTC')).toInstant().toEpochMilli()
    assertEquals(new Time(expectedUtcTime), timeUtc)
    assertFalse(rs.wasNull())

    Timestamp tsUtc = rs.getTimestamp(3, utcCal)
    assertEquals(new Timestamp(epochMillis), tsUtc)
    assertFalse(rs.wasNull())

    // The stored wall-clock value is interpreted in the supplied calendar's zone.
    Date dateCet = rs.getDate(1, cetCal)
    long expectedDate = LocalDate.of(2024, 4, 29).atStartOfDay(stockholm).toInstant().toEpochMilli()
    assertEquals(new Date(expectedDate), dateCet)
    Time timeCet = rs.getTime(2, cetCal)
    long expectedTime = LocalDateTime.of(1970, 1, 1, 12, 34, 56).atZone(stockholm).toInstant().toEpochMilli()
    assertEquals(new Time(expectedTime), timeCet)
    Timestamp tsCet = rs.getTimestamp(3, cetCal)
    long expectedDateTime = LocalDateTime.of(2024, 4, 29, 12, 34, 56).atZone(stockholm).toInstant().toEpochMilli()
    assertEquals(new Timestamp(expectedDateTime), tsCet)

    long transitionWallTime = Instant.parse('2024-10-27T01:30:00Z').toEpochMilli()
    ResultSet transition = new MatrixResultSet(
        Matrix.builder('transition').data([ts: [transitionWallTime]]).types(Long).build()
    )
    assertTrue(transition.next())
    long expectedTransition = ZonedDateTime.of(
        LocalDateTime.of(2024, 10, 27, 1, 30), stockholm
    ).toInstant().toEpochMilli()
    assertEquals(new Timestamp(expectedTransition), transition.getTimestamp(1, cetCal))
  }

  @Test
  void testCalendarGettersReinterpretLocalDateLocalTimeLocalDateTimeCells() {
    LocalDate localDate = LocalDate.of(2024, 4, 29)
    LocalTime localTime = LocalTime.of(12, 34, 56)
    LocalDateTime localDateTime = LocalDateTime.of(2024, 4, 29, 12, 34, 56)
    Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
    Calendar cetCal = Calendar.getInstance(TimeZone.getTimeZone('Europe/Stockholm'))
    ZoneId utc = ZoneId.of('UTC')
    ZoneId stockholm = ZoneId.of('Europe/Stockholm')

    Matrix matrix = Matrix.builder('javaTimeCells').data([
        d: [localDate],
        dt: [localDateTime],
        t: [localTime],
        ts: [localDateTime]
    ])
    .types(LocalDate, LocalDateTime, LocalTime, LocalDateTime)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())

    // LocalDate/LocalTime/LocalDateTime cells are already zoneless wall-clock values - no
    // ambient-default-zone decoding is involved, they are reinterpreted directly.
    assertEquals(new Date(localDate.atStartOfDay(utc).toInstant().toEpochMilli()), rs.getDate('d', utcCal))
    assertEquals(new Date(localDate.atStartOfDay(stockholm).toInstant().toEpochMilli()), rs.getDate('d', cetCal))

    // getDate on a LocalDateTime cell must drop the time-of-day, matching ValueConverter.asSqlDate.
    assertEquals(new Date(localDate.atStartOfDay(utc).toInstant().toEpochMilli()), rs.getDate('dt', utcCal))

    assertEquals(new Time(LocalDateTime.of(1970, 1, 1, 0, 0).with(localTime).atZone(utc).toInstant().toEpochMilli()),
        rs.getTime('t', utcCal))
    assertEquals(new Time(LocalDateTime.of(1970, 1, 1, 0, 0).with(localTime).atZone(stockholm).toInstant().toEpochMilli()),
        rs.getTime('t', cetCal))

    assertEquals(Timestamp.from(localDateTime.atZone(utc).toInstant()), rs.getTimestamp('ts', utcCal))
    Timestamp tsCet = rs.getTimestamp('ts', cetCal)
    assertEquals(Timestamp.from(localDateTime.atZone(stockholm).toInstant()), tsCet)

    // getTimestamp on a LocalDate cell must apply atStartOfDay(), matching ValueConverter.asTimestamp.
    assertEquals(Timestamp.from(localDate.atStartOfDay().atZone(utc).toInstant()), rs.getTimestamp('d', utcCal))
  }

  @Test
  void testCalendarGetTimestampReinterpretsZonedDateTimeCellByDiscardingItsOwnZone() {
    // A ZonedDateTime cell already carries its own zone (New York), unlike every other cell type
    // reinterpreted by this method. Per the documented decision (matrix-sql/req/v2.5.0-fixes.md
    // §2.1), getTimestamp(_, Calendar) discards that embedded zone and reinterprets the cell's
    // local wall-clock fields in cal's zone - it must NOT convert the New York instant into cal's
    // zone.
    ZonedDateTime zonedCell = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneId.of('America/New_York'))
    Calendar cetCal = Calendar.getInstance(TimeZone.getTimeZone('Europe/Stockholm'))
    ZoneId stockholm = ZoneId.of('Europe/Stockholm')

    Matrix matrix = Matrix.builder('zonedCell').data([ts: [zonedCell]]).types(ZonedDateTime).build()
    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())

    Timestamp actual = rs.getTimestamp(1, cetCal)

    // Expected: the LOCAL fields (2024-06-15T10:30:00), reinterpreted in Stockholm's zone - same
    // as an equivalent LocalDateTime cell would produce.
    Timestamp expectedLocalFieldsReinterpreted = Timestamp.from(
        zonedCell.toLocalDateTime().atZone(stockholm).toInstant()
    )
    assertEquals(expectedLocalFieldsReinterpreted, actual)

    // Must NOT be the New York instant converted into Stockholm's zone (the rejected alternative
    // policy) - assert the two differ, pinning the chosen policy against the other one.
    Timestamp instantConvertedToStockholm = Timestamp.from(zonedCell.withZoneSameInstant(stockholm).toInstant())
    assertNotEquals(instantConvertedToStockholm, actual)
  }

  @Test
  void testCalendarGettersAreIndependentOfJvmDefaultTimezoneAtDstGap() {
    TimeZone original = TimeZone.getDefault()
    try {
      // Stockholm's spring-forward gap on 2024-03-31 runs 02:00 -> 03:00; 02:30 does not exist as
      // a local Stockholm time that day. Parsing/reinterpreting it must give the identical result
      // regardless of the JVM's ambient default timezone at the moment of the call.
      LocalDate localDate = LocalDate.of(2024, 3, 31)
      LocalTime localTime = LocalTime.of(2, 30, 0)
      LocalDateTime localDateTime = LocalDateTime.of(2024, 3, 31, 2, 30, 0)

      Matrix matrix = Matrix.builder('dstGap').data([
          strTs: ['2024-03-31 02:30:00'],
          strD: ['2024-03-31'],
          strT: ['02:30:00'],
          ld: [localDate],
          lt: [localTime],
          ldt: [localDateTime]
      ])
      .types(String, String, String, LocalDate, LocalTime, LocalDateTime)
      .build()

      Calendar cetCal = Calendar.getInstance(TimeZone.getTimeZone('Europe/Stockholm'))

      TimeZone.setDefault(TimeZone.getTimeZone('Europe/Stockholm'))
      ResultSet rsStockholmDefault = new MatrixResultSet(matrix)
      assertTrue(rsStockholmDefault.next())
      Map underStockholmDefault = [
          strTs: rsStockholmDefault.getTimestamp('strTs', cetCal),
          strD: rsStockholmDefault.getDate('strD', cetCal),
          strT: rsStockholmDefault.getTime('strT', cetCal),
          ld: rsStockholmDefault.getDate('ld', cetCal),
          lt: rsStockholmDefault.getTime('lt', cetCal),
          ldt: rsStockholmDefault.getTimestamp('ldt', cetCal)
      ]

      TimeZone.setDefault(TimeZone.getTimeZone('UTC'))
      ResultSet rsUtcDefault = new MatrixResultSet(matrix)
      assertTrue(rsUtcDefault.next())
      Map underUtcDefault = [
          strTs: rsUtcDefault.getTimestamp('strTs', cetCal),
          strD: rsUtcDefault.getDate('strD', cetCal),
          strT: rsUtcDefault.getTime('strT', cetCal),
          ld: rsUtcDefault.getDate('ld', cetCal),
          lt: rsUtcDefault.getTime('lt', cetCal),
          ldt: rsUtcDefault.getTimestamp('ldt', cetCal)
      ]

      underStockholmDefault.each { key, value ->
        assertEquals(value, underUtcDefault[key],
            "getter for '$key' must be independent of the JVM default timezone")
      }

      // The DST-gap value must parse to exactly the requested local fields, reinterpreted in the
      // target calendar's zone - not silently shifted by an ambient-zone-dependent construction
      // path (the Timestamp.valueOf(str).toLocalDateTime() approach this fix replaced).
      Timestamp expectedTs = Timestamp.from(localDateTime.atZone(ZoneId.of('Europe/Stockholm')).toInstant())
      assertEquals(expectedTs, underUtcDefault.strTs)
    } finally {
      TimeZone.setDefault(original)
    }
  }

  @Test
  void testGetURLRoutesThroughGuardsAndUpdatesLastReadValue() {
    Matrix matrix = Matrix.builder('urls').data([
        site: ['https://example.com', null]
    ])
    .types(String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)

    // Cursor before first row should throw
    assertThrows(SQLException) { rs.getURL(1) }
    assertThrows(SQLException) { rs.getURL('site') }

    assertTrue(rs.next())

    // Valid access by index and label
    assertEquals(URI.create('https://example.com').toURL(), rs.getURL(1))
    assertFalse(rs.wasNull())
    assertEquals(URI.create('https://example.com').toURL(), rs.getURL('site'))
    assertFalse(rs.wasNull())

    // Null value
    assertTrue(rs.next())
    assertNull(rs.getURL(1))
    assertTrue(rs.wasNull())
    assertNull(rs.getURL('site'))
    assertTrue(rs.wasNull())

    // Invalid column index and label
    assertThrows(SQLException) { rs.getURL(0) }
    assertThrows(SQLException) { rs.getURL(2) }
    assertThrows(SQLException) { rs.getURL('missing') }

    // After last row
    assertFalse(rs.next())
    assertThrows(SQLException) { rs.getURL(1) }

    // Closed result set
    rs.close()
    assertThrows(SQLException) { rs.getURL(1) }
    assertThrows(SQLException) { rs.getURL('site') }
  }

  @Test
  void testWrapperContracts() {
    Matrix matrix = Matrix.builder('wrap').data([
        name: ['x']
    ])
    .types(String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.isWrapperFor(Matrix))
    assertTrue(rs.isWrapperFor(List))
    assertTrue(rs.isWrapperFor(ResultSet))
    assertFalse(rs.isWrapperFor(String))
    assertNotNull(rs.unwrap(Matrix))
    assertNotNull(rs.unwrap(List))
    assertSame(rs, rs.unwrap(ResultSet))
    assertThrows(SQLException) { rs.unwrap(String) }

    def rsmd = rs.getMetaData()
    assertTrue(rsmd.isWrapperFor(Matrix))
    assertTrue(rsmd.isWrapperFor(List))
    assertTrue(rsmd.isWrapperFor(java.sql.ResultSetMetaData))
    assertFalse(rsmd.isWrapperFor(String))
    assertNotNull(rsmd.unwrap(Matrix))
    assertNotNull(rsmd.unwrap(List))
    assertSame(rsmd, rsmd.unwrap(java.sql.ResultSetMetaData))
    assertThrows(SQLException) { rsmd.unwrap(String) }

    rs.close()
    assertThrows(SQLException) { rs.unwrap(Matrix) }
  }

  @Test
  void testAbsoluteRelativeAndPreviousCursorMovement() {
    Matrix matrix = Matrix.builder('cursor').data([id: [1, 2, 3], value: ['a', 'b', 'c']]).types(int, String).build()
    ResultSet rs = new MatrixResultSet(matrix)

    assertTrue(rs.absolute(-1))
    assertEquals(3, rs.getRow())
    assertEquals(3, rs.getInt(1))

    assertFalse(rs.absolute(0))
    assertTrue(rs.isBeforeFirst())
    assertEquals(0, rs.getRow())

    assertTrue(rs.absolute(3))
    assertFalse(rs.relative(5))
    assertTrue(rs.isAfterLast())
    assertEquals(0, rs.getRow())
    assertTrue(rs.previous())
    assertEquals(3, rs.getRow())

    int reverseCount = 0
    while (rs.previous()) {
      reverseCount++
    }
    assertEquals(2, reverseCount)
    assertTrue(rs.isBeforeFirst())
    assertFalse(rs.previous())
  }

  @Test
  void testDeleteRowDoesNotSkipTheFollowingRow() {
    Matrix source = Matrix.builder('deletable').data([id: [1, 2, 3, 4]]).types(int).build()
    ResultSet rs = new MatrixResultSet(source)
    // MatrixResultSet clones its constructor argument, so assertions must go against the
    // result set's own live data, not the original source Matrix.
    Matrix rsData = rs.unwrap(Matrix)

    assertTrue(rs.absolute(2))
    assertEquals(2, rs.getInt(1))
    rs.deleteRow()

    assertEquals(3, rsData.rowCount())
    // The row that slid into the deleted slot (previously id=3) must now be the current row,
    // not skipped.
    assertEquals(3, rs.getInt(1))
    assertEquals(4, source.rowCount(), 'the original Matrix passed to the constructor must be untouched')

    assertTrue(rs.next())
    assertEquals(4, rs.getInt(1), 'next() must not skip the row that slid down')

    // Deleting the last remaining row must leave the cursor correctly positioned after-last.
    assertTrue(rs.absolute(-1))
    assertEquals(4, rs.getInt(1))
    rs.deleteRow()
    assertEquals(2, rsData.rowCount())
    assertFalse(rs.next())
    assertTrue(rs.isAfterLast())

    rs.beforeFirst()
    assertThrows(SQLException) { rs.deleteRow() }
  }

  @Test
  void testRepeatedNextKeepsCursorAfterLast() {
    ResultSet rs = new MatrixResultSet(
        Matrix.builder('forward').data([id: [1, 2, 3]]).types(int).build()
    )

    assertTrue(rs.next())
    assertTrue(rs.next())
    assertTrue(rs.next())
    assertFalse(rs.next())
    assertFalse(rs.next())
    assertTrue(rs.isAfterLast())
    assertTrue(rs.previous())
    assertEquals(3, rs.getRow())
  }

  @Test
  void testEmptyCursorStateAndRoundedBigDecimal() {
    ResultSet empty = new MatrixResultSet(Matrix.builder('empty').data([amount: []]).types(BigDecimal).build())
    assertFalse(empty.isBeforeFirst())
    assertFalse(empty.isAfterLast())
    assertFalse(empty.isFirst())
    assertFalse(empty.isLast())

    ResultSet rs = new MatrixResultSet(
        Matrix.builder('decimal').data([amount: [1.2345]]).types(BigDecimal).build()
    )
    assertTrue(rs.next())
    assertEquals(1.23, rs.getBigDecimal(1, 2))
  }

  @Test
  void testUpdaterValidationAndMetadataContracts() {
    ResultSet rs = new MatrixResultSet(
        Matrix.builder('metadata').data([amount: [123.4500], name: ['xyz'], count: [1]]).types(BigDecimal, String, int).build()
    )
    assertThrows(SQLException) { rs.updateString(1, 'x') }
    assertThrows(SQLException) { rs.updateString('name', 'x') }
    assertTrue(rs.next())
    assertThrows(SQLException) { rs.updateString(0, 'x') }
    assertThrows(SQLException) { rs.updateString(4, 'x') }
    assertThrows(SQLException) { rs.updateString('missing', 'x') }
    assertThrows(SQLException) { rs.updateObject(4, 1.2345, 2) }
    rs.updateObject(1, 1.2345, 2)
    assertEquals(1.23, rs.getBigDecimal(1))
    rs.updateObject('amount', 2.3456, 2)
    assertEquals(2.35, rs.getBigDecimal('amount'))

    def metadata = rs.metaData
    assertSame(rs.metaData, metadata)
    assertEquals('amount', metadata.getColumnName(1))
    assertFalse(metadata.isCurrency(1))
    rs.updateBigDecimal(1, 123456.789)
    assertEquals(9, metadata.getPrecision(1), 'Column-name access must not calculate precision')
    rs.updateBigDecimal(1, 1.2)
    assertEquals(9, metadata.getPrecision(1), 'Precision is cached after it is first requested')
    assertEquals(3, metadata.getScale(1), 'Scale is cached with precision for a compatible numeric shape')
    rs.updateBigDecimal(1, 1.2345)
    assertEquals(3, metadata.getScale(1), 'Scale is cached after it is first requested')
    assertEquals(3, metadata.getPrecision(2))
    assertEquals(10, metadata.getPrecision(3))
    assertThrows(SQLException) { metadata.getColumnName(0) }
    assertThrows(SQLException) { metadata.getColumnType(4) }

    rs.close()
    assertThrows(SQLException) { rs.updateString('name', 'x') }
    assertThrows(SQLException) { rs.metaData }
  }

  @Test
  void testGetPrecisionIgnoresSignForBigInteger() {
    ResultSet rs = new MatrixResultSet(
        Matrix.builder('bigints').data([value: [-123]]).types(BigInteger).build()
    )
    assertEquals(3, rs.metaData.getPrecision(1))
  }

  @Test
  void testDecimalPrecisionAccommodatesMaximumScaleAndIntegerDigits() {
    ResultSet rs = new MatrixResultSet(
        Matrix.builder('decimalPrecision')
            .data([small: [0.001g, 0.002g], mixed: [123.4g, 0.001g]])
            .types(BigDecimal, BigDecimal)
            .build()
    )

    assertEquals(4, rs.metaData.getPrecision(1))
    assertEquals(3, rs.metaData.getScale(1))
    assertEquals(6, rs.metaData.getPrecision(2))
    assertEquals(3, rs.metaData.getScale(2))
  }

}
