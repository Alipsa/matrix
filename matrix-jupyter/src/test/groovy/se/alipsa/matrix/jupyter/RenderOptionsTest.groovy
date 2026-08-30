package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

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
}
