package charm.render.scale

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.render.scale.ScaleUtils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@SuppressWarnings('ExplicitCallToCompareToMethod')
class ScaleUtilsTest {

  @Test
  void strictNumericParsingRejectsDigitBearingLabels() {
    assertTrue(ScaleUtils.isStrictNumeric(3))
    assertTrue(ScaleUtils.isStrictNumeric(' -1.5e3 '))
    assertFalse(ScaleUtils.isStrictNumeric('Q1'))
    assertFalse(ScaleUtils.isStrictNumeric('g1'))
    assertFalse(ScaleUtils.isStrictNumeric('1,234'))
    assertFalse(ScaleUtils.isStrictNumeric(Double.NaN))
    assertNull(ScaleUtils.coerceStrictNumber('Q1'))
  }

  @Test
  void strictNumericInferenceIncludesTemporalValues() {
    assertTrue(ScaleUtils.isStrictNumeric(LocalDate.of(2026, 9, 20)))
    assertTrue(ScaleUtils.isStrictNumeric(LocalDateTime.of(2026, 9, 20, 12, 0)))
    assertTrue(ScaleUtils.isStrictNumeric(Instant.parse('2026-09-20T12:00:00Z')))
    assertTrue(ScaleUtils.isStrictNumeric(new Date(0)))
  }

  @Test
  void niceNumUsesDecimalMagnitudeForSmallAndLargeNumbers() {
    assertEquals(0, ScaleUtils.niceNum(0.00003, true).compareTo(0.00005))
    assertEquals(0, ScaleUtils.niceNum(0.0015, true).compareTo(0.002))
    assertEquals(0, ScaleUtils.niceNum(30_000_000, true).compareTo(50_000_000))
    assertEquals(0, ScaleUtils.niceNum(-1.5, true).compareTo(-2))
  }
}
