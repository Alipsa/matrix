package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.bridge.GgCharmCompilation
import se.alipsa.matrix.gg.bridge.GgCharmCompiler

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

class DateTimeScaleBridgeTest {

  @Test
  void testDateScaleParamsArePropagatedToCharm() {
    Matrix data = Matrix.builder()
        .columnNames('d', 'y')
        .rows([
            [LocalDate.of(2025, 1, 1), 1],
            [LocalDate.of(2025, 1, 2), 2]
        ])
        .types(LocalDate, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'd', y: 'y')) +
        geom_point() +
        scale_x_date(date_breaks: '1 day', date_format: 'yyyy-MM-dd')

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    Scale x = adaptation.charmChart.scale.x
    assertEquals('date', x.transform)
    assertEquals('1 day', x.params['dateBreaks'])
    assertEquals('yyyy-MM-dd', x.params['dateFormat'])
    assertEquals(ZoneId.systemDefault().id, x.params['zoneId'])
  }

  @Test
  void testTimeScaleMapsToCharmTimeAndCarriesZoneId() {
    Matrix data = Matrix.builder()
        .columnNames('t', 'y')
        .rows([
            [LocalTime.of(8, 0), 1],
            [LocalTime.of(10, 30), 2]
        ])
        .types(LocalTime, Integer)
        .build()

    def chart = ggplot(data, aes(x: 't', y: 'y')) +
        geom_point() +
        scale_x_time(time_breaks: '30 minutes', time_format: 'HH:mm')

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    Scale x = adaptation.charmChart.scale.x
    assertEquals('time', x.transform)
    assertEquals('30 minutes', x.params['timeBreaks'])
    assertEquals('HH:mm', x.params['timeFormat'])
    assertEquals(ZoneId.systemDefault().id, x.params['zoneId'])
  }

  @Test
  void testDatetimeScaleMapsToCharmDatetimeAndKeepsExplicitBreaks() {
    Matrix data = Matrix.builder()
        .columnNames('dt', 'y')
        .rows([
            [LocalDateTime.of(2025, 1, 1, 0, 0), 1],
            [LocalDateTime.of(2025, 1, 1, 12, 0), 2]
        ])
        .types(LocalDateTime, Integer)
        .build()
    List<LocalDateTime> explicitBreaks = [
        LocalDateTime.of(2025, 1, 1, 0, 0),
        LocalDateTime.of(2025, 1, 1, 6, 0)
    ]

    def chart = ggplot(data, aes(x: 'dt', y: 'y')) +
        geom_point() +
        scale_x_datetime(
            date_breaks: '6 hours',
            date_format: 'yyyy-MM-dd HH:mm',
            breaks: explicitBreaks,
            labels: ['start', 'mid']
        )

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    Scale x = adaptation.charmChart.scale.x
    assertEquals('datetime', x.transform)
    assertEquals(explicitBreaks, x.breaks)
    assertEquals(['start', 'mid'], x.labels)
    assertEquals('6 hours', x.params['dateBreaks'])
    assertEquals('yyyy-MM-dd HH:mm', x.params['dateFormat'])
    assertEquals(ZoneId.systemDefault().id, x.params['zoneId'])
    assertNotNull(x.params['zoneId'])
  }
}
