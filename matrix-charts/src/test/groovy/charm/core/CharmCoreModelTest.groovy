package charm.core

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmMappingException
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Cols
import se.alipsa.matrix.charm.CustomScaleTransform
import se.alipsa.matrix.charm.Log10ScaleTransform
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.ReverseScaleTransform
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.ScaleType
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.geom.PointBuilder
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
    spec.mapping(x: 'cty', y: col['hwy'], color: 'class')

    assertEquals('cty', spec.mapping.x.columnName())
    assertEquals('hwy', spec.mapping.y.columnName())
    assertEquals('class', spec.mapping.color.columnName())
  }

  @Test
  void testUnsupportedMappingTypeThrowsClearError() {
    PlotSpec spec = plot(Dataset.mpg())
    CharmMappingException e = assertThrows(CharmMappingException.class) {
      spec.mapping(x: 123)
    }
    assertTrue(e.message.contains("Unsupported mapping for 'x'"))
    assertTrue(e.message.contains('ColumnExpr and CharSequence'))
  }

  @Test
  void testClosureAssignmentRuleRequiresEqualsToApplyMappings() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x: 'cty'
        y: 'hwy'
      }
      layers { geomPoint() }
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('missing required mappings'))
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
      mapping {
        x = 'does_not_exist'
        y = 'hwy'
      }
      layers { geomPoint() }
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains("Unknown column 'does_not_exist'"))
  }

  @Test
  void testMissingRequiredAesFailsForPointLayer() {
    PlotSpec spec = plot(Dataset.mpg()) {
      layers { geomPoint() }
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('missing required mappings'))
    assertTrue(e.message.contains('POINT'))
  }

  @Test
  void testInheritMappingFalseRequiresLayerMapping() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      layers { geomPoint().inheritMapping(false) }
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('without inherited plot mappings'))
  }

  @Test
  void testLayerMappingCanOverrideWhenNotInheriting() {
    Chart chart = plot(Dataset.mpg()) {
      mapping {
        x = 'displ'
        y = 'hwy'
      }
      layers { geomPoint().inheritMapping(false).mapping(x: 'cty', y: 'hwy') }
    }.build()

    assertEquals(1, chart.layers.size())
    assertNotNull(chart.layers.first().mapping)
    assertEquals('cty', chart.layers.first().mapping.x.columnName())
  }

  @Test
  void testSmoothAndTileDslMapToExpectedLayerModel() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      layers {
        geomSmooth().method('lm')
        geomTile().alpha(0.3)
      }
    }

    assertEquals(2, spec.layers.size())
    assertEquals(CharmGeomType.SMOOTH, spec.layers[0].geomType)
    assertEquals(CharmStatType.SMOOTH, spec.layers[0].statType)
    assertEquals('lm', spec.layers[0].params.method)
    assertEquals(CharmGeomType.TILE, spec.layers[1].geomType)
    assertEquals(0.3, spec.layers[1].params.alpha)
  }

  @Test
  void testUnsupportedGeomStatCombinationFailsValidation() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      // Directly construct an invalid LayerSpec to test validation
      layers << new LayerSpec(
          GeomSpec.of(CharmGeomType.SMOOTH),
          StatSpec.of(CharmStatType.IDENTITY),
          null, true,
          PositionSpec.of(CharmPositionType.IDENTITY),
          [method: 'lm']
      )
    }

    CharmValidationException e = assertThrows(CharmValidationException.class) {
      spec.build()
    }
    assertTrue(e.message.contains('geom SMOOTH requires stat SMOOTH'))
  }

  @Test
  void testLayerMapInheritMappingParsesFalseString() {
    Chart chart = plot(Dataset.mpg()) {
      addLayer(new PointBuilder()
          .inheritMapping(false)
          .mapping(x: 'cty', y: 'hwy')
          .size(2))
    }.build()

    assertEquals(1, chart.layers.size())
    assertEquals(false, chart.layers.first().inheritMapping)
  }

  @Test
  void testLayerDslPositionParsesStringValue() {
    Chart chart = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      layers { geomPoint().position('stack') }
    }.build()

    assertEquals(CharmPositionType.STACK, chart.layers.first().positionType)
  }

  @Test
  void testLayerDslPositionRejectsInvalidStringValue() {
    CharmValidationException e = assertThrows(CharmValidationException.class) {
      plot(Dataset.mpg()) {
        mapping {
          x = 'cty'
          y = 'hwy'
        }
        layers { geomPoint().position('bogus-position') }
      }
    }
    assertTrue(e.message.contains("Unsupported layer position 'bogus-position'"))
  }

  @Test
  void testScaleTransformsUseStrategyObjects() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      layers { geomPoint() }
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
          rows = ['year']
          wrap {
            vars = ['drv']
          }
        }
      }
    }
    assertTrue(e.message.contains('Cannot combine wrap{} with rows/cols'))
  }

  @Test
  void testFacetGridAfterWrapConflictThrowsValidationError() {
    CharmValidationException e = assertThrows(CharmValidationException.class) {
      plot(Dataset.mpg()) {
        facet {
          wrap {
            vars = ['drv']
          }
          rows = ['year']
        }
      }
    }
    assertTrue(e.message.contains('Cannot set rows/cols after wrap{}'))
  }

  @Test
  void testBuildReturnsImmutableChartState() {
    Matrix data = Dataset.mpg()
    PlotSpec spec = plot(data) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      layers { geomPoint().size(2) }
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

  @Test
  void testLayerMappingGetterReturnsDefensiveCopy() {
    Chart chart = plot(Dataset.mpg()) {
      layers { geomPoint().mapping(x: 'cty', y: 'hwy') }
    }.build()

    def firstRead = chart.layers.first().mapping
    firstRead.x = 'displ'

    def secondRead = chart.layers.first().mapping
    assertEquals('cty', secondRead.x.columnName())
  }

  @Test
  void testCompiledChartDeepFreezesNestedLayerParams() {
    Map<String, Object> externalPayload = [
        nested: [kind: 'point'],
        values: [1, 2]
    ]
    Set<String> tags = ['a', 'b'] as Set<String>

    Chart chart = plot(Dataset.mpg()) {
      addLayer(new PointBuilder()
          .mapping(x: 'cty', y: 'hwy')
          .param('payload', externalPayload)
          .param('tags', tags))
    }.build()

    externalPayload.nested.kind = 'changed'
    (externalPayload.values as List) << 3
    tags << 'c'

    Map<String, Object> payload = chart.layers.first().params.payload as Map<String, Object>
    assertEquals('point', (payload.nested as Map).kind)
    assertEquals(2, (payload.values as List).size())
    assertEquals(2, (chart.layers.first().params.tags as Set).size())

    assertThrows(UnsupportedOperationException.class) {
      (payload.nested as Map).put('x', 1)
    }
    assertThrows(UnsupportedOperationException.class) {
      (payload.values as List) << 99
    }
    assertThrows(UnsupportedOperationException.class) {
      (chart.layers.first().params.tags as Set) << 'z'
    }
  }
}
