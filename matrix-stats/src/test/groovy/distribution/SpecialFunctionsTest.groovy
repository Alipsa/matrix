package distribution

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.distribution.SpecialFunctions

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for SpecialFunctions class.
 * Reference values computed using R's pbeta(), gamma(), and lgamma() functions.
 */
class SpecialFunctionsTest {

  private static final double TOLERANCE = 1e-6

  @Test
  void testRegularizedIncompleteBetaValidation() {
    // x must be in [0, 1]
    assertThrows(IllegalArgumentException) {
      SpecialFunctions.regularizedIncompleteBeta(-0.1, 2, 3)
    }
    assertThrows(IllegalArgumentException) {
      SpecialFunctions.regularizedIncompleteBeta(1.1, 2, 3)
    }

    // a and b must be positive
    assertThrows(IllegalArgumentException) {
      SpecialFunctions.regularizedIncompleteBeta(0.5, 0, 3)
    }
    assertThrows(IllegalArgumentException) {
      SpecialFunctions.regularizedIncompleteBeta(0.5, 2, -1)
    }
  }

  @Test
  void testRegularizedIncompleteBetaBoundaryValues() {
    // I_0(a, b) = 0
    assertEquals(0.0, SpecialFunctions.regularizedIncompleteBeta(0, 2, 3), TOLERANCE, 'I_0(2,3) = 0')

    // I_1(a, b) = 1
    assertEquals(1.0, SpecialFunctions.regularizedIncompleteBeta(1, 2, 3), TOLERANCE, 'I_1(2,3) = 1')
  }

  @Test
  void testRegularizedIncompleteBetaAgainstR() {
    // Test against R: pbeta(x, a, b)
    // R> pbeta(0.3, 2, 3)
    // [1] 0.3483
    assertEquals(0.3483, SpecialFunctions.regularizedIncompleteBeta(0.3, 2, 3), TOLERANCE, 'I_0.3(2,3)')

    // R> pbeta(0.5, 2, 2)
    // [1] 0.5
    assertEquals(0.5, SpecialFunctions.regularizedIncompleteBeta(0.5, 2, 2), TOLERANCE, 'I_0.5(2,2) symmetric')

    // R> pbeta(0.7, 5, 2)
    // [1] 0.420175
    assertEquals(0.420175, SpecialFunctions.regularizedIncompleteBeta(0.7, 5, 2), TOLERANCE, 'I_0.7(5,2)')

    // R> pbeta(0.25, 0.5, 0.5)
    // [1] 0.3333333
    assertEquals(0.3333333, SpecialFunctions.regularizedIncompleteBeta(0.25, 0.5, 0.5), TOLERANCE, 'I_0.25(0.5,0.5)')

    // R> pbeta(0.9, 10, 3)
    // [1] 0.88913
    assertEquals(0.88913, SpecialFunctions.regularizedIncompleteBeta(0.9, 10, 3), TOLERANCE, 'I_0.9(10,3)')
  }

  @Test
  void testRegularizedIncompleteBetaSymmetry() {
    // I_x(a, b) = 1 - I_(1-x)(b, a)
    double x = 0.3
    double a = 2.0
    double b = 3.0

    double result1 = SpecialFunctions.regularizedIncompleteBeta(x, a, b)
    double result2 = 1.0 - SpecialFunctions.regularizedIncompleteBeta(1.0 - x, b, a)

    assertEquals(result2, result1, TOLERANCE, 'symmetry relation')
  }

  @Test
  void testRegularizedIncompleteBetaSmallX() {
    // Test with very small x values
    // R> pbeta(0.01, 2, 3)
    // [1] 0.00059203
    assertEquals(0.00059203, SpecialFunctions.regularizedIncompleteBeta(0.01, 2, 3), TOLERANCE, 'I_0.01(2,3)')

    // R> pbeta(0.001, 3, 4)
    // [1] 1.995504e-08
    assertEquals(1.995504e-08, SpecialFunctions.regularizedIncompleteBeta(0.001, 3, 4), TOLERANCE, 'I_0.001(3,4)')
  }

  @Test
  void testRegularizedIncompleteBetaLargeX() {
    // Test with values close to 1
    // R> pbeta(0.99, 2, 3)
    // [1] 0.999996
    assertEquals(0.999996, SpecialFunctions.regularizedIncompleteBeta(0.99, 2, 3), TOLERANCE, 'I_0.99(2,3)')

    // R> pbeta(0.999, 3, 4)
    // [1] 1.0
    assertEquals(1.0, SpecialFunctions.regularizedIncompleteBeta(0.999, 3, 4), TOLERANCE, 'I_0.999(3,4)')
  }

  @Test
  void testLogGammaValidation() {
    // x must be positive
    assertThrows(IllegalArgumentException) {
      SpecialFunctions.logGamma(0)
    }
    assertThrows(IllegalArgumentException) {
      SpecialFunctions.logGamma(-5)
    }
  }

