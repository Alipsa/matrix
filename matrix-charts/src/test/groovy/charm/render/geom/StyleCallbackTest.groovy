package charm.render.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

/**
 * Tests for the per-datum style callback on layers.
 *
 * <p>Uses {@code geomCol()} (stat=IDENTITY) so that each datum retains its
 * original row reference. {@code geomBar()} uses stat=COUNT which produces
 * synthetic data that does not preserve per-row metadata.</p>
 */
class StyleCallbackTest {

  @Test
  void testConditionalFillOverride() {
    def data = Matrix.builder().data(
        month: ['Jan', 'Feb', 'Mar'],
        pct: [72, 45, 88]
    ).types(String, Integer).build()

    def chart = plot(data) {
      mapping { x = 'month'; y = 'pct' }
      layers {
        geomCol().fill('#2ecc71').style { row, s ->
          if (row['pct'] < 50) s.fill = '#e74c3c'
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)

    assertTrue(xml.contains('class="charm-bar"'), 'Should render bars')
    // Feb (pct=45) should be red, others should be green from layer param
    assertTrue(xml.contains('#e74c3c'), 'Should contain the style callback override color for low pct')
    assertTrue(xml.contains('#2ecc71'), 'Should contain the layer param default color for high pct')
  }

  @Test
  void testConditionalColorOverride() {
    def data = Matrix.builder().data(
        month: ['Jan', 'Feb', 'Mar'],
        pct: [72, 45, 88]
    ).types(String, Integer).build()

    def chart = plot(data) {
      mapping { x = 'month'; y = 'pct' }
      layers {
        geomCol().style { row, s ->
          if (row['month'] == 'Feb') s.color = 'yellow'
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)
    assertTrue(xml.contains('yellow'), 'Feb bar should have yellow stroke from style callback')
  }

  @Test
  void testMultipleOverridesInOneCallback() {
    def data = Matrix.builder().data(
        x: ['A', 'B'],
        y: [10, 20]
    ).types(String, Integer).build()

    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomCol().style { row, s ->
          if (row['x'] == 'A') {
            s.fill = '#ff0000'
            s.color = '#0000ff'
          }
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)

    assertTrue(xml.contains('class="charm-bar"'), 'Should render bars')
    assertTrue(xml.contains('#ff0000'), 'Should contain the overridden fill')
    assertTrue(xml.contains('#0000ff'), 'Should contain the overridden stroke')
  }

  @Test
  void testCallbackWithNoOverridesPassesThrough() {
    def data = Matrix.builder().data(
        x: ['A', 'B'],
        y: [10, 20]
    ).types(String, Integer).build()

    // Style callback that doesn't set anything
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomCol().fill('#aabbcc').style { row, s ->
          // intentionally empty
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)

    assertTrue(xml.contains('class="charm-bar"'), 'Should render bars')
    // All bars should use the layer param fill since callback sets nothing
    int barCount = countOccurrences(xml, 'class="charm-bar"')
    int fillCount = countOccurrences(xml, 'fill="#aabbcc"')
    assertTrue(barCount > 0, 'Should render at least one bar')
    assertTrue(fillCount >= barCount, 'Layer param should be used when style callback sets nothing')
  }

  @Test
  void testStyleCallbackOverridesLayerParam() {
    def data = Matrix.builder().data(
        x: ['A'],
        y: [10]
    ).types(String, Integer).build()

    // Style callback overrides the layer param
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomCol().fill('#aa00bb').style { row, s ->
          s.fill = '#cc00dd'
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)

    assertTrue(xml.contains('class="charm-bar"'), 'Should render bar')
    assertTrue(xml.contains('fill="#cc00dd"'), 'Style callback should override layer param')
    assertFalse(xml.contains('fill="#aa00bb"'), 'Layer param fill should not appear when overridden')
  }

  @Test
  void testCallbackIsEvaluatedOncePerDatum() {
    def data = Matrix.builder().data(
        x: ['A', 'B'],
        y: [10, 20]
    ).types(String, Integer).build()

    int callCount = 0
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomCol().style { row, s ->
          callCount++
          s.fill = '#counted'
        }
      }
    }.build()

    chart.render()

    // Should be called exactly once per datum (2 data rows)
    assertEquals(2, callCount, 'Style callback should be called once per datum')
  }

  private static int countOccurrences(String text, String token) {
    int count = 0
    int idx = 0
    while (true) {
      idx = text.indexOf(token, idx)
      if (idx < 0) {
        break
      }
      count++
      idx += token.length()
    }
    count
  }
}
