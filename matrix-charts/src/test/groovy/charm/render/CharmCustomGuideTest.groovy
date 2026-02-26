package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.GuidesSpec
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

class CharmCustomGuideTest {

  @Test
  void testCustomGuideClosureReceivesContext() {
    boolean closureCalled = false
    Map receivedContext = null

    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    PlotSpec spec = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      points {}
    }
    spec.guides.setSpec('custom', GuideSpec.custom({ ctx ->
      closureCalled = true
      receivedContext = ctx
    }))
    Chart built = spec.build()

    Svg svg = built.render()
    assertTrue(closureCalled, 'Custom guide closure should be called')
    assertNotNull(receivedContext, 'Context should not be null')
    assertNotNull(receivedContext.svg, 'Context should contain svg')
    assertNotNull(receivedContext.theme, 'Context should contain theme')
    assertTrue(receivedContext.x instanceof Number, 'Context should contain x position')
    assertTrue(receivedContext.y instanceof Number, 'Context should contain y position')
    assertTrue(receivedContext.width instanceof Number, 'Context should contain width')
    assertTrue(receivedContext.height instanceof Number, 'Context should contain height')
    assertNotNull(receivedContext.scales, 'Context should contain scales')
  }

  @Test
  void testCustomGuideErrorHandling() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    PlotSpec spec = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      points {}
    }
    spec.guides.setSpec('custom', GuideSpec.custom({ ctx ->
      throw new RuntimeException("Test error")
    }))
    Chart built = spec.build()

    // Should not throw - error is caught and rendered as placeholder
    Svg svg = built.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('charm-legend-error'), 'Error placeholder should be rendered')
  }

  @Test
  void testCustomGuideWithTitle() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    PlotSpec spec = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      points {}
    }
    spec.guides.setSpec('custom', GuideSpec.custom({ ctx ->
      ctx.svg.addRect(20, 20).x(ctx.x).y(ctx.y).fill('blue')
    }, [title: 'My Custom Guide', width: 30, height: 30] as Map<String, Object>))
    Chart built = spec.build()

    Svg svg = built.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('My Custom Guide'), 'Custom guide title should be rendered')
    assertTrue(content.contains('id="custom-guide-custom"'), 'Custom guide group should have id')
  }
}
