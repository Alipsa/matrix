package se.alipsa.matrix.core.util

import java.lang.reflect.Array

/**
 * Compares and hashes Matrix cell values according to Groovy equality semantics.
 *
 * Numeric values compare by mathematical value. Numeric tolerance applies directly
 * to numeric values and recursively to elements of ordered sequences (Lists and
 * arrays). Sets and maps retain exact Groovy equality semantics.
 */
final class ValueComparison {

  private ValueComparison() {
  }

  /**
   * Determines whether two values differ using the supplied numeric tolerance.
   *
   * @param left the first value
   * @param right the second value
   * @param allowedDiff maximum allowed numeric difference for direct values and
   *        ordered sequence elements; Sets and Maps compare exactly
   * @return true when the values differ
   */
  static boolean valuesAreDifferent(Object left, Object right, BigDecimal allowedDiff) {
    if (left instanceof Character && right instanceof Number) {
      return numericValuesAreDifferent(left.charValue() as int, right, allowedDiff)
    }
    if (left instanceof Number && right instanceof Character) {
      return numericValuesAreDifferent(left, right.charValue() as int, allowedDiff)
    }
    if (left instanceof Number && right instanceof Number) {
      return numericValuesAreDifferent(left, right, allowedDiff)
    }
    if (isSequence(left) && isSequence(right)) {
      return sequenceValuesAreDifferent(left, right, allowedDiff)
    }
    left != right
  }

  /**
   * Produces a hash consistent with {@link #valuesAreDifferent(Object, Object, BigDecimal)}
   * when the numeric tolerance is zero.
   *
   * @param value the value to hash
   * @return its normalized hash code
   */
  static int normalizedValueHash(Object value) {
    if (value == null) {
      return 0
    }
    if (value instanceof Number) {
      if (isNonFiniteFloatingPoint(value)) {
        return Double.hashCode(value.doubleValue())
      }
      BigDecimal normalized = value.toBigDecimal().stripTrailingZeros()
      // Groovy considers an integral numeric value equal to its matching Character
      // and one-character String. Character and String hash to the character code,
      // so use that hash for the Character range rather than BigDecimal.hashCode().
      if (normalized.scale() <= 0 && normalized >= Character.MIN_VALUE && normalized <= Character.MAX_VALUE) {
        return normalized.intValue()
      }
      return normalized.hashCode()
    }
    if (value instanceof CharSequence) {
      return value.toString().hashCode()
    }
    if (value.getClass().isArray()) {
      return sequenceValueHash(sequenceValues(value))
    }
    if (value instanceof Map) {
      return mapValueHash(value)
    }
    if (value instanceof Set) {
      return setValueHash(value)
    }
    if (value instanceof Collection) {
      return sequenceValueHash(value)
    }
    value.hashCode()
  }

  private static boolean numericValuesAreDifferent(Number left, Number right, BigDecimal allowedDiff) {
    boolean leftIsNonFinite = isNonFiniteFloatingPoint(left)
    boolean rightIsNonFinite = isNonFiniteFloatingPoint(right)
    if (leftIsNonFinite || rightIsNonFinite) {
      return !leftIsNonFinite || !rightIsNonFinite || Double.compare(left.doubleValue(), right.doubleValue()) != 0
    }
    (left.toBigDecimal() - right.toBigDecimal()).abs() > allowedDiff
  }

  private static boolean isNonFiniteFloatingPoint(Number value) {
    if (value instanceof Double || value instanceof Float) {
      double floatingPointValue = value.doubleValue()
      return Double.isNaN(floatingPointValue) || Double.isInfinite(floatingPointValue)
    }
    false
  }

  private static boolean isSequence(Object value) {
    value != null && (value.getClass().isArray() || value instanceof List)
  }

  private static boolean sequenceValuesAreDifferent(Object left, Object right, BigDecimal allowedDiff) {
    List<Object> leftValues = sequenceValues(left)
    List<Object> rightValues = sequenceValues(right)
    if (leftValues.size() != rightValues.size()) {
      return true
    }
    for (int i = 0; i < leftValues.size(); i++) {
      if (valuesAreDifferent(leftValues[i], rightValues[i], allowedDiff)) {
        return true
      }
    }
    false
  }

  private static List<Object> sequenceValues(Object value) {
    if (value.getClass().isArray()) {
      int length = Array.getLength(value)
      List<Object> result = new ArrayList<>(length)
      for (int i = 0; i < length; i++) {
        result.add(Array.get(value, i))
      }
      return result
    }
    new ArrayList<>(value as List<Object>)
  }

  private static int sequenceValueHash(Collection<Object> values) {
    int result = 1
    for (Object value : values) {
      result = 31 * result + normalizedValueHash(value)
    }
    result
  }

  private static int setValueHash(Set<Object> values) {
    int result = 0
    for (Object value : values) {
      result += normalizedValueHash(value)
    }
    result
  }

  private static int mapValueHash(Map<Object, Object> values) {
    int result = 0
    for (Map.Entry<Object, Object> entry : values.entrySet()) {
      result += normalizedValueHash(entry.key) ^ normalizedValueHash(entry.value)
    }
    result
  }
}
