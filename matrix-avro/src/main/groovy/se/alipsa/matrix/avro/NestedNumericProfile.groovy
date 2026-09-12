package se.alipsa.matrix.avro

import groovy.transform.PackageScope

import se.alipsa.matrix.core.util.DecimalColumnProfile

/**
 * Incremental numeric characteristics for one nested collection position
 * (list element, map value, or record-like map field) or for the numeric values
 * of an unfixed scalar column.
 */
@PackageScope
final class NestedNumericProfile {

  DecimalColumnProfile decimalProfile
  boolean hasBigInteger
  boolean hasBigDecimal
  boolean hasFloating
  boolean needsLong

  /**
   * Profiles a single non-null numeric value, using the supplied decimal expansion
   * for precision/scale tracking.
   *
   * @param value the original numeric value
   * @param decimal the decimal expansion of {@code value}
   */
  void include(Number value, BigDecimal decimal) {
    decimalProfile = decimalProfile == null
        ? DecimalColumnProfile.profile([decimal])
        : decimalProfile.include(decimal)
    hasBigInteger |= BigInteger.isInstance(value)
    hasBigDecimal |= BigDecimal.isInstance(value)
    hasFloating |= Double.isInstance(value) || Float.isInstance(value)
    needsLong |= NumericKinds.needsLongStorage(value)
  }

  /**
   * @return true once at least one value has been included
   */
  boolean hasValues() {
    decimalProfile?.hasValues == true
  }

  /**
   * @return the schema class for this position: BigInteger for pure BigInteger positions,
   * BigDecimal when BigInteger or BigDecimal values are mixed with anything else,
   * Double for floating-point-only positions, otherwise Long or Integer
   */
  Class<?> schemaClass() {
    if (hasBigInteger && !hasBigDecimal && !hasFloating) {
      return BigInteger
    }
    if (hasBigInteger || hasBigDecimal) {
      return BigDecimal
    }
    if (hasFloating) {
      return Double
    }
    needsLong ? Long : Integer
  }

  /**
   * Returns a profile that can represent every value of this profile and the supplied profile.
   *
   * @param other the profile to combine with this one (may be null)
   * @return a new profile accommodating both profiles' values
   */
  NestedNumericProfile merge(NestedNumericProfile other) {
    if (other == null) {
      return this
    }
    NestedNumericProfile merged = new NestedNumericProfile()
    merged.decimalProfile = decimalProfile?.merge(other.decimalProfile) ?: other.decimalProfile
    merged.hasBigInteger = hasBigInteger || other.hasBigInteger
    merged.hasBigDecimal = hasBigDecimal || other.hasBigDecimal
    merged.hasFloating = hasFloating || other.hasFloating
    merged.needsLong = needsLong || other.needsLong
    merged
  }

  /**
   * Merges a collection of profiles into one.
   *
   * @param profiles the profiles to combine (may be empty or contain nulls)
   * @return the merged profile, or null when there is nothing to merge
   */
  static NestedNumericProfile merge(Collection<NestedNumericProfile> profiles) {
    NestedNumericProfile result = null
    profiles.each { NestedNumericProfile profile ->
      if (profile != null) {
        result = result == null ? profile : result.merge(profile)
      }
    }
    result
  }
}
