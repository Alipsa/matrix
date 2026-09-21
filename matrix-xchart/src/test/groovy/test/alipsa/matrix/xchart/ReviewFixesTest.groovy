package test.alipsa.matrix.xchart

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.AreaChart
import se.alipsa.matrix.xchart.BoxChart
import se.alipsa.matrix.xchart.BubbleChart
import se.alipsa.matrix.xchart.CorrelationHeatmapChart
import se.alipsa.matrix.xchart.HeatmapChart
import se.alipsa.matrix.xchart.LineChart
import se.alipsa.matrix.xchart.OhlcChart
import se.alipsa.matrix.xchart.PieChart
import se.alipsa.matrix.xchart.RadarChart
import se.alipsa.matrix.xchart.ScatterChart

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

/** Regression tests for the findings in req/v0.4.0-fixes.md. */
@CompileStatic
class ReviewFixesTest {

  private static Matrix gridData() {
    Matrix.builder().data(c1: [1, 2, 3], c2: [4, 5, 6], c3: [7, 8, 9]).types(Number, Number, Number).build()
  }

  @Test
  void heatmapRejectsLabelCountMismatch() {
    Matrix m = gridData()
    List<Column> cols = [m['c1'], m['c2'], m['c3']]
    IllegalArgumentException tooFewColumnLabels = assertThrows(IllegalArgumentException) {
      HeatmapChart.create(m).addSeries('S', ['a', 'b'], ['r1', 'r2', 'r3'], cols)
    }
    assertTrue(tooFewColumnLabels.message.contains('3 columns and 3 rows'), tooFewColumnLabels.message)
    assertTrue(tooFewColumnLabels.message.contains('2 column labels'), tooFewColumnLabels.message)
    assertThrows(IllegalArgumentException) {
      HeatmapChart.create(m).addSeries('S', ['a', 'b', 'c'], ['r1', 'r2'], cols)
    }
    assertThrows(IllegalArgumentException) {
      HeatmapChart.create(m).addSeries('S', ['a', 'b', 'c', 'd'], ['r1', 'r2', 'r3'], cols)
    }
    assertThrows(IllegalArgumentException) {
      HeatmapChart.builder(m).values(['c1', 'c2', 'c3']).columnLabels(['a', 'b']).rowLabels(['r1', 'r2', 'r3']).build()
    }
    assertEquals(9, HeatmapChart.create(m).addSeries('S', ['a', 'b', 'c'], ['r1', 'r2', 'r3'], cols).getSeries('S').heatData.size())
  }

  @Test
  void correlationHeatmapRejectsLabelCountMismatchAndEmptyData() {
    Matrix m = gridData()
    List<Number> first = [1, 2, 3]
    List<Number> second = [4, 5, 6]
    List<Number> third = [7, 8, 9]
    List<List<Number>> corr = [first, second, third]
    assertThrows(IllegalArgumentException) {
      CorrelationHeatmapChart.create(m).addSeries('S', ['a'], ['r1', 'r2', 'r3'], corr)
    }
    assertThrows(IllegalArgumentException) {
      CorrelationHeatmapChart.create(m).addSeries('S', ['a', 'b', 'c'], ['r1'], corr)
    }
    List<List<Number>> empty = []
    assertThrows(IllegalArgumentException) { CorrelationHeatmapChart.create(m).addSeries('S', [], [], empty) }
    List<Number> shorter = [4, 5]
    List<List<Number>> ragged = [first, shorter, third]
    IllegalArgumentException raggedError = assertThrows(IllegalArgumentException) {
      CorrelationHeatmapChart.create(m).addSeries('S', ['a', 'b', 'c'], ['r1', 'r2', 'r3'], ragged)
    }
    assertTrue(raggedError.message.contains('column 1') && raggedError.message.contains('2 values'), raggedError.message)
    List<List<Number>> withNull = [first, null, third]
    IllegalArgumentException nullError = assertThrows(IllegalArgumentException) {
      CorrelationHeatmapChart.create(m).addSeries('S', ['a', 'b', 'c'], ['r1', 'r2', 'r3'], withNull)
    }
    assertTrue(nullError.message.contains('column 1') && nullError.message.contains('null'), nullError.message)
    assertEquals(9, CorrelationHeatmapChart.create(m).addSeries('S', ['a', 'b', 'c'], ['r1', 'r2', 'r3'], corr).getSeries('S').heatData.size())
  }

