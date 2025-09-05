package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static se.alipsa.matrix.gsheets.GsUtil.*
import static org.junit.jupiter.api.Assertions.*

class GsUtilTest {

  @Test
  void testConvertToLocalDate() {
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate(45467))
    assertEquals(LocalDate.parse('2024-06-24'), asLocalDate('2024-06-24'))
    assertEquals(LocalDate.parse('2025-06-01'), asLocalDate(45809))
    assertEquals(LocalDate.parse('2025-06-01'), asLocalDate('2025-06-01'))
  }

  @Test
  void testConvertToLocalDateTime() {
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), asLocalDateTime(44575.5231481481))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), asLocalDateTime(16674.4180439815))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), asLocalDateTime('1945-08-25T10:01:59'))
  }

  @Test
  void testConvertLocalTime() {
    assertEquals(LocalTime.parse("18:25:44"), asLocalTime(0.7678703704))
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
  void testColumnCountForRange() {
    def range = "Arkiv!B2:H100"
    def columns = columnCountForRange(range)
    println "The number of columns in the range is: ${columns}"
    assertEquals(7, columns)

// Example with a single-letter range
    def simpleRange = "C:F"
    def simpleColumns = columnCountForRange(simpleRange)
    println "The number of columns in the simple range is: ${simpleColumns}"
    assertEquals(4, simpleColumns)

// Example with a multi-letter range
    def multiRange = "A1:AB10"
    def multiColumns = columnCountForRange(multiRange)
    println "The number of columns in the multi-letter range is: ${multiColumns}"
  }

}
