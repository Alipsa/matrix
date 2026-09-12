package se.alipsa.matrix.core.spi

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class OptionMapsTest {

  @Test
  void booleanValueOrNullAcceptsOnlyBooleansAndBooleanStrings() {
    assertTrue(OptionMaps.booleanValueOrNull(true, 'enabled'))
    assertFalse(OptionMaps.booleanValueOrNull(false, 'enabled'))
    assertTrue(OptionMaps.booleanValueOrNull(' TRUE ', 'enabled'))
    assertFalse(OptionMaps.booleanValueOrNull('False', 'enabled'))
    assertNull(OptionMaps.booleanValueOrNull(null, 'enabled'))

    [1, 0, 'yes', '', new Object()].each { Object value ->
      IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
        OptionMaps.booleanValueOrNull(value, 'enabled')
      }
      assertTrue(exception.message.startsWith('enabled must be'))
    }
  }
}
