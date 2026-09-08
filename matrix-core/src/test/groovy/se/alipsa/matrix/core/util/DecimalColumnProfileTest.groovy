package se.alipsa.matrix.core.util

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class DecimalColumnProfileTest {

  @Test
  void testProfilesCompatiblePrecisionAndScale() {
    DecimalColumnProfile small = DecimalColumnProfile.profile([0.001g, 0.002g])
    DecimalColumnProfile mixed = DecimalColumnProfile.profile([123.4g, 0.001g])

    assertTrue(small.hasValues)
    assertEquals(4, small.precision)
    assertEquals(3, small.scale)
    assertEquals(6, mixed.precision)
    assertEquals(3, mixed.scale)
  }

  @Test
  void testIgnoresNullsAndReportsEmptyInput() {
    DecimalColumnProfile empty = DecimalColumnProfile.profile([null, null])

    assertFalse(empty.hasValues)
    assertEquals(0, empty.precision)
    assertEquals(0, empty.scale)
  }

  @Test
  void testAccommodatesNegativeScale() {
    DecimalColumnProfile profile = DecimalColumnProfile.profile([new BigDecimal('1E+3'), 0.01g])

    assertEquals(6, profile.precision)
    assertEquals(2, profile.scale)
  }
}
