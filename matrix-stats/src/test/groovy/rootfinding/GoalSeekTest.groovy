package rootfinding

import se.alipsa.financials.LoanCalculator
import se.alipsa.groovy.matrix.Stat

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.groovy.stats.rootfinding.GoalSeek

class GoalSeekTest {

  @Test
  void testGoalSeek() {
    Map val = GoalSeek.solve(27000, 0, 100) {
      it * 100_000
    }
    println val
    assertTrue(val.interations < 37)
    assertEquals(0.270, val.value, 0.0000001)
  }

  @Test
  void testList() {
    // There has been 3 tests, one remains, what score do we need to have to get a total average of 70?
    List grades = [50, 80, 60]
    def r = GoalSeek.solve(70, 0, 100, 0.0001, 100) {
      grade ->
        List n = grades + [grade]
        Stat.mean(n)
    }
    println(r)
    assertEquals(90.0, r.value, 0.001)
  }

  @Test
  void testLoanAmount() {
    def ir = 0.06
    def tenure = 30
    def loanAmt = 300_000

    assertEquals(-1798.65158, LoanCalculator.pmt(ir/12, tenure*12, loanAmt), 0.0001)
    assertEquals(-1500, LoanCalculator.pmt(ir/12, tenure*12,250187.421588503), 0.0001)

    def s = GoalSeek.solve(1500, -300_000, 0, 0.00001, 1000) { amt ->
      LoanCalculator.pmt(ir/12, tenure*12, amt)
    }
    assertEquals(-250187.421588503, s.value, String.valueOf(s))
  }
}
