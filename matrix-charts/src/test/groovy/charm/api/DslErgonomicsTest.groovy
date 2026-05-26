package charm.api

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.charm.geom.Geoms.geomLine
import static se.alipsa.matrix.charm.geom.Geoms.geomPoint

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.LabelsSpec
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.theme.CharmThemes
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

  // ---- Phase 7: + operator composition ----

  @Test
  void testPlusLayerBuilder() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
    }
    spec = spec + geomPoint().size(3)
    Chart chart = spec.build()
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.POINT, chart.layers[0].geomType)
  }

  @Test
  void testPlusMultipleLayers() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
    }
    spec = spec + geomPoint() + geomLine()
    Chart chart = spec.build()
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.POINT, chart.layers[0].geomType)
    assertEquals(CharmGeomType.LINE, chart.layers[1].geomType)
  }

  @Test
  void testPlusTheme() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
    }
    spec = spec + CharmThemes.dark()
    Chart chart = spec.build()
    assertNotNull(chart.theme)
    assertEquals('dark', chart.theme.themeName)
  }

  @Test
  void testPlusLabels() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
    }
    LabelsSpec lbl = new LabelsSpec()
    lbl.title = 'MPG Plot'
    lbl.x = 'City'
    lbl.y = 'Highway'
    spec = spec + lbl
    Chart chart = spec.build()
    assertEquals('MPG Plot', chart.labels.title)
    assertEquals('City', chart.labels.x)
    assertEquals('Highway', chart.labels.y)
  }

  @Test
  void testPlusCoordSpec() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
      layers { geomPoint() }
    }
    CoordSpec flip = new CoordSpec()
    flip.type = CharmCoordType.FLIP
    spec = spec + flip
    Chart chart = spec.build()
    assertEquals(CharmCoordType.FLIP, chart.coord.type)
  }

  @Test
  void testPlusChainCombination() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping { x = 'cty'; y = 'hwy' }
    }
    spec = spec + geomPoint().size(2) + CharmThemes.minimal()
    Chart chart = spec.build()
    assertEquals(1, chart.layers.size())
    assertEquals('minimal', chart.theme.themeName)
  }

}
