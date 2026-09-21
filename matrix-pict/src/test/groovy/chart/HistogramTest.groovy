package chart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.pict.Histogram

class HistogramTest {

  @Test
  void testGrouping() {
    def chart = Histogram.create([1.2, 2.1, 4.1, 4.3, 5.7, 6.2, 6.9, 8.5, 9.9], 3)
    def expected = [3, 4, 2]
    int i = 0
    chart.ranges.each {
      assertEquals(expected[i], it.value)
      i++
    }
  }

  @Test
  void testMtcarsMpg() {
    def mtcars = Dataset.mtcars()
    //println "${mtcars['mpg']}: ${mtcars.type('mpg')}"
    def chart = Histogram.create('mtcars.mpg', mtcars, 'mpg', 5)

    def expected = [6, 12, 8, 2, 4]
    int i = 0
    chart.ranges.each {
      assertEquals(expected[i], it.value)
      i++
    }
  }

  @Test
  void testRoundedBinLabelsDoNotDropValuesAtExactMax() {
    Matrix data = Matrix.builder()
        .matrixName('RoundedHistogram')
        .columns([value: [0.0, 3.4, 6.8, 10.4]])
        .types([Number])
        .build()
    def chart = Histogram.create('Rounded Histogram', data, 'value', 3, 0)

    assertEquals(4, chart.ranges.values().sum())
    assertEquals(['0-3', '3-7', '7-10'], chart.ranges.keySet()*.toString())
  }

  @Test
  void testRangesCountMaxValueWhenChunkRoundsDown() {
    Histogram chart = Histogram.create([0, 0.5, 1], 3)

    assertEquals(3, chart.ranges.values().sum(), "every value must land in a bin: ${chart.ranges}")
    assertEquals(1, chart.ranges.values().last())
    assertEquals(1.0, chart.ranges.keySet().last().maxValue)
  }

  @Test
  void testRangesCountMaxValueWithDefaultBins() {
    Matrix data = Matrix.builder().columns([v: (0..10).toList()]).types([Integer]).build()
    Histogram chart = Histogram.builder(data).x('v').build()

    assertEquals(11, chart.ranges.values().sum(), "got ${chart.ranges}")
    assertEquals(10.0, chart.ranges.keySet().last().maxValue)
  }

  @Test
  void testHistogramIgnoresNullValues() {
    Matrix data = Matrix.builder().columns([v: [1, null, 3, 4]]).types([Integer]).build()
    Histogram chart = Histogram.builder(data).x('v').bins(3).build()

    assertEquals([1, 3, 4], chart.originalData)
    assertEquals(3, chart.ranges.values().sum())
    assertEquals(2, Histogram.create([2, null, 5], 2).originalData.size())
  }

  @Test
  void testHistogramRejectsColumnWithoutNumericValues() {
    Matrix data = Matrix.builder().columns([v: [null, null]]).types([Integer]).build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      Histogram.builder(data).x('v').build()
    }
    assertTrue(ex.message.contains("Column 'v' contains no non-null values"), ex.message)
  }

  @Test
  void testHistogramRejectsNonPositiveBins() {
    Matrix data = Matrix.builder().columns([v: [1, 2, 3]]).types([Integer]).build()

    IllegalArgumentException zero = assertThrows(IllegalArgumentException) { Histogram.builder(data).x('v').bins(0) }
    assertTrue(zero.message.contains('bins must be a positive integer, got 0'), zero.message)
    assertThrows(IllegalArgumentException) { Histogram.builder(data).x('v').bins(null) }
    assertThrows(IllegalArgumentException) { Histogram.create([1, 2, 3], -1) }
    assertThrows(IllegalArgumentException) { Histogram.create('t', data, 'v', 0) }
  }

}
