package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.*
import se.alipsa.matrix.gsheets.GsConverter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gsheets.GsConverter.*

class GsConverterTest {
  @Test
  void testConvertToLocalDate() {
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate(45467))
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate("45467"))
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate('2024-06-24'))
    assertEquals(LocalDate.parse('2025-06-01'), asLocalDate(45809))
    assertEquals(LocalDate.parse('2025-06-01'), asLocalDate('2025-06-01'))
  }

  @Test
  void testConvertToLocalDateTime() {
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime(44575.5231481481))
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime("44575.5231481481"))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), asLocalDateTime(16674.4180439815))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), asLocalDateTime('1945-08-25T10:01:59'))
  }

  @Test
  void testConvertLocalTime() {
    assertEquals(LocalTime.parse("18:25:44"), asLocalTime(0.7678703704))
    assertEquals(LocalTime.parse("18:25:44"), asLocalTime("0.7678703704"))
    assertEquals(LocalTime.parse("06:20"), asLocalTime(0.263888888888889))
    assertEquals(LocalTime.parse("06:20"), asLocalTime("06:20"))
  }

  @Test
  void testLocalTimeAsSerial() {
    assertEquals(0.7678703704, asSerial(LocalTime.parse("18:25:44")))
  }

  @Test
  void testLocalDateAsSerial() {
    assertEquals(new BigDecimal(45467), asSerial(LocalDate.parse('2024-06-24')))
  }

  @Test
  void testLocalDateTimeAsSerial() {
    assertEquals(44575.5231481481, asSerial(LocalDateTime.parse('2022-01-14T12:33:20')))
  }

  @Test
  void testToLocalDates() {
    def list = [45467, "2024-06-24", LocalDate.parse('2024-06-24'), '2024-06-24']
    def dates = toLocalDates(list)
    assertEquals(4, dates.size())
    dates.each { assertEquals(LocalDate.parse('2024-06-24'), it)  }
  }

  @Test
  void testToLocalDateTimes() {
    def list = [44575.5231481481, "44575.5231481481", LocalDateTime.parse('2022-01-14T12:33:20'), '2022-01-14T12:33:20']
    def dateTimes = []
    list.each { dateTimes.add(asLocalDateTime(it)) }
    assertEquals(4, dateTimes.size())
    dateTimes.each { assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), it)  }
  }

  @Test
  void testToLocalTimes() {
    def list = [0.7678703704, "0.7678703704", LocalTime.parse("18:25:44"), '18:25:44']
    def times = []
    list.each { times.add(asLocalTime(it)) }
    assertEquals(4, times.size())
    times.each { assertEquals(LocalTime.parse("18:25:44"), it)  }
  }

  @Test
  void testMatrixConversion() {
    Matrix m = Matrix.builder('testDates').types([BigDecimal]*3)
        .rows([
            [45467, 44575.5231481481, 0.7678703704],
            [45809, 44682.2638888889, 0.263888888888889],
            [44197, 44197.5000000000, 0.5000000000]
        ]
    ).build()
    Matrix m2 = m.clone().convert(
        Converter.of('c1', LocalDate, GsConverter.&asLocalDate),
        Converter.of('c2', LocalDateTime, GsConverter.&asLocalDateTime),
        Converter.of('c3', LocalTime, GsConverter.&asLocalTime)
    )

    Matrix m3 = m2.clone().convert(
        Converter.of('c1', BigDecimal, GsConverter.&asSerial),
        Converter.of('c2', BigDecimal, GsConverter.&asSerial),
        Converter.of('c3', BigDecimal, GsConverter.&asSerial)
    )

    assert m == m3 :  "\nOriginal:\n${m.content()}\nAfter conversions:\n${m3.content()}"
  }

  @Test
  void testAsLocalDateWithCustomFormatter() {
    // Test with US date format (MM/dd/yyyy)
    def usFormatter = java.time.format.DateTimeFormatter.ofPattern('MM/dd/yyyy')
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate('06/24/2024', usFormatter))
    assertEquals(LocalDate.parse('2025-12-31'), asLocalDate('12/31/2025', usFormatter))

    // Test with European date format (dd/MM/yyyy)
    def euFormatter = java.time.format.DateTimeFormatter.ofPattern('dd/MM/yyyy')
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate('24/06/2024', euFormatter))
    assertEquals(LocalDate.parse('2025-12-31'), asLocalDate('31/12/2025', euFormatter))

    // Test default ISO_DATE formatter still works
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate('2024-06-24'))
  }

  @Test
  void testAsLocalDateWithSpaces() {
    // Test that spaces are removed before parsing as number
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate('45 467'))
    assertEquals(LocalDate.parse('2025-06-01'), asLocalDate('45 809'))
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate(' 45467 '))

    // Test with multiple spaces
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate('4 5 4 6 7'))
  }

  @Test
  void testAsLocalDateTimeWithCustomFormatter() {
    // Test with custom date-time format (dd/MM/yyyy HH:mm:ss)
    def customFormatter = java.time.format.DateTimeFormatter.ofPattern('dd/MM/yyyy HH:mm:ss')
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime('14/01/2022 12:33:20', customFormatter))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), asLocalDateTime('25/08/1945 10:01:59', customFormatter))

    // Test with US format (MM/dd/yyyy HH:mm:ss)
    def usFormatter = java.time.format.DateTimeFormatter.ofPattern('MM/dd/yyyy HH:mm:ss')
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime('01/14/2022 12:33:20', usFormatter))
    assertEquals(LocalDateTime.parse('2022-01-14T08:15:30'), asLocalDateTime('01/14/2022 08:15:30', usFormatter))

    // Test default ISO_LOCAL_DATE_TIME formatter still works
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime('2022-01-14T12:33:20'))
  }

  @Test
  void testAsLocalDateTimeWithSpaces() {
    // Test that spaces are removed before parsing as number
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime('44575.523 1481481'))
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime(' 44575.5231481481 '))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), asLocalDateTime('16 674.4180439815'))

    // Test with multiple spaces
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime('4 4 5 7 5.5231481481'))
  }

  // Error case tests
  @Test
  void testAsLocalDateWithNull() {
    assertNull(asLocalDate(null))
  }

  @Test
  void testAsLocalDateTimeWithNull() {
    assertNull(asLocalDateTime(null))
  }

  @Test
  void testAsLocalTimeWithNull() {
    assertNull(asLocalTime(null))
  }

  @Test
  void testAsLocalDateWithInvalidString() {
    assertThrows(IllegalArgumentException, () -> asLocalDate('not-a-date'))
    assertThrows(IllegalArgumentException, () -> asLocalDate('invalid'))
    assertThrows(IllegalArgumentException, () -> asLocalDate('2024-13-45'))
  }

  @Test
  void testAsLocalDateTimeWithInvalidString() {
    assertThrows(IllegalArgumentException, () -> asLocalDateTime('not-a-datetime'))
    assertThrows(IllegalArgumentException, () -> asLocalDateTime('invalid'))
    assertThrows(IllegalArgumentException, () -> asLocalDateTime('2024-13-45T25:99:99'))
  }

  @Test
  void testAsLocalTimeWithInvalidString() {
    assertThrows(IllegalArgumentException, () -> asLocalTime('not-a-time'))
    assertThrows(IllegalArgumentException, () -> asLocalTime('invalid'))
    assertThrows(IllegalArgumentException, () -> asLocalTime('25:99:99'))
  }

  @Test
  void testAsSerialWithNull() {
    // asSerial(Date) should return null for null input
    assertNull(asSerial((Date)null))
  }

  @Test
  void testAsSerialWithNullLocalDate() {
    assertThrows(IllegalArgumentException, () -> asSerial((LocalDate)null))
  }

  @Test
  void testAsSerialWithNullLocalDateTime() {
    assertThrows(IllegalArgumentException, () -> asSerial((LocalDateTime)null))
  }

  @Test
  void testAsSerialWithInvalidObject() {
    assertThrows(IllegalArgumentException, () -> asSerial("not a date object"))
    assertThrows(IllegalArgumentException, () -> asSerial(123))
  }

  @Test
  void testToLocalDatesWithNull() {
    assertNull(toLocalDates(null))
  }

  @Test
  void testToLocalDateTimesWithNull() {
    assertNull(GsConverter.toLocalDateTimes(null))
  }

  @Test
  void testToLocalTimesWithNull() {
    assertNull(GsConverter.toLocalTimes(null))
  }

  @Test
  void testToSerialsWithNull() {
    assertNull(GsConverter.toSerials(null))
  }
}
