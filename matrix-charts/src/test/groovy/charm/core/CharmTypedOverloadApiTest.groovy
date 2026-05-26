package charm.core

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.Coord
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.ReverseScaleTransform
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.ScaleTransform
import se.alipsa.matrix.charm.ScaleTransforms
import se.alipsa.matrix.charm.ScaleType

/**
 * Verifies callers can use typed scale and coord overloads.
 */
class CharmTypedOverloadApiTest {

  @Test
  void testScaleTransformTypedOverloads() {
    Scale scale = Scale.continuous()
    scale.transform = 'log10'

    assertEquals(ScaleType.TRANSFORM, scale.type)
    assertEquals('log10', scale.transform)

    ScaleTransform reverse = new ReverseScaleTransform()
    scale.transform = reverse

    assertEquals(ScaleType.TRANSFORM, scale.type)
    assertEquals('reverse', scale.transform)

    scale.transform = (Void) null

    assertEquals(ScaleType.CONTINUOUS, scale.type)
    assertNull(scale.transformStrategy)
    assertEquals('sqrt', ScaleTransforms.resolve('sqrt').id())
    assertEquals(reverse, ScaleTransforms.resolve(reverse))
    assertNull(ScaleTransforms.resolve((Void) null))
  }

  @Test
  void testManualScaleTypedOverloads() {
    Map<String, String> namedValues = [A: '#ff0000', B: '#00ff00']
    List<String> values = ['#ff0000', '#00ff00']

    Scale named = Scale.manual(namedValues)
    Scale ordered = Scale.manual(values)

    assertEquals('manual', named.params['colorType'])
    assertEquals(namedValues, named.params['namedValues'])
    assertFalse(named.params.containsKey('values'))

    assertEquals('manual', ordered.params['colorType'])
    assertEquals(values, ordered.params['values'])
    assertFalse(ordered.params.containsKey('namedValues'))
  }

  @Test
  void testScaleGuideTypedOverloads() {
    GuideSpec legend = GuideSpec.legend([title: 'Class'])
    Scale bySpec = Scale.continuous().guide(legend)
    Scale byType = Scale.continuous().guide(GuideType.COLORBAR)
    Scale byString = Scale.continuous().guide('none')
    Scale byBoolean = Scale.continuous().guide(false)

    assertEquals(GuideType.LEGEND, (bySpec.params['guide'] as GuideSpec).type)
    assertEquals('Class', ((bySpec.params['guide'] as GuideSpec).params['title']))
    assertEquals(GuideType.COLORBAR, (byType.params['guide'] as GuideSpec).type)
    assertEquals(GuideType.NONE, (byString.params['guide'] as GuideSpec).type)
    assertEquals(GuideType.NONE, (byBoolean.params['guide'] as GuideSpec).type)

    bySpec.guide((Void) null)
    assertFalse(bySpec.params.containsKey('guide'))
  }

  @Test
  void testCoordTypedOverloads() {
    Coord coord = new Coord()
    coord.type = CharmCoordType.POLAR
    assertEquals(CharmCoordType.POLAR, coord.type)

    coord.type = 'flip'
    assertEquals(CharmCoordType.FLIP, coord.type)

    coord.setType((Void) null)
    assertEquals(CharmCoordType.CARTESIAN, coord.type)

    CoordSpec spec = new CoordSpec()
        .type(CharmCoordType.FIXED)
        .type('polar')
        .type((Void) null)
    spec.ratio = 1.5
    spec.start = 0

    assertEquals(CharmCoordType.CARTESIAN, spec.type)
    assertEquals(0, spec.ratio.compareTo(1.5 as BigDecimal))
    assertEquals(0, spec.start.compareTo(0 as BigDecimal))
  }

}
