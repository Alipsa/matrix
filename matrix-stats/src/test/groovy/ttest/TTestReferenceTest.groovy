package ttest

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.ttest.Student
import se.alipsa.matrix.stats.ttest.Welch

class TTestReferenceTest {

  @Test
  void tinyWelchSamples() {
    // R 4.6.1: t.test(c(1e-7,2e-7,3e-7,4e-7), c(2e-7,3e-7,5e-7,6e-7))
    def result = Welch.tTest([1e-7, 2e-7, 3e-7, 4e-7], [2e-7, 3e-7, 5e-7, 6e-7])
    assertEquals(-1.341640786d, result.tVal as double, 1e-8d)
    assertEquals(5.4d, result.df as double, 1e-10d)
    assertEquals(0.23337d, result.pVal as double, 1e-5d)
  }

  @Test
  void nullSamplesAndExtremeTail() {
    // R 4.6.1: t.test(c(1.2,NA,2.4,3.1),c(2,2.9,3.8,4.1),var.equal=TRUE)
    def result = Student.tTest([1.2, null, 2.4, 3.1], [2.0, 2.9, 3.8, 4.1], true)
    assertEquals(5, result.df as int)
    assertEquals(-1.327262397d, result.tVal as double, 1e-8d)
    assertEquals(0.241796067d, result.pVal as double, 1e-8d)
    // R 4.6.1: a=seq(0,1,length.out=51); t.test(a+0.6,a,var.equal=TRUE)
    List<BigDecimal> a = (0..50).collect { it / 50.0 }
    def tail = Student.tTest(a.collect { it + 0.6 }, a, true)
    assertEquals(3.785949246e-17d, tail.pVal as double, 4e-23d)
  }

  @Test
  void incompletePairsAreDroppedAndConstantSamplesFail() {
    // R 4.6.1: t.test(c(1,NA,3,5),c(0,2,1,2),paired=TRUE)
    def result = Student.pairedTTest([1, null, 3, 5], [0, 2, 1, 2])
    assertEquals(2, result.df as int)
    assertEquals(0.074179900d, result.pVal as double, 1e-8d)
    assertThrows(IllegalArgumentException) { Student.tTest([1, 1], [2, 2], true) }
    assertThrows(IllegalArgumentException) { Welch.tTest([1, 1], [2, 2]) }
    assertThrows(IllegalArgumentException) { Student.pairedTTest([1, 2], [0, 1]) }
  }
}
