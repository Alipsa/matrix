package chart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charts.Histogram
import se.alipsa.matrix.datasets.Dataset

import static org.junit.jupiter.api.Assertions.*

class HistogramTest {

  @Test
  void testGrouping() {
    def chart = Histogram.create([1.2, 2.1, 4.1, 4.3, 5.7, 6.2, 6.9, 8.5, 9.9], 3)
    def expected = [3,4,2]
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
    def chart = Histogram.create("mtcars.mpg", mtcars, 'mpg', 5)

    def expected = [6,12,8,2,4]
    int i = 0;
    chart.ranges.each {
      assertEquals(expected[i], it.value)
      i++
    }

  }
}