  @Test
  void testLogGammaAgainstR() {
    // Test against R: lgamma(x)
    // R> lgamma(1)
    // [1] 0
    assertEquals(0.0, SpecialFunctions.logGamma(1), TOLERANCE, 'log(Γ(1)) = 0')

    // R> lgamma(2)
    // [1] 0
    assertEquals(0.0, SpecialFunctions.logGamma(2), TOLERANCE, 'log(Γ(2)) = 0')

    // R> lgamma(5)
    // [1] 3.17805383034795
    assertEquals(3.17805383034795, SpecialFunctions.logGamma(5), TOLERANCE, 'log(Γ(5))')

    // R> lgamma(10)
    // [1] 12.8018274800815
    assertEquals(12.8018274800815, SpecialFunctions.logGamma(10), TOLERANCE, 'log(Γ(10))')

    // R> lgamma(0.5)
    // [1] 0.5723649
    assertEquals(0.5723649, SpecialFunctions.logGamma(0.5), TOLERANCE, 'log(Γ(0.5))')

    // R> lgamma(100)
    // [1] 359.134205369575
    assertEquals(359.134205369575, SpecialFunctions.logGamma(100), TOLERANCE, 'log(Γ(100))')
  }

  @Test
  void testLogGammaFactorialRelation() {
    // Γ(n) = (n-1)! for positive integers n
    // log(Γ(n)) = log((n-1)!)
    // For n=5: Γ(5) = 4! = 24, so log(Γ(5)) = log(24)
    double expected = Math.log(24.0)
    assertEquals(expected, SpecialFunctions.logGamma(5), TOLERANCE, 'Γ(5) = 4!')
  }

  @Test
  void testGammaFunction() {
    // Test against R: gamma(x)
    // R> gamma(1)
    // [1] 1
    assertEquals(1.0, SpecialFunctions.gamma(1), TOLERANCE, 'Γ(1) = 1')

    // R> gamma(5)
    // [1] 24
    assertEquals(24.0, SpecialFunctions.gamma(5), TOLERANCE, 'Γ(5) = 24')

    // R> gamma(0.5)
    // [1] 1.772454
    assertEquals(1.772454, SpecialFunctions.gamma(0.5), TOLERANCE, 'Γ(0.5) = √π')

    // R> gamma(3.5)
    // [1] 3.323351
    assertEquals(3.323351, SpecialFunctions.gamma(3.5), TOLERANCE, 'Γ(3.5)')
  }

  @Test
  void testGammaHalfInteger() {
    // Γ(0.5) = √π
    double sqrtPi = Math.sqrt(Math.PI)
    assertEquals(sqrtPi, SpecialFunctions.gamma(0.5), TOLERANCE, 'Γ(0.5) = √π')

    // Γ(1.5) = 0.5 * √π
    assertEquals(0.5 * sqrtPi, SpecialFunctions.gamma(1.5), TOLERANCE, 'Γ(1.5) = 0.5√π')
  }

  @Test
  void testLogGammaLargeValues() {
    // Test with large values where direct gamma computation would overflow
    // R> lgamma(200)
    // [1] 857.933669825857
    assertEquals(857.933669825857, SpecialFunctions.logGamma(200), TOLERANCE, 'log(Γ(200))')

    // R> lgamma(500)
    // [1] 2605.11585036173
    assertEquals(2605.11585036173, SpecialFunctions.logGamma(500), TOLERANCE, 'log(Γ(500))')
  }

  @Test
  void testLogGammaSmallValues() {
    // Test with values less than 0.5 (uses reflection formula)
    // R> lgamma(0.1)
    // [1] 2.252713
    assertEquals(2.252713, SpecialFunctions.logGamma(0.1), TOLERANCE, 'log(Γ(0.1))')

    // R> lgamma(0.01)
    // [1] 4.59948
    assertEquals(4.59948, SpecialFunctions.logGamma(0.01), TOLERANCE, 'log(Γ(0.01))')
  }

  @Test
  void testRegularizedIncompleteBetaForTDistribution() {
    // The t-distribution CDF uses regularized incomplete beta
    // Verify it works correctly for typical t-distribution parameters
    // This tests the integration with TDistribution

    // For t=2, df=10, x = df/(df + t^2) = 10/(10+4) = 0.714286
    // R> pbeta(10/14, 5, 0.5)
    // [1] 0.07338803
    double x = 10.0 / 14.0
    double a = 5.0  // df/2
    double b = 0.5
    assertEquals(0.07338803, SpecialFunctions.regularizedIncompleteBeta(x, a, b), TOLERANCE, 't-dist parameter test')
  }

  @Test
  void testRegularizedIncompleteBetaForFDistribution() {
    // The F-distribution CDF uses regularized incomplete beta
    // For f=3, df1=5, df2=10, x = df1*f/(df1*f + df2) = 15/25 = 0.6
    // R> pbeta(0.6, 2.5, 5)
    // [1] 0.9344424
    assertEquals(0.9344424, SpecialFunctions.regularizedIncompleteBeta(0.6, 2.5, 5), TOLERANCE, 'F-dist parameter test')
  }

  @Test
  void testRegularizedIncompleteBetaEqualParameters() {
    // When a = b, I_x(a, a) has nice properties
    // For a=b and x=0.5, I_0.5(a, a) = 0.5 (symmetry)
    assertEquals(0.5, SpecialFunctions.regularizedIncompleteBeta(0.5, 1, 1), TOLERANCE, 'I_0.5(1,1)')
    assertEquals(0.5, SpecialFunctions.regularizedIncompleteBeta(0.5, 5, 5), TOLERANCE, 'I_0.5(5,5)')
    assertEquals(0.5, SpecialFunctions.regularizedIncompleteBeta(0.5, 10, 10), TOLERANCE, 'I_0.5(10,10)')
  }
}
