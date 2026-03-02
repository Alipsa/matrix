package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.scale.NewScaleMarker

import java.util.Locale

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for gg compatibility of per-layer scale overrides via new_scale_color/new_scale_fill.
 */
class PerLayerScaleCompatibilityTest {

  @Test
  void testNewScaleColorBetweenTwoLayers() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'A'], [2, 4, 'B'], [3, 6, 'A'],
            [4, 3, 'B'], [5, 5, 'A'], [6, 7, 'B']
        ])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y', color: 'grp')) +
        geom_point() +
        scale_color_manual(values: [A: '#FF0000', B: '#00FF00']) +
        new_scale_color() +
        geom_point() +
        scale_color_manual(values: [A: '#0000FF', B: '#FFFF00'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = normalizeSvg(svg)
    assertContainsColor(content, '#FF0000')
    assertContainsColor(content, '#00FF00')
    assertContainsColor(content, '#0000FF')
    assertContainsColor(content, '#FFFF00')

    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() >= 12, "Expected at least 12 circles (6 per layer), got ${circles.size()}")
  }

  @Test
  void testNewScaleFillBetweenTwoLayers() {
    Matrix data = Matrix.builder()
        .columnNames('cat', 'value', 'grp')
        .rows([
            ['X', 3, 'A'], ['Y', 7, 'A'],
            ['X', 5, 'B'], ['Y', 4, 'B']
        ])
        .build()

    GgChart chart = ggplot(data, aes(x: 'cat', y: 'value', fill: 'grp')) +
        geom_col() +
        scale_fill_manual(values: [A: '#FF0000', B: '#00FF00']) +
        new_scale_fill() +
        geom_point() +
        scale_fill_manual(values: [A: '#0000FF', B: '#FFFF00'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = normalizeSvg(svg)
    assertContainsColor(content, '#FF0000')
    assertContainsColor(content, '#00FF00')
    assertContainsColor(content, '#0000FF')
    assertContainsColor(content, '#FFFF00')
  }

  @Test
  void testScalesBeforeMarkerRemainGlobal() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'A'], [2, 4, 'B'], [3, 6, 'A']
        ])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y', color: 'grp')) +
        geom_point() +
        scale_color_manual(values: [A: '#FF0000', B: '#00FF00'])

    // No new_scale_color() marker — the scale should be global
    Svg svg = chart.render()
    assertNotNull(svg)

    String content = normalizeSvg(svg)
    assertContainsColor(content, '#FF0000')
    assertContainsColor(content, '#00FF00')

    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() >= 3, "Expected at least 3 circles, got ${circles.size()}")
  }

  @Test
  void testMultipleNewScaleMarkersAcrossThreeLayers() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'A'], [2, 4, 'B'], [3, 6, 'C']
        ])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y', color: 'grp')) +
        geom_point() +
        scale_color_manual(values: [A: '#FF0000', B: '#00FF00', C: '#0000FF']) +
        new_scale_color() +
        geom_point() +
        scale_color_manual(values: [A: '#111111', B: '#222222', C: '#333333']) +
        new_scale_color() +
        geom_point() +
        scale_color_manual(values: [A: '#AAAAAA', B: '#BBBBBB', C: '#CCCCCC'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = normalizeSvg(svg)
    assertContainsColor(content, '#FF0000')
    assertContainsColor(content, '#111111')
    assertContainsColor(content, '#AAAAAA')

    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() >= 9, "Expected at least 9 circles (3 per layer), got ${circles.size()}")
  }

  @Test
  void testNewScaleMarkerCreation() {
    NewScaleMarker colorMarker = new_scale_color()
    assertEquals('color', colorMarker.aesthetic)

    NewScaleMarker colourMarker = new_scale_colour()
    assertEquals('color', colourMarker.aesthetic)

    NewScaleMarker fillMarker = new_scale_fill()
    assertEquals('fill', fillMarker.aesthetic)
  }

  @Test
  void testComponentsListTracksInOrder() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([[1, 2, 'A'], [2, 4, 'B']])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y', color: 'grp')) +
        geom_point() +
        new_scale_color() +
        geom_point()

    // Components should track layers and markers in order
    assertFalse(chart.components.isEmpty(), 'Components list should not be empty')
    assertTrue(chart.components.any { it instanceof NewScaleMarker }, 'Should contain a NewScaleMarker')
  }

  private static String normalizeSvg(Svg svg) {
    SvgWriter.toXml(svg).toLowerCase(Locale.ROOT)
  }

  private static void assertContainsColor(String content, String color) {
    String normalized = color.toLowerCase(Locale.ROOT)
    assertTrue(content.contains(normalized), "Expected rendered svg to contain color ${color}")
  }
}
