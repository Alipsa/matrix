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

}
