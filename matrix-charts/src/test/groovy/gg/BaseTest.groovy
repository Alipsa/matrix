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
      if (expected[i] instanceof Number || actual[i] instanceof Number) {
        assertEquals(expected[i] as BigDecimal, actual[i] as BigDecimal, "Value mismatch at index $i, Expected ${expected[i]} but was ${actual[i]}")
      } else if (expected[i] != actual[i]) {
        fail(message ?: "Value mismatch at index $i, Expected ${expected[i]} but was ${actual[i]}")
      }
    }
  }
}
