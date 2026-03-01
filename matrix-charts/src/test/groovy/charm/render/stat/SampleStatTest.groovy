package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.geom.PointBuilder
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.SampleStat

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class SampleStatTest {

  @Test
  void testLayerBuilderDefaultsToGeomStatWhenUnspecified() {
    LayerSpec layer = new PointBuilder().build()
    assertEquals(CharmStatType.IDENTITY, layer.statType)
  }

  @Test
  void testLayerBuilderAllowsExplicitStatOverride() {
    LayerSpec layer = new PointBuilder()
        .stat('sample')
        .params([n: 3, seed: 7])
        .build()

    assertEquals(CharmStatType.SAMPLE, layer.statType)
    assertEquals(3, layer.params['n'])
    assertEquals(7, layer.params['seed'])
  }

  @Test
  void testLayerBuilderStatSpecCarriesStatParams() {
    LayerSpec layer = new PointBuilder()
        .stat(StatSpec.of(CharmStatType.SAMPLE, [n: 4, method: 'systematic']))
        .build()

    assertEquals(CharmStatType.SAMPLE, layer.statType)
    assertEquals(4, layer.statSpec.params['n'])
    assertEquals('systematic', layer.statSpec.params['method'])
  }

  @Test
  void testLayerBuilderStatCallReplacesPreviousStatParams() {
    LayerSpec layer = new PointBuilder()
        .stat(StatSpec.of(CharmStatType.SAMPLE, [n: 4, method: 'systematic']))
        .stat(CharmStatType.SAMPLE)
        .build()

    assertEquals(CharmStatType.SAMPLE, layer.statType)
    assertTrue(layer.statSpec.params.isEmpty())
  }

  @Test
  void testLayerBuilderRejectsUnsupportedStatNameWithValidationError() {
    CharmValidationException e = assertThrows(CharmValidationException) {
      new PointBuilder().stat('not_a_stat')
    }

    assertTrue(e.message.contains("Unsupported stat 'not_a_stat'"))
  }

  @Test
  void testLayerBuilderRejectsUnsupportedStatTypeWithValidationError() {
    CharmValidationException e = assertThrows(CharmValidationException) {
      new PointBuilder().stat([:])
    }

    assertTrue(e.message.contains("Unsupported stat type"))
  }

  @Test
  void testRandomSamplingRespectsSizeAndSeed() {
    LayerSpec layer = sampleLayer([n: 5, seed: 42, method: 'random'])
    List<LayerData> data = sampleData(20)

    List<LayerData> first = SampleStat.compute(layer, data)
    List<LayerData> second = SampleStat.compute(layer, data)

    assertEquals(5, first.size())
    assertEquals(first*.rowIndex, second*.rowIndex)
    assertEquals(first*.rowIndex.sort(), first*.rowIndex)
    assertNotSame(data[first[0].rowIndex], first[0])
  }

  @Test
  void testSystematicSamplingIsDeterministic() {
    LayerSpec layer = sampleLayer([n: 4, method: 'systematic'])
    List<LayerData> sampled = SampleStat.compute(layer, sampleData(10))

    assertEquals([0, 2, 5, 7], sampled*.rowIndex)
  }

  @Test
  void testSampleReturnsAllRowsWhenNExceedsInputSize() {
    LayerSpec layer = sampleLayer([n: 100, method: 'random'])
    List<LayerData> data = sampleData(8)
    List<LayerData> sampled = SampleStat.compute(layer, data)

    assertEquals(8, sampled.size())
    assertTrue(sampled*.rowIndex.containsAll((0..<8)))
  }

  private static LayerSpec sampleLayer(Map<String, Object> statParams) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.POINT),
        StatSpec.of(CharmStatType.SAMPLE, statParams),
        null,
        true,
        PositionSpec.of(CharmPositionType.IDENTITY),
        [:]
    )
  }

  private static List<LayerData> sampleData(int count) {
    (0..<count).collect { int i ->
      new LayerData(x: i, y: i * 2, rowIndex: i)
    }
  }
}
