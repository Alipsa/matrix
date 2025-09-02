package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gsheets.GsUtil

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.*

class GsUtilTest {

  @Test
  void testConvertToLocalDate() {
    assertEquals(LocalDate.parse('2024-06-24'), GsUtil.asLocalDate(45467))
    assertEquals(LocalDate.parse('2024-06-24'), GsUtil.asLocalDate('2024-06-24'))
    assertEquals(LocalDate.parse('2025-06-01'), GsUtil.asLocalDate(45809))
    assertEquals(LocalDate.parse('2025-06-01'), GsUtil.asLocalDate('2025-06-01'))
  }

  @Test
  void testConvertToLocalDateTime() {
    assertEquals(LocalDateTime.parse('2022-01-14T12:33:20'), GsUtil.asLocalDateTime(44575.5231481481))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), GsUtil.asLocalDateTime(16674.4180439815))
    assertEquals(LocalDateTime.parse('1945-08-25T10:01:59'), GsUtil.asLocalDateTime('1945-08-25T10:01:59'))
  }

  @Test
  void testConvertLocalTime() {
    assertEquals(LocalTime.parse("18:25:44"), GsUtil.asLocalTime(0.76787037037037))
    assertEquals(LocalTime.parse("06:20"), GsUtil.asLocalTime(0.263888888888889))
    assertEquals(LocalTime.parse("06:20"), GsUtil.asLocalTime("06:20"))
  }

}
