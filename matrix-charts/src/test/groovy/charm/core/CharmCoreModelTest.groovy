package charm.core

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.CharmMappingException
import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Cols
import se.alipsa.matrix.charm.CustomScaleTransform
import se.alipsa.matrix.charm.Geom
import se.alipsa.matrix.charm.Log10ScaleTransform
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.ReverseScaleTransform
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.ScaleType
import se.alipsa.matrix.charm.Stat
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

class CharmCoreModelTest {

  @Test
  void testNamedArgMappingCoercionAcceptsStringAndColumnExpr() {
    Cols col = new Cols()
    PlotSpec spec = plot(Dataset.mpg())
    spec.aes(x: 'cty', y: col['hwy'], color: col['class'])

    assertEquals('cty', spec.aes.x.columnName())
    assertEquals('hwy', spec.aes.y.columnName())
    assertEquals('class', spec.aes.color.columnName())
  }

  @Test
  void testUnsupportedMappingTypeThrowsClearError() {
    PlotSpec spec = plot(Dataset.mpg())
    CharmMappingException e = assertThrows(CharmMappingException.class) {
      spec.aes(x: 123)
    }
    assertTrue(e.message.contains("Unsupported mapping for 'x'"))
    assertTrue(e.message.contains('ColumnExpr and CharSequence'))
  }

  @Test
  void testClosureAssignmentRuleRequiresEqualsToApplyMappings() {
    PlotSpec spec = plot(Dataset.mpg()) {
      aes {
        x: col.cty
        y: col.hwy
      }
      points {}
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('missing required aesthetics'))
  }

  @Test
  void testColBracketAndDotAccessAreSupported() {
    Cols col = new Cols()
    assertEquals('class', col['class'].columnName())
    assertEquals('cty', col.cty.columnName())
  }

  @Test
  void testBuildValidationRejectsUnknownColumn() {
    PlotSpec spec = plot(Dataset.mpg()) {
      aes {
        x = col['does_not_exist']
        y = col.hwy
      }
      points {}
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains("Unknown column 'does_not_exist'"))
  }

  @Test
  void testMissingRequiredAesFailsForPointLayer() {
    PlotSpec spec = plot(Dataset.mpg()) {
      points {}
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('missing required aesthetics'))
    assertTrue(e.message.contains('POINT'))
  }

  @Test
  void testInheritAesFalseRequiresLayerAes() {
    PlotSpec spec = plot(Dataset.mpg()) {
      aes {
        x = col.cty
        y = col.hwy
      }
      points {
        inheritAes = false
      }
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('without inherited plot mappings'))
  }

  @Test
  void testLayerAesCanOverrideWhenNotInheriting() {
    Chart chart = plot(Dataset.mpg()) {
      aes {
        x = col.displ
        y = col.hwy
      }
      points {
        inheritAes = false
        aes {
          x = col.cty
          y = col.hwy
        }
      }
    }.build()

    assertEquals(1, chart.layers.size())
    assertNotNull(chart.layers.first().aes)
    assertEquals('cty', chart.layers.first().aes.x.columnName())
  }

  @Test
  void testSmoothAndTileDslMapToExpectedLayerModel() {
    PlotSpec spec = plot(Dataset.mpg()) {
      aes {
        x = col.cty
        y = col.hwy
      }
      smooth {
        method = 'lm'
      }
      tile {
        alpha = 0.3
      }
    }

    assertEquals(2, spec.layers.size())
    assertEquals(Geom.SMOOTH, spec.layers[0].geom)
    assertEquals(Stat.SMOOTH, spec.layers[0].stat)
    assertEquals('lm', spec.layers[0].params.method)
    assertEquals(Geom.TILE, spec.layers[1].geom)
    assertEquals(0.3, spec.layers[1].params.alpha)
  }

  @Test
  void testUnsupportedGeomStatCombinationFailsValidation() {
    PlotSpec spec = plot(Dataset.mpg()) {
      aes {
        x = col.cty
        y = col.hwy
      }
      layer(Geom.SMOOTH, [stat: Stat.IDENTITY, method: 'lm'])
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('geom SMOOTH requires stat SMOOTH'))
  }

  @Test
  void testLayerMapInheritAesParsesFalseString() {
    Chart chart = plot(Dataset.mpg()) {
      layer(Geom.POINT, [
          inheritAes: 'false',
          aes       : [x: 'cty', y: 'hwy'],
          size      : 2
      ])
    }.build()

    assertEquals(1, chart.layers.size())
    assertEquals(false, chart.layers.first().inheritAes)
  }

  @Test
  void testLayerMapInheritAesRejectsInvalidString() {
    CharmValidationException e = assertThrows(CharmValidationException.class) {
      plot(Dataset.mpg()) {
        layer(Geom.POINT, [inheritAes: 'not-a-bool', aes: [x: 'cty', y: 'hwy']])
      }.build()
    }
    assertTrue(e.message.contains("Unsupported boolean value 'not-a-bool'"))
    assertTrue(e.message.contains("layer option 'inheritAes'"))
  }

  @Test
  void testScaleTransformsUseStrategyObjects() {
    PlotSpec spec = plot(Dataset.mpg()) {
      aes {
        x = col.cty
        y = col.hwy
      }
      points {}
      scale {
        x = log10()
        y = reverse()
        color = date()
        fill = custom('times2', { BigDecimal v -> v * 2 }, { BigDecimal v -> v / 2 })
      }
    }

    assertEquals('log10', spec.scale.x.transform)
    assertEquals('reverse', spec.scale.y.transform)
    assertEquals('date', spec.scale.color.transform)
    assertEquals('times2', spec.scale.fill.transform)
    assertTrue(spec.scale.x.transformStrategy instanceof Log10ScaleTransform)
    assertTrue(spec.scale.y.transformStrategy instanceof ReverseScaleTransform)
    assertTrue(spec.scale.fill.transformStrategy instanceof CustomScaleTransform)
  }

  @Test
  void testClearingTransformResetsScaleTypeConsistency() {
    Scale scale = Scale.transform('log10')
    assertEquals(ScaleType.TRANSFORM, scale.type)
    assertNotNull(scale.transformStrategy)

    scale.transform = null
    assertEquals(ScaleType.CONTINUOUS, scale.type)
    assertEquals(null, scale.transformStrategy)

    scale.transformStrategy = new ReverseScaleTransform()
    assertEquals(ScaleType.TRANSFORM, scale.type)
    assertNotNull(scale.transformStrategy)

    scale.transformStrategy = null
    assertEquals(ScaleType.CONTINUOUS, scale.type)
    assertEquals(null, scale.transformStrategy)
  }

  @Test
  void testFacetWrapGridConflictThrowsValidationError() {
    CharmValidationException e = assertThrows(CharmValidationException.class) {
      plot(Dataset.mpg()) {
        facet {
          rows = [col.year]
          wrap {
            vars = [col.drv]
          }
        }
      }
    }
    assertTrue(e.message.contains('Cannot combine wrap{} with rows/cols'))
  }

  @Test
  void testBuildReturnsImmutableChartState() {
    Matrix data = Dataset.mpg()
    PlotSpec spec = plot(data) {
      aes {
        x = col.cty
        y = col.hwy
      }
      points {
        size = 2
      }
      labels {
        title = 'Original'
      }
    }

    Chart chart = spec.build()
    spec.labels {
      title = 'Changed in spec'
    }
    assertEquals('Original', chart.labels.title)

    chart.labels.title = 'Mutated copy'
    assertEquals('Original', chart.labels.title)

    assertThrows(UnsupportedOperationException.class) {
      chart.layers << chart.layers.first()
    }
  }
}
