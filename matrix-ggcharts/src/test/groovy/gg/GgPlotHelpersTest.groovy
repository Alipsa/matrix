package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.aes.AfterScale

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

class GgPlotHelpersTest {

  @Test
  void testAfterScaleHelper() {
    AfterScale ref = after_scale('color')
    assertEquals('color', ref.aesthetic)
  }

  @Test
  void testVarsHelper() {
    List<String> varsList = vars('a', 'b', ['c', 'd'])
    assertEquals(['a', 'b', 'c', 'd'], varsList)
  }

  @Test
  void testExpansionHelper() {
    List<Number> expanded = expansion(mult: 0.1, add: 0.2)
    assertEquals(2, expanded.size())
    assertEquals(0.1d, expanded[0] as double, 1e-9)
    assertEquals(0.2d, expanded[1] as double, 1e-9)

    List<Number> expandedPositional = expansion(0.05, 1)
    assertTrue(expandedPositional[0] instanceof Number)
    assertTrue(expandedPositional[1] instanceof Number)
  }
}
