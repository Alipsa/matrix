package charm.api

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.datasets.Dataset

class DslErgonomicsTest {

  @Test
  void testThemePresetByName() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
      theme { preset = 'minimal' }
    }
    Chart chart = spec.build()
    assertNotNull(chart)
    assertNotNull(chart.theme)
  }

  @Test
  void testThemePresetViaUse() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
      theme { apply(dark()) }
    }
    Chart chart = spec.build()
    assertNotNull(chart)
    assertNotNull(chart.theme)
  }

  @Test
  void testThemeUnknownPresetThrows() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
    }
    assertThrows(CharmValidationException) {
      spec.theme { preset = 'nonexistent' }
    }
  }

  @Test
  void testCoordFlipShorthand() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
    }
    spec.coordFlip()
    Chart chart = spec.build()
    assertEquals(CharmCoordType.FLIP, chart.coord.type)
  }

  @Test
  void testCoordFlipInsideClosure() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
      coordFlip()
    }
    Chart chart = spec.build()
    assertEquals(CharmCoordType.FLIP, chart.coord.type)
  }

  @Test
  void testCssAttributesClosure() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
      cssAttributes {
        enabled = true
        includeClasses = true
        includeIds = false
        chartIdPrefix = 'mpg'
      }
    }
    Chart chart = spec.build()
    assertNotNull(chart.cssAttributes)
    assertTrue(chart.cssAttributes.enabled)
    assertTrue(chart.cssAttributes.includeClasses)
    assertEquals(false, chart.cssAttributes.includeIds)
    assertEquals('mpg', chart.cssAttributes.chartIdPrefix)
  }

  @Test
  void testCssAttributesDefaultsWhenNotConfigured() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
    }
    Chart chart = spec.build()
    assertNotNull(chart.cssAttributes)
    assertEquals(false, chart.cssAttributes.enabled)
  }

}
