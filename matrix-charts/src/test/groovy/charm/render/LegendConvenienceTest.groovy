package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.datasets.Dataset

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static se.alipsa.matrix.charm.Charts.plot

class LegendConvenienceTest {

  @Test
  void testScaleDslColorConvenienceMethods() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      layers { geomPoint() }
      scale {
        color = colorBrewer('Set1')
        fill = colorGradient('#ffffff', '#ff0000')
      }
    }

    assertEquals('brewer', spec.scale.color.params['colorType'])
    assertEquals('Set1', spec.scale.color.params['palette'])
    assertEquals('gradient', spec.scale.fill.params['colorType'])
    assertEquals('#ffffff', spec.scale.fill.params['low'])
    assertEquals('#ff0000', spec.scale.fill.params['high'])
  }

  @Test
  void testScaleGuideMaterializesWithoutGuidesBlock() {
    Chart chart = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
        color = 'class'
      }
      layers { geomPoint() }
      scale {
        color = colorBrewer('Set1').guide(none())
      }
    }.build()

    assertNotNull(chart.guides.getSpec('color'))
    assertEquals(GuideType.NONE, chart.guides.getSpec('color').type)
  }

  @Test
  void testScaleGuideMaterializesColorbarWithParams() {
    Chart chart = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
        color = 'displ'
      }
      layers { geomPoint() }
      scale {
        color = colorGradient('#132B43', '#56B1F7').guide(colorbar(title: 'Engine'))
      }
    }.build()

    assertNotNull(chart.guides.getSpec('color'))
    assertEquals(GuideType.COLORBAR, chart.guides.getSpec('color').type)
    assertEquals('Engine', chart.guides.getSpec('color').params['title'])
  }

  @Test
  void testExplicitGuidesOverrideScaleAttachedGuides() {
    Chart chart = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
        color = 'class'
      }
      layers { geomPoint() }
      scale {
        color = colorBrewer('Set1').guide(none())
      }
      guides {
        color = legend()
      }
    }.build()

    assertEquals(GuideType.LEGEND, chart.guides.getSpec('color').type)
  }

  @Test
  void testScaleGuideFluentMethodNormalizesInput() {
    Scale guideByString = Scale.continuous().guide('none')
    Scale guideByBoolean = Scale.continuous().guide(false)

    assertEquals(GuideType.NONE, (guideByString.params['guide'] as GuideSpec).type)
    assertEquals(GuideType.NONE, (guideByBoolean.params['guide'] as GuideSpec).type)
  }

  @Test
  void testLegendPositionShorthandSetsThemeLegendPosition() {
    PlotSpec spec = plot(Dataset.mpg())
    spec.legendPosition('bottom')
    assertEquals('bottom', spec.theme.legendPosition)
  }

  @Test
  void testLegendPositionLastWriteWinsAgainstThemeDsl() {
    PlotSpec firstThemeThenShorthand = plot(Dataset.mpg()) {
      theme { legendPosition = 'left' }
      legendPosition('top')
    }
    assertEquals('top', firstThemeThenShorthand.theme.legendPosition)

    PlotSpec firstShorthandThenTheme = plot(Dataset.mpg()) {
      legendPosition('top')
      theme { legendPosition = 'left' }
    }
    assertEquals('left', firstShorthandThenTheme.theme.legendPosition)
  }
}
