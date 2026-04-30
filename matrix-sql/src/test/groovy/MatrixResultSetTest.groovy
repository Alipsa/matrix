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
    long epochMillis = 1714392896000L  // 2024-04-29 12:34:56 UTC
    Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone('UTC'))
    Calendar cetCal = Calendar.getInstance(TimeZone.getTimeZone('Europe/Stockholm'))
    int cetOffset = cetCal.getTimeZone().getOffset(0)

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
    assertEquals(new Date(epochMillis), dateUtc)
    assertFalse(rs.wasNull())

    Time timeUtc = rs.getTime(2, utcCal)
    assertEquals(new Time(epochMillis), timeUtc)
    assertFalse(rs.wasNull())

    Timestamp tsUtc = rs.getTimestamp(3, utcCal)
    assertEquals(new Timestamp(epochMillis), tsUtc)
    assertFalse(rs.wasNull())

    // CET calendar: offset applied
    Date dateCet = rs.getDate(1, cetCal)
    assertEquals(new Date(epochMillis + cetOffset), dateCet)
    Time timeCet = rs.getTime(2, cetCal)
    assertEquals(new Time(epochMillis + cetOffset), timeCet)
    Timestamp tsCet = rs.getTimestamp(3, cetCal)
    assertEquals(new Timestamp(epochMillis + cetOffset), tsCet)
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
    assertEquals(new URL('https://example.com'), rs.getURL(1))
    assertFalse(rs.wasNull())
    assertEquals(new URL('https://example.com'), rs.getURL('site'))
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
    assertFalse(rs.isWrapperFor(String))
    assertNotNull(rs.unwrap(Matrix))
    assertNotNull(rs.unwrap(List))
    assertThrows(SQLException) { rs.unwrap(String) }

    def rsmd = rs.getMetaData()
    assertTrue(rsmd.isWrapperFor(Matrix))
    assertTrue(rsmd.isWrapperFor(List))
    assertFalse(rsmd.isWrapperFor(String))
    assertNotNull(rsmd.unwrap(Matrix))
    assertNotNull(rsmd.unwrap(List))
    assertThrows(SQLException) { rsmd.unwrap(String) }
  }

}
