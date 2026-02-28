package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.render.scale.ScaleEngine

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class DateTimeScaleTest {

  @Test
  void testDateScaleUsesUtcEpochMillisByDefault() {
    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3)],
        Scale.date(),
        0,
        100
    ) as ContinuousCharmScale

    long expectedMin = LocalDate.of(2025, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    long expectedMax = LocalDate.of(2025, 1, 3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    assertEquals(expectedMin, scale.domainMin.longValue())
    assertEquals(expectedMax, scale.domainMax.longValue())
  }

  @Test
  void testTimeScaleUsesMillisSinceMidnight() {
    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalTime.of(6, 30), LocalTime.of(9, 45)],
        Scale.time(),
        0,
        100
    ) as ContinuousCharmScale

    assertEquals(23_400_000L, scale.domainMin.longValue())
    assertEquals(35_100_000L, scale.domainMax.longValue())
  }

  @Test
  void testDatetimeLabelsRespectZoneOverride() {
    Scale datetime = Scale.datetime()
    datetime.params['zoneId'] = 'Europe/Stockholm'
    datetime.params['dateFormat'] = 'yyyy-MM-dd HH:mm'
    Instant tick = Instant.parse('2025-01-01T00:00:00Z')
    datetime.breaks = [tick]

    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [tick, tick.plusSeconds(3600)],
        datetime,
        0,
        100
    ) as ContinuousCharmScale

    List<String> labels = scale.tickLabels(5)
    assertEquals(['2025-01-01 01:00'], labels)
  }

  @Test
  void testDateBreakSpecGeneratesCalendarBreaks() {
    Scale date = Scale.date()
    date.params['dateBreaks'] = '1 month'

    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalDate.of(2025, 1, 10), LocalDate.of(2025, 3, 10)],
        date,
        0,
        100
    ) as ContinuousCharmScale

    List<String> labels = scale.tickLabels(6)
    assertTrue(labels.contains('2025-02-01'))
    assertTrue(labels.contains('2025-03-01'))
  }

  @Test
  void testTimeBreakSpecGeneratesExpectedCadence() {
    Scale time = Scale.time()
    time.params['timeBreaks'] = '2 hours'

    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalTime.of(0, 0), LocalTime.of(8, 0)],
        time,
        0,
        100
    ) as ContinuousCharmScale

    List<Object> ticks = scale.ticks(10)
    assertFalse(ticks.isEmpty())
    assertEquals(0L, (ticks.first() as BigDecimal).longValue())
    assertEquals(7_200_000L, (ticks[1] as BigDecimal).longValue())
  }

  @Test
  void testMultiWeekDateBreaksStayMondayAligned() {
    Scale date = Scale.date()
    date.params['dateBreaks'] = '2 weeks'

    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalDate.of(2025, 1, 10), LocalDate.of(2025, 2, 20)],
        date,
        0,
        100
    ) as ContinuousCharmScale

    List<String> labels = scale.tickLabels(10)
    assertFalse(labels.isEmpty())
    labels.each { String label ->
      LocalDate parsed = LocalDate.parse(label)
      assertEquals(1, parsed.dayOfWeek.value, "Expected Monday-aligned break, got $parsed")
    }
  }

  @Test
  void testDatetimeDurationBreaksAlignToLocalZoneBoundaries() {
    Scale datetime = Scale.datetime()
    datetime.params['zoneId'] = 'Asia/Kolkata'
    datetime.params['dateBreaks'] = '1 hour'
    datetime.params['dateFormat'] = 'HH:mm'

    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [Instant.parse('2025-01-01T00:10:00Z'), Instant.parse('2025-01-01T03:10:00Z')],
        datetime,
        0,
        100
    ) as ContinuousCharmScale

    assertEquals(['06:00', '07:00', '08:00'], scale.tickLabels(10))
  }
}
