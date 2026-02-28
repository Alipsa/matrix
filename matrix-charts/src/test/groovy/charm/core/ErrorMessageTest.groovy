package charm.core

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.assertThrows
import static se.alipsa.matrix.charm.Charts.plot

class ErrorMessageTest {

  private static Matrix sampleData() {
    Matrix.builder()
        .columnNames('cty', 'hwy', 'class')
        .rows([
            [18, 24, 'compact'],
            [21, 31, 'subcompact']
        ])
        .types(Integer, Integer, String)
        .build()
  }

  @Test
  void testUnknownColumnIncludesDidYouMeanSuggestion() {
    CharmValidationException e = assertThrows(CharmValidationException.class) {
      plot(sampleData()) {
        mapping {
          x = 'city'
          y = 'hwy'
        }
        layers { geomPoint() }
      }.build()
    }

    assertTrue(e.message.contains("Unknown column 'city'"))
    assertTrue(e.message.contains('Did you mean: cty?'))
  }

  @Test
  void testColorLiteralInMappingIncludesHint() {
    CharmValidationException e = assertThrows(CharmValidationException.class) {
      plot(sampleData()) {
        mapping {
          x = 'cty'
          y = 'hwy'
          color = '#ff0000'
        }
        layers { geomPoint() }
      }.build()
    }

    assertTrue(e.message.contains("Unknown column '#ff0000'"))
    assertTrue(e.message.contains('looks like a literal color'))
  }

  @Test
  void testMissingYMappingIncludesPointLineHint() {
    CharmValidationException e = assertThrows(CharmValidationException.class) {
      plot(sampleData()) {
        mapping { x = 'cty' }
        layers { geomPoint() }
      }.build()
    }

    assertTrue(e.message.contains('missing required mappings [y]'))
    assertTrue(e.message.contains('point/line layers require both x and y mappings'))
  }

}
