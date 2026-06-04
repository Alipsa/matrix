// Full package declaration required to access @PackageScope members of CharmRenderer.
package se.alipsa.matrix.charm.render

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale

/**
 * Tests scale divergence decisions in the Charm renderer.
 */
class CharmRendererScaleDivergenceTest {

  @Test
  void testPositionalDivergenceWarningForLayerScaleOverTenPercentFromGlobalAxis() {
    ContinuousCharmScale globalScale = new ContinuousCharmScale(domainMin: 0, domainMax: 100)
    ContinuousCharmScale layerScale = new ContinuousCharmScale(domainMin: 20, domainMax: 120)

    String warning = CharmRenderer.positionalDivergenceWarning('x', 1, layerScale, globalScale)

    assertEquals(
        'Layer 2 uses a per-layer x scale that diverges from the global axis by 20.0%; axis ticks may be misleading',
        warning
    )
  }

  @Test
  void testPositionalDivergenceWarningIgnoresLayerScaleWithinTenPercentOfGlobalAxis() {
    ContinuousCharmScale globalScale = new ContinuousCharmScale(domainMin: 0, domainMax: 100)
    ContinuousCharmScale layerScale = new ContinuousCharmScale(domainMin: 5, domainMax: 105)

    assertNull(CharmRenderer.positionalDivergenceWarning('y', 0, layerScale, globalScale))
  }

}
