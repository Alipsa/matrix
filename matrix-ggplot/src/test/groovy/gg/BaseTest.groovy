package gg
import static org.junit.jupiter.api.Assertions.fail
import static org.junit.jupiter.api.Assertions.assertEquals as assertEqualsJUnit

class BaseTest {

  /**
   * Custom assertEquals for BigDecimal that uses compareTo instead of equals.
   * This compares numeric value only, ignoring scale differences.
   *
   * @param expected the expected BigDecimal value
   * @param actual the actual BigDecimal value
   * @param message optional failure message
   */
  static void assertEquals(BigDecimal expected, BigDecimal actual, String message = '') {
    if (expected == null && actual == null) return
    if (expected == null || actual == null) {
      fail(message ?: "Expected ${expected} but was ${actual}")
    }
    if (expected.compareTo(actual) != 0) {
      fail(message ?: "Expected ${expected} but was ${actual}")
    }
  }

  static void assertEquals(BigDecimal expected, BigDecimal actual, Number tolerance, String message = '') {
    if (expected == null && actual == null) return
    if (expected == null || actual == null) {
      fail(message ?: "Expected ${expected} but was ${actual}")
    }
    if ((expected - actual).abs() > tolerance) {
      fail(message ?: "Expected ${expected} but was ${actual}")
    }
  }

  /**
   * Custom assertEquals overload that accepts Number as expected value.
   * Converts the Number to BigDecimal before comparison.
   *
   * @param expected the expected numeric value
   * @param actual the actual BigDecimal value
   * @param message optional failure message
   */
  static void assertEquals(Number expected, BigDecimal actual, String message = '') {
    assertEquals(expected as BigDecimal, actual, message)
  }

  static void assertEquals(Number expected, BigDecimal actual, Number tolerance, String message = '') {
    assertEquals(expected as BigDecimal, actual, tolerance, message)
  }

  // Delegate to JUnit's assertEquals for non-BigDecimal types
  static void assertEquals(int expected, int actual) {
    assertEqualsJUnit(expected, actual)
  }

  static void assertEquals(List expected, List actual, String message = '') {
    assertIterableEquals(expected, actual, message)
  }

  static void assertIterableEquals(List expected, List actual, String message = '') {
    assertEqualsJUnit(expected.size(), actual.size(), "Size mismatch")
    for (int i = 0; i < expected.size(); i++) {
      def exp = expected[i]
      def act = actual[i]
      if (exp == null && act == null) return
      if (exp == null || act == null) {
        fail(message ?: "Value mismatch at index $i, Expected ${exp} but was ${act}")
      }
      if (exp instanceof Number || act instanceof Number) {
        assertEquals(new BigDecimal(exp.toString()), new BigDecimal(act.toString()), "Value mismatch at index $i, Expected ${exp} but was ${act}")
      } else if (exp != act) {
        fail(message ?: "Value mismatch at index $i, Expected ${exp} but was ${act}")
      }
    }
  }
}
