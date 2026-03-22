package se.alipsa.matrix.core

/**
 * Assertion helpers for comparing matrices in tests and examples.
 *
 * <p>These wrappers delegate to {@link Matrix#equals(Object, boolean, boolean, boolean, Double, boolean, String)}
 * with repository-standard defaults for structure and numeric tolerance checks.</p>
 */
class MatrixAssertions {

  static void assertEquals(Matrix expected, Matrix actual, String message = '') {
    expected.equals(actual, true, true, false, 0.0001d, true, message)
  }

  static void assertContentEquals(Matrix expected, Matrix actual, String message = '') {
    expected.equals(actual, true, true, false, 0.00001d, true, message)
  }

  static void assertContentMatches(Matrix expected, Matrix actual, String message = '') {
    expected.equals(actual, true, true, true, 0.00001d, true, message)
  }

  static void assertNotEquals(Matrix expected, Matrix actual, String message = '') {
    boolean eq = expected.equals(actual, true, true, false, 0.0001d, false, message)
    if (eq) {
      throw new IllegalArgumentException(message)
    }
  }

  static void assertContentNotEquals(Matrix expected, Matrix actual, String message = '') {
    boolean eq = expected.equals(actual, true, true, true, 0.0001d, false, message)
    if (eq) {
      throw new IllegalArgumentException(message)
    }
  }

}
