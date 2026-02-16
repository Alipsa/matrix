package charm.render

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Validates Phase 1 fixture corpus coverage for P0 migration features.
 */
@CompileStatic
class CharmParityCorpusTest {

  private static final Set<String> P0_GEOMS = [
      'GeomPoint', 'GeomLine', 'GeomBar', 'GeomCol', 'GeomHistogram', 'GeomBoxplot',
      'GeomArea', 'GeomSmooth', 'GeomDensity', 'GeomViolin', 'GeomTile', 'GeomText'
  ] as Set<String>

  private static final Set<String> P0_STATS = [
      'IDENTITY', 'COUNT', 'BIN', 'BOXPLOT', 'SMOOTH', 'DENSITY', 'YDENSITY'
  ] as Set<String>

  private static final Set<String> P0_COORDS = [
      'CoordCartesian', 'CoordFlip', 'CoordFixed'
  ] as Set<String>

  /**
   * Verify that the manifest covers all required P0 geoms, stats, and coords.
   */
  @Test
  void testManifestCoversP0Features() {
    List<List<String>> rows = CharmRenderTestUtil.loadSimpleCsvResource('charm-parity/p0-fixture-manifest.csv')
    assertTrue(rows.size() > 1, 'manifest must contain header + at least one data row')
    assertEquals(['area', 'feature', 'fixture'], rows.first())

    Map<String, Set<String>> actual = [
        geom : [] as Set<String>,
        stat : [] as Set<String>,
        coord: [] as Set<String>
    ]

    rows.tail().each { List<String> row ->
      assertEquals(3, row.size(), "manifest row must contain area, feature, fixture: ${row}")
      String area = row[0]
      String feature = row[1]
      String fixture = row[2]
      assertTrue(actual.containsKey(area), "unknown manifest area '${area}'")
      assertTrue(feature != null && !feature.isBlank(), 'feature must be non-blank')
      assertTrue(fixture != null && !fixture.isBlank(), 'fixture path must be non-blank')
      actual[area] << feature
    }

    assertEquals(P0_GEOMS, actual.geom, 'manifest must include each P0 geom')
    assertEquals(P0_STATS, actual.stat, 'manifest must include each P0 stat')
    assertEquals(P0_COORDS, actual.coord, 'manifest must include each P0 coord')
  }

  /**
   * Verify every fixture referenced by the manifest exists and contains data.
   */
  @Test
  void testReferencedFixturesExistAndAreNonEmpty() {
    List<List<String>> rows = CharmRenderTestUtil.loadSimpleCsvResource('charm-parity/p0-fixture-manifest.csv')
    rows.tail().each { List<String> row ->
      String fixture = row[2]
      String resourcePath = "charm-parity/${fixture}"
      URL resource = this.class.classLoader.getResource(resourcePath)
      assertNotNull(resource, "missing fixture resource: ${resourcePath}")
      List<List<String>> fixtureRows = CharmRenderTestUtil.loadSimpleCsvResource(resourcePath)
      assertTrue(fixtureRows.size() > 1, "fixture '${resourcePath}' must contain header + at least one row")
      assertTrue(fixtureRows.first().size() > 0, "fixture '${resourcePath}' must contain at least one column")
    }
  }
}
