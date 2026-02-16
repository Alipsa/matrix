package charm.render

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Assertions
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.gg.GgChart

/**
 * Shared rendering and assertion helpers for charm/gg parity tests.
 */
@CompileStatic
class CharmRenderTestUtil {

  private static final Map<String, Class<?>> PRIMITIVE_TYPES = [
      circle: Circle,
      line  : Line,
      rect  : Rect,
      path  : Path,
      text  : Text
  ].asImmutable()

  /**
   * Render a compiled charm chart using explicit dimensions.
   *
   * @param chart the compiled charm chart
   * @param width svg width
   * @param height svg height
   * @return rendered svg
   */
  static Svg renderCharm(Chart chart, int width = 800, int height = 600) {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    RenderConfig config = new RenderConfig(width: width, height: height)
    new CharmRenderer().render(chart, config)
  }

  /**
   * Build and render a charm plot specification.
   *
   * @param plotSpec mutable charm plot spec
   * @param width svg width
   * @param height svg height
   * @return rendered svg
   */
  static Svg renderCharm(PlotSpec plotSpec, int width = 800, int height = 600) {
    if (plotSpec == null) {
      throw new IllegalArgumentException('plotSpec cannot be null')
    }
    renderCharm(plotSpec.build(), width, height)
  }

  /**
   * Render a gg chart with explicit dimensions.
   *
   * @param chart gg chart
   * @param width svg width
   * @param height svg height
   * @return rendered svg
   */
  static Svg renderGg(GgChart chart, int width = 800, int height = 600) {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    int originalWidth = chart.width
    int originalHeight = chart.height
    try {
      chart.width = width
      chart.height = height
      chart.render()
    } finally {
      chart.width = originalWidth
      chart.height = originalHeight
    }
  }

  /**
   * Count primitive SVG element occurrences.
   *
   * @param svg rendered svg
   * @return map with primitive counts
   */
  static Map<String, Integer> primitiveCounts(Svg svg) {
    if (svg == null) {
      throw new IllegalArgumentException('svg cannot be null')
    }
    List elements = svg.descendants()
    Map<String, Integer> counts = [:]
    PRIMITIVE_TYPES.each { String name, Class<?> type ->
      counts[name] = elements.count { Object e -> type.isInstance(e) } as int
    }
    counts.clipPath = elements.count { Object e -> e.class.simpleName == 'ClipPath' } as int
    counts.asImmutable()
  }

  /**
   * Assert that an SVG contains an exact count of an element type.
   *
   * @param svg rendered svg
   * @param elementType element class
   * @param expected expected count
   * @param message optional assertion message
   */
  static void assertElementCount(Svg svg, Class<?> elementType, int expected, String message = null) {
    int actual = elementCount(svg, elementType)
    String assertionMessage = message ?: "Expected ${expected} ${elementType.simpleName} element(s), got ${actual}"
    Assertions.assertEquals(expected, actual, assertionMessage)
  }

  /**
   * Assert that an SVG contains at least the expected count of an element type.
   *
   * @param svg rendered svg
   * @param elementType element class
   * @param minimum minimum expected count
   * @param message optional assertion message
   */
  static void assertElementCountAtLeast(Svg svg, Class<?> elementType, int minimum, String message = null) {
    int actual = elementCount(svg, elementType)
    String assertionMessage = message ?: "Expected at least ${minimum} ${elementType.simpleName} element(s), got ${actual}"
    Assertions.assertTrue(actual >= minimum, assertionMessage)
  }

  /**
   * Assert that rendered SVG XML contains the provided text.
   *
   * @param svg rendered svg
   * @param text expected text fragment
   */
  static void assertSvgContainsText(Svg svg, String text) {
    String xml = SvgWriter.toXml(svg)
    Assertions.assertTrue(xml.contains(text), "Expected SVG to contain text '${text}'")
  }

  /**
   * Assert that rendered SVG XML contains a specific attribute value.
   *
   * @param svg rendered svg
   * @param attribute attribute name
   * @param expectedValue expected value
   */
  static void assertSvgContainsAttribute(Svg svg, String attribute, String expectedValue) {
    String xml = SvgWriter.toXml(svg)
    String expectedFragment = "${attribute}=\"${expectedValue}\""
    Assertions.assertTrue(xml.contains(expectedFragment), "Expected SVG to contain attribute fragment '${expectedFragment}'")
  }

  /**
   * Compare primitive counts between charm and gg rendering and assert tolerance bounds.
   *
   * @param charmSvg svg rendered from charm
   * @param ggSvg svg rendered from gg
   * @param tolerances per-primitive max absolute difference
   */
  static void assertPrimitiveParityWithinTolerance(
      Svg charmSvg,
      Svg ggSvg,
      Map<String, Integer> tolerances = [:]
  ) {
    Map<String, Integer> charmCounts = primitiveCounts(charmSvg)
    Map<String, Integer> ggCounts = primitiveCounts(ggSvg)
    Set<String> keys = (charmCounts.keySet() + ggCounts.keySet()) as Set<String>
    keys.each { String key ->
      int tolerance = tolerances.getOrDefault(key, 0)
      int charmValue = charmCounts.getOrDefault(key, 0)
      int ggValue = ggCounts.getOrDefault(key, 0)
      int delta = Math.abs(charmValue - ggValue)
      Assertions.assertTrue(
          delta <= tolerance,
          "Primitive '${key}' differs too much: charm=${charmValue}, gg=${ggValue}, tolerance=${tolerance}"
      )
    }
  }

  /**
   * Parse a simple CSV resource (comma-separated, no escaping/quoting rules).
   *
   * @param resourcePath classpath resource path
   * @return parsed rows as list of string lists
   */
  static List<List<String>> loadSimpleCsvResource(String resourcePath) {
    InputStream in = CharmRenderTestUtil.class.classLoader.getResourceAsStream(resourcePath)
    if (in == null) {
      throw new IllegalArgumentException("Resource not found: ${resourcePath}")
    }
    String text = ''
    in.withCloseable { InputStream stream ->
      text = stream.getText('UTF-8')
    }
    text = text.trim()
    if (text.isEmpty()) {
      return []
    }
    text.readLines()
        .findAll { String line -> line != null && !line.trim().isEmpty() && !line.trim().startsWith('#') }
        .collect { String line -> line.split(',', -1).collect { String token -> token.trim() } as List<String> }
  }

  private static int elementCount(Svg svg, Class<?> elementType) {
    if (svg == null) {
      throw new IllegalArgumentException('svg cannot be null')
    }
    if (elementType == null) {
      throw new IllegalArgumentException('elementType cannot be null')
    }
    List elements = svg.descendants()
    elements.count { Object e -> elementType.isInstance(e) } as int
  }
}
