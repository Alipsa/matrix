package se.alipsa.matrix.core

/**
 * Assertion helpers for comparing matrices in tests and examples.
 *
 * <p>These wrappers delegate to {@link Matrix#equals(Object, boolean, boolean, boolean, Double, boolean, String)}
 * with repository-standard defaults for structure and numeric tolerance checks.</p>
 */
class MatrixAssertions {

  private static final double STRUCTURE_TOLERANCE = 0.0001d
  private static final double CONTENT_TOLERANCE = 0.00001d

  static void assertEquals(Matrix expected, Matrix actual, String message = '') {
    expected.equals(actual, true, true, false, STRUCTURE_TOLERANCE, true, message)
  }

  static void assertContentEquals(Matrix expected, Matrix actual, String message = '') {
    expected.equals(actual, true, true, false, CONTENT_TOLERANCE, true, message)
  }

  static void assertContentMatches(Matrix expected, Matrix actual, String message = '') {
    expected.equals(actual, true, true, true, CONTENT_TOLERANCE, true, message)
  }

  static void assertNotEquals(Matrix expected, Matrix actual, String message = '') {
    boolean eq = expected.equals(actual, true, true, false, STRUCTURE_TOLERANCE, false, message)
    if (eq) {
      throw new IllegalArgumentException(message)
    }
  }

  static void assertContentNotEquals(Matrix expected, Matrix actual, String message = '') {
    boolean eq = expected.equals(actual, true, true, true, STRUCTURE_TOLERANCE, false, message)
    if (eq) {
      throw new IllegalArgumentException(message)
    }
  }

}