  @Test
  void heatmapMatrixIsReadOnlyAndReflectsLastSeries() {
    Matrix m = gridData()
    HeatmapChart chart = HeatmapChart.create(m).addSeries('S', [m['c1'], m['c2'], m['c3']])
    assertEquals(3, chart.heatMapMatrix.rowCount())
    assertEquals(3, chart.heatMapMatrix.columnCount())
    assertEquals(4, chart.heatMapMatrix[0, 1])
    assertFalse(HeatmapChart.metaClass.properties.any { it.name == 'numberArray' }, 'numberArray must not be a public property')
    assertFalse(HeatmapChart.metaClass.methods.any { it.name == 'setHeatMapMatrix' }, 'heatMapMatrix must be read-only')
  }

  @Test
  void addAllToSeriesByRejectsUnknownColumn() {
    Matrix m = Matrix.builder().matrixName('hm').data(a: [1, 2], b: [3, 4]).types(Number, Number).build()
    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      HeatmapChart.create(m).addAllToSeriesBy('nope')
    }
    assertTrue(e.message.contains("'nope'"), e.message)
  }

  private static Matrix ohlcData(Class xType, List x) {
    Matrix.builder().data(d: x, o: [1, 2], h: [3, 4], l: [0.5, 1], c: [2, 3])
        .types(xType, Number, Number, Number, Number).build()
  }

  @Test
  void ohlcBuilderAcceptsLocalDateAndLocalDateTimeAndNumber() {
    Matrix localDates = ohlcData(LocalDate, [LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)])
    OhlcChart chart = OhlcChart.builder(localDates).date('d').open('o').high('h').low('l').close('c').build()
    assertNotNull(chart.getSeries('OHLC'))
    Matrix dateTimes = ohlcData(LocalDateTime, [LocalDateTime.of(2024, 1, 1, 9, 30), LocalDateTime.of(2024, 1, 1, 16, 0)])
    assertNotNull(OhlcChart.builder(dateTimes).date('d').open('o').high('h').low('l').close('c').build())
    Matrix numbers = ohlcData(Number, [1, 2])
    assertNotNull(OhlcChart.builder(numbers).date('d').open('o').high('h').low('l').close('c').build())
    Matrix strings = ohlcData(String, ['2024-01-01', '2024-01-02'])
    IllegalArgumentException e = assertThrows(IllegalArgumentException) { OhlcChart.builder(strings).date('d') }
    assertTrue(e.message.contains("'d'"), e.message)
    Matrix years = ohlcData(Year, [Year.of(2023), Year.of(2024)])
    IllegalArgumentException yearError = assertThrows(IllegalArgumentException) { OhlcChart.builder(years).date('d') }
    assertTrue(yearError.message.contains('Year'), yearError.message)
  }

  @Test
  void ohlcAddSeriesAcceptsTemporalLists() {
    Matrix localDates = ohlcData(LocalDate, [LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)])
    OhlcChart chart = OhlcChart.create(localDates)
        .addSeries('X', localDates['d'] as List<?>, localDates['o'] as List<Number>, localDates['h'] as List<Number>,
            localDates['l'] as List<Number>, localDates['c'] as List<Number>)
    assertNotNull(chart.getSeries('X'))
  }

  @Test
  void ohlcAddSeriesValidatesEveryXValue() {
    Matrix m = ohlcData(Number, [1, 2])
    List<Number> o = m['o'] as List<Number>
    List<Number> h = m['h'] as List<Number>
    List<Number> l = m['l'] as List<Number>
    List<Number> c = m['c'] as List<Number>
    IllegalArgumentException unsupported = assertThrows(IllegalArgumentException) {
      OhlcChart.create(m).addSeries('X', [LocalDate.of(2024, 1, 1), Year.of(2024)], o, h, l, c)
    }
    assertTrue(unsupported.message.contains('index 1') && unsupported.message.contains('Year'), unsupported.message)
    IllegalArgumentException leadingNull = assertThrows(IllegalArgumentException) {
      OhlcChart.create(m).addSeries('X', [null, LocalDate.of(2024, 1, 2)], o, h, l, c)
    }
    assertTrue(leadingNull.message.contains('index 0') && leadingNull.message.contains('null'), leadingNull.message)
    IllegalArgumentException trailingNull = assertThrows(IllegalArgumentException) {
      OhlcChart.create(m).addSeries('X', [LocalDate.of(2024, 1, 1), null], o, h, l, c)
    }
    assertTrue(trailingNull.message.contains('index 1'), trailingNull.message)
    IllegalArgumentException mixed = assertThrows(IllegalArgumentException) {
      OhlcChart.create(m).addSeries('X', [1, LocalDate.of(2024, 1, 2)], o, h, l, c)
    }
    assertTrue(mixed.message.contains('index 1') && mixed.message.contains('mix'), mixed.message)
  }

  @Test
  void xyBuildersAcceptTemporalXAndRejectStringXEarly() {
    Matrix dates = Matrix.builder().data(x: [LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)], y: [1, 2])
        .types(LocalDate, Number).build()
    assertNotNull(ScatterChart.builder(dates).x('x').y('y').build())
    assertNotNull(LineChart.builder(dates).x('x').y('y').build())
    assertNotNull(AreaChart.builder(dates).x('x').y('y').build())
    Matrix strings = Matrix.builder().data(x: ['a', 'b'], y: [1, 2]).types(String, Number).build()
    ['line', 'area', 'scatter'].each { String kind ->
      IllegalArgumentException e = assertThrows(IllegalArgumentException) {
        switch (kind) {
          case 'line' -> LineChart.builder(strings).x('x').y('y').build()
          case 'area' -> AreaChart.builder(strings).x('x').y('y').build()
          default -> ScatterChart.builder(strings).x('x').y('y').build()
        }
      }
      assertTrue(e.message.contains("Column 'x' must be numeric or a date/time type"), "$kind: ${e.message}")
    }
    Matrix years = Matrix.builder().data(x: [Year.of(2023), Year.of(2024)], y: [1, 2]).types(Year, Number).build()
    IllegalArgumentException yearError = assertThrows(IllegalArgumentException) { LineChart.builder(years).x('x').y('y').build() }
    assertTrue(yearError.message.contains("Column 'x'"), yearError.message)
  }

  @Test
  void boxBuilderAppliesAxisTitles() {
    Matrix m = Matrix.builder().data(a: [1, 2, 3], b: [2, 3, 4]).types(Number, Number).build()
    BoxChart chart = BoxChart.builder(m).y('a', 'b').xAxisTitle('Series').yAxisTitle('Value').build()
    assertEquals('Series', chart.XLabel)
    assertEquals('Value', chart.YLabel)
    assertThrows(IllegalArgumentException) { BoxChart.builder(m).x('a') }
  }

  @Test
  void groupedScatterKeepsFirstAppearanceOrder() {
    Matrix m = Matrix.builder().data(x: [1, 2, 3, 4, 5], y: [1, 2, 3, 4, 5], g: ['zeta', 'alpha', 'mid', 'beta', 'alpha'])
        .types(Number, Number, String).build()
    ScatterChart sc = ScatterChart.create('t', m, 'x', 'y', 'g')
    assertEquals(['g=zeta', 'g=alpha', 'g=mid', 'g=beta'], sc.series.keySet() as List)
  }

  @Test
  void correlationHeatmapRejectsNullValuesClearly() {
    Matrix m = Matrix.builder().data(a: [1, 2, null, 4], b: [2, 4, 6, 8]).types(Number, Number).build()
    IllegalArgumentException e = assertThrows(IllegalArgumentException) { CorrelationHeatmapChart.create(m).addSeries('S', ['a', 'b']) }
    assertTrue(e.message.contains("'a'"), e.message)
    assertTrue(e.message.toLowerCase().contains('null'), e.message)
  }

  @Test
  void correlationHeatmapIsSymmetricWithUnitDiagonal() {
    Matrix m = Matrix.builder().data(a: [1, 2, 3, 4], b: [2, 4, 6, 8], c: [4, 3, 2, 1]).types(Number, Number, Number).build()
    CorrelationHeatmapChart chart = CorrelationHeatmapChart.create(m).addSeries('S', ['a', 'b', 'c'])
    Map<String, Number> cell = chart.getSeries('S').heatData.collectEntries { Number[] p -> ["${p[0]},${p[1]}".toString(), p[2]] }
    assertEquals(1.0G, cell['0,0'])
    assertEquals(1.0G, cell['1,1'])
    assertEquals(1.0G, cell['2,2'])
    assertEquals(1.0G, cell['0,1'])
    assertEquals(cell['0,1'], cell['1,0'])
    assertEquals(-1.0G, cell['0,2'])
    assertEquals(cell['0,2'], cell['2,0'])
  }

  @Test
  void radarRejectsNullLabelsAndValues() {
    Matrix nullLabel = Matrix.builder().data(name: ['a', null], v1: [0.1, 0.2], v2: [0.3, 0.4]).types(String, Number, Number).build()
    IllegalArgumentException e1 = assertThrows(IllegalArgumentException) { RadarChart.create(nullLabel).addSeries('name') }
    assertTrue(e1.message.contains('row 1'), e1.message)
    Matrix nullValue = Matrix.builder().data(name: ['a', 'b'], v1: [0.1, null], v2: [0.3, 0.4]).types(String, Number, Number).build()
    IllegalArgumentException e2 = assertThrows(IllegalArgumentException) { RadarChart.create(nullValue).addSeries('name') }
    assertTrue(e2.message.contains("'v1'"), e2.message)
    assertTrue(e2.message.contains('row 1'), e2.message)
    Matrix blankLabel = Matrix.builder().data(name: ['a', '  '], v1: [0.1, 0.2], v2: [0.3, 0.4]).types(String, Number, Number).build()
    IllegalArgumentException e3 = assertThrows(IllegalArgumentException) { RadarChart.create(blankLabel).addSeries('name') }
    assertTrue(e3.message.contains("'name'") && e3.message.contains('row 1') && e3.message.contains('blank'), e3.message)
  }

  @Test
  void pieRejectsNullValues() {
    Matrix m = Matrix.builder().data(label: ['a', 'b'], value: [1, null]).types(String, Number).build()
    IllegalArgumentException e = assertThrows(IllegalArgumentException) { PieChart.create(m).addSeries('label', 'value') }
    assertTrue(e.message.contains("'b'"), e.message)
  }

  @Test
  void pieRejectsNullAndBlankLabels() {
    Matrix nullLabel = Matrix.builder().data(label: ['a', null], value: [1, 2]).types(String, Number).build()
    IllegalArgumentException e1 = assertThrows(IllegalArgumentException) { PieChart.create(nullLabel).addSeries('label', 'value') }
    assertTrue(e1.message.contains('row 1') && e1.message.toLowerCase().contains('null'), e1.message)
    Matrix blankLabel = Matrix.builder().data(label: ['a', ' '], value: [1, 2]).types(String, Number).build()
    IllegalArgumentException e2 = assertThrows(IllegalArgumentException) { PieChart.create(blankLabel).addSeries('label', 'value') }
    assertTrue(e2.message.contains('row 1'), e2.message)
  }

  @Test
  void bubbleRejectsNullColumnsOnEveryOverload() {
    Matrix m = Matrix.builder().data(x: [1, 2], y: [1, 2], v: [10, 20]).types(Number, Number, Number).build()
    Column nothing = null
    assertThrows(IllegalArgumentException) { BubbleChart.create(m).addSeries(m['x'], m['y'], nothing) }
    assertThrows(IllegalArgumentException) { BubbleChart.create(m).addSeries('s', nothing, m['y'], m['v']) }
    assertThrows(IllegalArgumentException) { BubbleChart.create(m).addSeries('s', m['x'], nothing, m['v']) }
  }
}
