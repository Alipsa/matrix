import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.sql.MatrixResultSet

import java.net.URI
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

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
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
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

    assertEquals(date, rs.getDate('d', calendar))
    assertEquals(time, rs.getTime('t', calendar))
    assertEquals(timestamp, rs.getTimestamp('ts', calendar))

    assertNull(rs.getDate('missingDate', calendar))
    assertTrue(rs.wasNull())
    assertNull(rs.getTime('missingDate', calendar))
    assertTrue(rs.wasNull())
    assertNull(rs.getTimestamp('missingDate', calendar))
    assertTrue(rs.wasNull())
  }

  @Test
  void testCalendarGettersConvertStringStoredTemporalValues() {
    Matrix matrix = Matrix.builder('stringTemporal').data([
        d: ['2026-04-29'],
        t: ['12:34:56'],
        ts: ['2026-04-29 12:34:56']
    ])
    .types(String, String, String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))

    assertEquals(Date.valueOf('2026-04-29'), rs.getDate(1, cal))
    assertEquals(Time.valueOf('12:34:56'), rs.getTime(2, cal))
    assertEquals(Timestamp.valueOf('2026-04-29 12:34:56'), rs.getTimestamp(3, cal))
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
    assertArrayEquals([-1, -1, -1] as int[], metadata.@precisionByColumn)
    assertArrayEquals([-1, -1, -1] as int[], metadata.@scaleByColumn)
    assertEquals('amount', metadata.getColumnName(1))
    assertArrayEquals([-1, -1, -1] as int[], metadata.@precisionByColumn)
    assertFalse(metadata.isCurrency(1))
    assertEquals(3, metadata.getPrecision(1))
    assertEquals(2, metadata.getScale(1))
    assertEquals(3, metadata.getPrecision(2))
    assertEquals(10, metadata.getPrecision(3))
    rs.updateBigDecimal(1, 123456.789)
    assertEquals(3, metadata.getPrecision(1), 'Precision is a snapshot from when the metadata was created')
    assertThrows(SQLException) { metadata.getColumnName(0) }
    assertThrows(SQLException) { metadata.getColumnType(4) }

    rs.close()
    assertNull(rs.@metaData)
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

}
