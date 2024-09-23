package rootfinding

import se.alipsa.financials.LoanCalculator
import se.alipsa.groovy.matrix.Stat

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.groovy.stats.solver.GoalSeek

/**
 * The expected values are taken from doing the same thing in OpenOffice calc.
 * The methods that Excel and Calc are using (Newton-Raphson with an unknown fallback)
 * are different from the metod used in GoalSeek (Extended Brent-Decker) so the result
 * is very similar, but not identical. We allow a difference on the 9:th decimal in these
 * tests which should be more than enough.
 */
class GoalSeekTest {

  final double delta = 0.00000001

  @Test
  void testGoalSeek() {
    Map val = GoalSeek.solve(27000, 0, 100) {
      it * 100_000
    }
    println val
    assertTrue(val.interations < 37)
    assertEquals(0.270, val.value, delta)
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
    assertEquals(90.0, r.value, delta)
  }

  @Test
  void testLoanAmount() {
    // If i can afford to pay 1500 per month, how much can i borrow?
    double ir = 0.06
    int tenure = 30
    int loanAmt = 300_000

    double expectedLoanAmount = 250187.421588503

    // Sanity check that Calc/Excel PMT is the same as LoanCalculator.pmt()
    assertEquals(-1798.65157546, LoanCalculator.pmt(ir/12, tenure*12, loanAmt), delta)
    // verify that the excel solution and the pmt algorithm agrees
    assertEquals(-1500, LoanCalculator.pmt(ir/12, tenure*12,expectedLoanAmount), delta)

    def s = GoalSeek.solve(-1500, 0, 300_000, delta, 100) { amt ->
      LoanCalculator.pmt(ir/12, tenure*12, amt)
    }
    println(s)
    // Now check that our goal seek produced a similar value
    assertEquals(expectedLoanAmount, s.value, delta, String.valueOf(s))
  }
}
