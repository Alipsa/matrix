package rootfinding

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.jfinancials.Financials.*

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.AllowedSolution
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.solver.GoalSeek

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
    assertEquals(-1798.65157546, pmt(ir/12, tenure*12, loanAmt), delta)
    // verify that the excel solution and the pmt algorithm agrees
    assertEquals(-1500, pmt(ir/12, tenure*12,expectedLoanAmount), delta)

    def s = GoalSeek.solve(-1500, 0, 300_000, delta, 100) { amt ->
      pmt(ir/12, tenure*12, amt)
    }
    println(s)
    // Now check that our goal seek produced a similar value
    assertEquals(expectedLoanAmount, s.value, delta, String.valueOf(s))
  }

  @Test
  void testMatchesApacheBrentReference() {
    double target = 27000.0d
    double minValue = 0.0d
    double maxValue = 100.0d
    double threshold = 1.0e-8d

    def actual = GoalSeek.solve(target, minValue, maxValue, threshold, 100) { value ->
      value * 100_000.0d
    }
    double expected = apacheGoalSeek(target, minValue, maxValue, threshold) { value ->
      value * 100_000.0d
    }

    assertEquals(expected, actual.value as double, 1e-10)
  }

  @Test
  void testLargeMagnitudeRootMatchesApacheBrentReference() {
    double target = 1_000_000_000_000.123d
    double minValue = 0.0d
    double maxValue = 2.0e12d
    double threshold = 1.0e-8d

    def actual = GoalSeek.solve(target, minValue, maxValue, threshold, 10) { value ->
      value
    }
    double expected = apacheGoalSeek(target, minValue, maxValue, threshold, 10) { value ->
      value
    }

    assertEquals(expected, actual.value as double, 1e-8)
  }

  private static double apacheGoalSeek(double target, double minValue, double maxValue, double threshold, Closure<Double> algorithm) {
    apacheGoalSeek(target, minValue, maxValue, threshold, 100, algorithm)
  }

  private static double apacheGoalSeek(
      double target,
      double minValue,
      double maxValue,
      double threshold,
      int maxIterations,
      Closure<Double> algorithm
  ) {
    UnivariateFunction function = new UnivariateFunction() {
      @Override
      double value(double v) {
        target - algorithm.call(v)
      }
    }
    double relativeAccuracy = threshold / 10_000.0d
    double absoluteAccuracy = threshold
    def solver = new BracketingNthOrderBrentSolver(relativeAccuracy, absoluteAccuracy, 5)
    solver.solve(maxIterations, function, minValue, maxValue, AllowedSolution.LEFT_SIDE)
  }
}
