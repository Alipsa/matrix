package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

/** Tests for rendering option defaults and defensive copies. */
class RenderOptionsTest {
  @Test
  void retainsDefaultsAndCopiesAttributes() {
    Map<String, String> attributes = [class: 'compact']
    RenderOptions options = new RenderOptions(10, 5, false, attributes, 1024, 768)
    attributes['class'] = 'changed'

    assertEquals(10, options.maxRows)
    assertEquals(5, options.maxColumns)
    assertEquals(false, options.fromHead)
    assertEquals('compact', options.attr['class'])
    assertEquals(1024, options.width)
    assertEquals(768, options.height)
    assertThrows(UnsupportedOperationException) { options.attr.id = 'table' }
  }

  @Test
  void rejectsNegativeLimitsAndNonPositiveSizes() {
    IllegalArgumentException rows = assertThrows(IllegalArgumentException) { new RenderOptions(-1, 50) }
    IllegalArgumentException columns = assertThrows(IllegalArgumentException) { new RenderOptions(50, -1) }
    IllegalArgumentException width = assertThrows(IllegalArgumentException) { new RenderOptions(50, 50, true, [:], 0, 600) }
    IllegalArgumentException height = assertThrows(IllegalArgumentException) { new RenderOptions(50, 50, true, [:], 800, -5) }

    assertTrue(rows.message.contains('maxRows'))
    assertTrue(columns.message.contains('maxColumns'))
    assertTrue(width.message.contains('width'))
    assertTrue(height.message.contains('height'))
  }

  @Test
  void allowsNullAndZeroLimits() {
    RenderOptions unlimited = new RenderOptions(null, null)
    RenderOptions zero = new RenderOptions(0, 0)

    assertNull(unlimited.maxRows)
    assertNull(unlimited.maxColumns)
    assertEquals(0, zero.maxRows)
    assertEquals(0, zero.maxColumns)
  }
}
