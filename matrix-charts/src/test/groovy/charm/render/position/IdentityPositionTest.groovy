package charm.render.position

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.position.IdentityPosition

import static org.junit.jupiter.api.Assertions.*

class IdentityPositionTest {

  @Test
  void testPassThrough() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, rowIndex: 0),
        new LayerData(x: 2, y: 20, rowIndex: 1)
    ]
    List<LayerData> result = IdentityPosition.compute(layer, data)
    assertSame(data, result)
  }

  @Test
  void testEmptyData() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = []
    List<LayerData> result = IdentityPosition.compute(layer, data)
    assertSame(data, result)
    assertTrue(result.isEmpty())
  }

  @Test
  void testPreservesAllFields() {
    LayerSpec layer = makeLayer()
    LayerData datum = new LayerData(
        x: 'cat', y: 42, color: 'red', fill: 'blue', rowIndex: 7
    )
    datum.meta.custom = 'value'
    List<LayerData> result = IdentityPosition.compute(layer, [datum])
    assertEquals(1, result.size())
    assertSame(datum, result[0])
    assertEquals('cat', result[0].x)
    assertEquals(42, result[0].y)
    assertEquals('red', result[0].color)
    assertEquals('blue', result[0].fill)
    assertEquals('value', result[0].meta.custom)
  }

  private static LayerSpec makeLayer() {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.POINT),
        StatSpec.of(CharmStatType.IDENTITY)
    )
  }
}
