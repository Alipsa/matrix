package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GuideCustomTest {

  @Test
  void testGuideCustomFactory() {
    def closure = { context -> }
    def guide = guide_custom(closure)
    assertEquals('custom', guide.type)
    assertNotNull(guide.params)
    assertEquals(closure, guide.params.renderClosure)
  }

  @Test
  void testGuideCustomRequiresClosure() {
    // Should throw IllegalArgumentException when renderClosure is null
    def exception = assertThrows(IllegalArgumentException.class) {
      guide_custom(null)
    }
    assertTrue(exception.message.contains("guide_custom requires a renderClosure parameter"))
  }

  @Test
  void testGuideCustomWithParameters() {
    def closure = { context -> }
    def guide = guide_custom(closure, [width: 100, height: 80, title: 'My Guide'])
    assertEquals('custom', guide.type)
    assertEquals(closure, guide.params.renderClosure)
    assertEquals(100, guide.params.width)
    assertEquals(80, guide.params.height)
    assertEquals('My Guide', guide.params.title)
  }

  @Test
  void testGuideCustomWithSimpleClosure() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def customGuide = guide_custom({ context ->
      // Simple custom rendering - add a rectangle and text
      context.svg.addRect(30, 20)
                 .x(context.x)
                 .y(context.y)
                 .fill('red')
                 .stroke('black')

      context.svg.addText('Custom')
                 .x(context.x + 5)
                 .y(context.y + 15)
                 .fontSize(10)
    }, [width: 40, height: 30])

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: customGuide)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('Custom'))
    assertTrue(content.contains('fill="red"'))
    assertTrue(content.contains('id="custom-guide-custom"'))
  }

  @Test
  void testGuideCustomClosureReceivesContext() {
    boolean closureCalled = false
    Map receivedContext = null

    def customGuide = guide_custom({ context ->
      closureCalled = true
      receivedContext = context
      null
    })

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: customGuide)

    chart.render()

    assertTrue(closureCalled)
    assertNotNull(receivedContext)
    assertNotNull(receivedContext.svg)
    assertNotNull(receivedContext.theme)
    assertTrue(receivedContext.x instanceof Number)
    assertTrue(receivedContext.y instanceof Number)
    assertTrue(receivedContext.width instanceof Number)
    assertTrue(receivedContext.height instanceof Number)
    assertNotNull(receivedContext.scales)
  }

  @Test
  void testGuideCustomWithTitle() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2]])
        .types(Integer, Integer)
        .build()

    def customGuide = guide_custom({ context ->
      context.svg.addCircle()
                 .cx(context.x + 15)
                 .cy(context.y + 15)
                 .r(10)
                 .fill('blue')
    }, [width: 30, height: 30, title: 'Custom Legend'])

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: customGuide)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('Custom Legend'))
    assertTrue(content.contains('fill="blue"'))
  }

  @Test
  void testGuideCustomMultipleShapes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def customGuide = guide_custom({ context ->
      // Draw multiple shapes
      def x = context.x
      def y = context.y

      // Rectangle
      context.svg.addRect(20, 10)
                 .x(x)
                 .y(y)
                 .fill('yellow')

      // Circle
      context.svg.addCircle()
                 .cx(x + 10)
                 .cy(y + 20)
                 .r(5)
                 .fill('green')

      // Line
      context.svg.addLine(x, y + 30, x + 20, y + 30)
                 .stroke('purple')
                 .strokeWidth(2)

      // Text
      context.svg.addText('Mix')
                 .x(x + 5)
                 .y(y + 45)
                 .fontSize(8)
    }, [width: 25, height: 50])

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        guides(custom: customGuide)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('fill="yellow"'))
    assertTrue(content.contains('fill="green"'))
    assertTrue(content.contains('stroke="purple"'))
    assertTrue(content.contains('Mix'))
  }

  @Test
  void testGuideCustomAccessToScales() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'color')
        .rows([[1, 2, 'A'], [2, 3, 'B'], [3, 4, 'C']])
        .types(Integer, Integer, String)
        .build()

    def customGuide = guide_custom({ context ->
      // Access color scale from context
      def colorScale = context.scales['color']
      if (colorScale) {
        context.svg.addText("Scale: ${colorScale.name ?: 'unnamed'}")
                   .x(context.x)
                   .y(context.y + 10)
                   .fontSize(8)
      }
    }, [width: 60, height: 20])

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'color')) +
        geom_point() +
        guides(custom: customGuide)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    // Should render successfully
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideCustomWithDifferentDimensions() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2]])
        .types(Integer, Integer)
        .build()

    // Small guide
    def smallGuide = guide_custom({ context ->
      context.svg.addRect(context.width, context.height)
                 .x(context.x)
                 .y(context.y)
                 .fill('lightblue')
    }, [width: 20, height: 20])

    def chartSmall = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: smallGuide)

    Svg svgSmall = chartSmall.render()
    String contentSmall = SvgWriter.toXml(svgSmall)

    // Large guide
    def largeGuide = guide_custom({ context ->
      context.svg.addRect(context.width, context.height)
                 .x(context.x)
                 .y(context.y)
                 .fill('lightgreen')
    }, [width: 100, height: 100])

    def chartLarge = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: largeGuide)

    Svg svgLarge = chartLarge.render()
    String contentLarge = SvgWriter.toXml(svgLarge)

    // Both should render successfully
    assertTrue(contentSmall.contains('fill="lightblue"'))
    assertTrue(contentLarge.contains('fill="lightgreen"'))
  }

  @Test
  void testGuideCustomErrorHandling() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2]])
        .types(Integer, Integer)
        .build()

    // Closure that throws an error
    def brokenGuide = guide_custom({ context ->
      throw new RuntimeException("Intentional error")
    }, [width: 30, height: 30])

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: brokenGuide)

    def orgErr = System.err
    System.err = new PrintStream(new ByteArrayOutputStream()) // Suppress error output
    try {
      // Should not throw - error should be caught
      Svg svg = chart.render()
      String content = SvgWriter.toXml(svg)

      // Should contain error placeholder
      assertTrue(content.contains('Error rendering custom guide'))
      assertTrue(content.contains('fill="#ffcccc"'))
    } finally {
      System.err = orgErr
    }
  }

  @Test
  void testGuideCustomWithMultipleCustomGuides() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2]])
        .types(Integer, Integer)
        .build()

    def guide1 = guide_custom({ context ->
      context.svg.addRect(20, 10)
                 .x(context.x)
                 .y(context.y)
                 .fill('red')
    }, [width: 25, height: 15, title: 'Guide 1'])

    def guide2 = guide_custom({ context ->
      context.svg.addCircle()
                 .cx(context.x + 10)
                 .cy(context.y + 10)
                 .r(8)
                 .fill('blue')
    }, [width: 25, height: 25, title: 'Guide 2'])

    // Note: Using guides() with multiple custom guides
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: guide1, custom2: guide2)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should render both guides
    assertTrue(content.contains('Guide 1'))
    assertTrue(content.contains('Guide 2'))
    assertTrue(content.contains('fill="red"'))
    assertTrue(content.contains('fill="blue"'))
  }

  @Test
  void testGuideCustomReturningNull() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2]])
        .types(Integer, Integer)
        .build()

    // Closure that returns null (elements already added)
    def customGuide = guide_custom({ context ->
      context.svg.addText('Direct addition')
                 .x(context.x)
                 .y(context.y + 10)
      return null
    })

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: customGuide)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('Direct addition'))
  }

  @Test
  void testGuideCustomIntegrationWithOtherGuides() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'color', 'size')
        .rows([[1, 2, 'A', 5], [2, 3, 'B', 10]])
        .types(Integer, Integer, String, Integer)
        .build()

    def customGuide = guide_custom({ context ->
      context.svg.addText('Info')
                 .x(context.x)
                 .y(context.y + 10)
                 .fontSize(9)
    }, [width: 30, height: 20, title: 'Info'])

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'color', size: 'size')) +
        geom_point() +
        guides(color: guide_legend(), size: guide_legend(), custom: customGuide)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should have legends and custom guide
    assertTrue(content.contains('id="legend"'))
    assertTrue(content.contains('Info'))
    assertTrue(content.contains('<svg'))
  }
}
