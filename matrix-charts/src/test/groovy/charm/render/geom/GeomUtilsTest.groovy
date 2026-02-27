package charm.render.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

/**
 * Tests that layer params take priority over mapped aesthetics in GeomUtils resolve methods.
 */
class GeomUtilsTest {

  @Test
  void testLayerFillParamOverridesMappedFill() {
    def data = Matrix.builder().data(
        category: ['A', 'B', 'C'],
        value: [10, 20, 30],
        status: ['good', 'bad', 'good']
    ).types(String, Integer, String).build()

    // Map fill to 'status' column but also set explicit fill on the layer
    def chart = plot(data) {
      mapping { x = 'category'; y = 'value'; fill = 'status' }
      layers { geomBar().fill('#ff0000') }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)

    // All bars should use the explicit layer fill
    assertTrue(xml.contains('class="charm-bar"'), 'Should render bars')
    assertTrue(xml.contains('fill="#ff0000"'), 'Layer param fill should be present')

    // Count occurrences of charm-bar and fill="#ff0000" — every bar should have the explicit fill
    int barCount = countOccurrences(xml, 'class="charm-bar"')
    int fillCount = countOccurrences(xml, 'fill="#ff0000"')
    assertTrue(barCount > 0, 'Should render at least one bar')
    assertTrue(fillCount >= barCount, 'Every bar should use the layer param fill')
  }

  @Test
  void testLayerColorParamOverridesMappedColor() {
    def data = Matrix.builder().data(
        category: ['A', 'B', 'C'],
        value: [10, 20, 30],
        cls: ['x', 'y', 'z']
    ).types(String, Integer, String).build()

    // Map color to 'cls' column but also set explicit color on the layer
    def chart = plot(data) {
      mapping { x = 'category'; y = 'value'; color = 'cls' }
      layers { geomBar().color('#00ff00') }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)

    assertTrue(xml.contains('class="charm-bar"'), 'Should render bars')
    // All bars should use the explicit layer stroke color
    int barCount = countOccurrences(xml, 'class="charm-bar"')
    int strokeCount = countOccurrences(xml, 'stroke="#00ff00"')
    assertTrue(barCount > 0, 'Should render at least one bar')
    assertTrue(strokeCount >= barCount, 'Every bar should use the layer param color as stroke')
  }

  @Test
  void testMappedAestheticsWorkWithoutLayerParams() {
    def data = Matrix.builder().data(
        category: ['A', 'B'],
        value: [10, 20],
        status: ['good', 'bad']
    ).types(String, Integer, String).build()

    // Map fill to 'status' without setting a layer param — uses geomCol() (stat=IDENTITY)
    def chart = plot(data) {
      mapping { x = 'category'; y = 'value'; fill = 'status' }
      layers { geomCol() }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)

    assertTrue(xml.contains('class="charm-bar"'), 'Should render bars')
    int barCount = countOccurrences(xml, 'class="charm-bar"')
    assertEquals(2, barCount, 'Should render exactly 2 bars')

    // Extract fill values from bar rects — each bar should have a fill from the scale
    // (they shouldn't all be the same since they map to different 'status' values)
    List<String> fills = extractBarFills(xml)
    assertTrue(fills.size() == 2, 'Should find 2 bar fill values')
    assertNotEquals(fills[0], fills[1], 'Mapped aesthetic should produce distinct fills for distinct values')
  }

  /**
   * Extracts fill attribute values from charm-bar rect elements.
   */
  private static List<String> extractBarFills(String xml) {
    List<String> fills = []
    // Find each rect with class="charm-bar" and extract its fill
    int idx = 0
    while (true) {
      idx = xml.indexOf('class="charm-bar"', idx)
      if (idx < 0) {
        break
      }
      // Look backwards to find the enclosing <rect tag
      int rectStart = xml.lastIndexOf('<rect', idx)
      if (rectStart >= 0) {
        String rectTag = xml.substring(rectStart, idx + 'class="charm-bar"'.length() + 10)
        def matcher = rectTag =~ /fill="([^"]+)"/
        if (matcher.find()) {
          fills << matcher.group(1)
        }
      }
      idx += 'class="charm-bar"'.length()
    }
    fills
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
