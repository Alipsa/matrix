package se.alipsa.matrix.charm.util

import groovy.transform.CompileStatic
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Shared numeric coercion helpers used by Charm and gg scale paths.
 */
@CompileStatic
class NumberCoercionUtil {

  /**
   * Coerces an arbitrary value to BigDecimal.
   *
   * @param value value to coerce
   * @return BigDecimal value or null when conversion is not possible
   */
  static BigDecimal coerceToBigDecimal(Object value) {
    if (value == null) return null
    if (value instanceof BigDecimal) return value as BigDecimal
    if (value instanceof Number) {
      if (value instanceof Double || value instanceof Float) {
        double v = value as double
        if (Double.isNaN(v) || Double.isInfinite(v)) return null
      }
      try {
        return new BigDecimal(value.toString())
      } catch (NumberFormatException ignored) {
        return null
      }
    }
    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty() || s.equalsIgnoreCase('NA') || s.equalsIgnoreCase('NaN') || s.equalsIgnoreCase('null')) {
        return null
      }
      try {
        return new BigDecimal(s)
      } catch (NumberFormatException ignored) {
        return null
      }
    }
    if (value instanceof LocalDate) {
      return (value as LocalDate).toEpochDay() as BigDecimal
    }
    if (value instanceof LocalDateTime) {
      return (value as LocalDateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() as BigDecimal
    }
    if (value instanceof ZonedDateTime) {
      return (value as ZonedDateTime).toInstant().toEpochMilli() as BigDecimal
    }
    if (value instanceof OffsetDateTime) {
      return (value as OffsetDateTime).toInstant().toEpochMilli() as BigDecimal
    }
    if (value instanceof Instant) {
      return (value as Instant).toEpochMilli() as BigDecimal
    }
    if (value instanceof LocalTime) {
      return (value as LocalTime).toSecondOfDay() as BigDecimal
    }
    return null
  }
}
