package se.alipsa.matrix.core

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
