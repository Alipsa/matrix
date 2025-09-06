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
}
