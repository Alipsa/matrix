package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.CssAttributeConfig
import se.alipsa.matrix.gg.geom.GeomUtils
import se.alipsa.matrix.gg.render.RenderContext

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for GeomUtils CSS utility methods.
 */
class GeomUtilsCssTest {

  @Test
  void testNormalizeIdPrefixLowercase() {
    assertEquals('gg', GeomUtils.normalizeIdPrefix(null))
    assertEquals('gg', GeomUtils.normalizeIdPrefix(''))
    assertEquals('gg', GeomUtils.normalizeIdPrefix('   '))
    assertEquals('mydata', GeomUtils.normalizeIdPrefix('MyData'))
    assertEquals('mychart', GeomUtils.normalizeIdPrefix('MYCHART'))
  }

  @Test
  void testNormalizeIdPrefixWhitespace() {
    assertEquals('my-chart', GeomUtils.normalizeIdPrefix('My Chart'))
    assertEquals('iris-data', GeomUtils.normalizeIdPrefix('iris data'))
    // Leading/trailing spaces create leading/trailing hyphens which should be stripped
    String result = GeomUtils.normalizeIdPrefix('  test  prefix  ')
    assertTrue(result.contains('test') && result.contains('prefix'),
        "Should contain both words after normalization")
  }

  @Test
  void testNormalizeIdPrefixInvalidCharacters() {
    assertEquals('mychart', GeomUtils.normalizeIdPrefix('my!@#chart'))
    assertEquals('test-data', GeomUtils.normalizeIdPrefix('test_data'))
    assertEquals('chart', GeomUtils.normalizeIdPrefix('chart$%^'))
    assertEquals('gg', GeomUtils.normalizeIdPrefix('!@#$%'))  // All invalid -> empty -> fallback
  }

  @Test
  void testNormalizeIdPrefixStartsWithDigit() {
    assertEquals('gg', GeomUtils.normalizeIdPrefix('123data'))
    assertEquals('gg', GeomUtils.normalizeIdPrefix('1'))
    assertEquals('data123', GeomUtils.normalizeIdPrefix('data123'))  // OK if digit not first
  }

  @Test
  void testNormalizeIdTokenLowercase() {
    assertEquals('', GeomUtils.normalizeIdToken(null))
    assertEquals('point', GeomUtils.normalizeIdToken('point'))
    assertEquals('point', GeomUtils.normalizeIdToken('Point'))
    assertEquals('point', GeomUtils.normalizeIdToken('POINT'))
  }

  @Test
  void testNormalizeIdTokenUnderscoreToHyphen() {
    assertEquals('qq-line', GeomUtils.normalizeIdToken('qq_line'))
    assertEquals('contour-filled', GeomUtils.normalizeIdToken('contour_filled'))
    assertEquals('density2d-filled', GeomUtils.normalizeIdToken('density2d_filled'))
  }

  @Test
  void testGenerateElementIdDisabled() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([enabled: false])

    assertNull(GeomUtils.generateElementId(ctx, 'point', 0),
        "Should return null when CSS attributes are disabled")
  }

  @Test
  void testGenerateElementIdIdsDisabled() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([enabled: true, includeIds: false])

    assertNull(GeomUtils.generateElementId(ctx, 'point', 0),
        "Should return null when IDs are disabled")
  }

  @Test
  void testGenerateElementIdSinglePanel() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([enabled: true])
    ctx.layerIndex = 0
    ctx.panelRow = null
    ctx.panelCol = null

    assertEquals('gg-layer-0-point-0',
        GeomUtils.generateElementId(ctx, 'point', 0))
    assertEquals('gg-layer-0-point-5',
        GeomUtils.generateElementId(ctx, 'point', 5))
  }

  @Test
  void testGenerateElementIdFaceted() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([enabled: true])
    ctx.layerIndex = 0
    ctx.panelRow = 1
    ctx.panelCol = 2

    assertEquals('gg-panel-1-2-layer-0-point-0',
        GeomUtils.generateElementId(ctx, 'point', 0))
  }

  @Test
  void testGenerateElementIdCustomPrefix() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([
      enabled: true,
      chartIdPrefix: 'iris'
    ])
    ctx.layerIndex = 0

    assertEquals('iris-layer-0-point-0',
        GeomUtils.generateElementId(ctx, 'point', 0))
  }

  @Test
  void testGenerateElementIdNormalizesCustomPrefix() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([
      enabled: true,
      chartIdPrefix: 'My Chart'
    ])
    ctx.layerIndex = 0

    assertEquals('my-chart-layer-0-point-0',
        GeomUtils.generateElementId(ctx, 'point', 0),
        "Should normalize custom prefix")
  }

  @Test
  void testGenerateElementIdInvalidPrefixFallback() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([
      enabled: true,
      chartIdPrefix: '123invalid',  // Starts with digit
      idPrefix: 'custom'
    ])
    ctx.layerIndex = 0

    String id = GeomUtils.generateElementId(ctx, 'point', 0)
    assertTrue(id.startsWith('gg-'),
        "Should use 'gg' fallback when custom prefix is invalid")
  }

  @Test
  void testGenerateElementIdMultipleLayers() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([enabled: true])

    ctx.layerIndex = 0
    assertEquals('gg-layer-0-bar-0',
        GeomUtils.generateElementId(ctx, 'bar', 0))

    ctx.layerIndex = 1
    assertEquals('gg-layer-1-line-0',
        GeomUtils.generateElementId(ctx, 'line', 0))

    ctx.layerIndex = 2
    assertEquals('gg-layer-2-point-0',
        GeomUtils.generateElementId(ctx, 'point', 0))
  }

  @Test
  void testGenerateElementIdNormalizesGeomType() {
    def ctx = new RenderContext()
    ctx.cssConfig = new CssAttributeConfig([enabled: true])
    ctx.layerIndex = 0

    assertEquals('gg-layer-0-qq-line-0',
        GeomUtils.generateElementId(ctx, 'qq_line', 0),
        "Should normalize underscores to hyphens in geom type")
  }

  @Test
  void testGenerateElementIdNullContext() {
    assertNull(GeomUtils.generateElementId(null, 'point', 0),
        "Should return null when context is null")
  }

  @Test
  void testGenerateElementIdNullConfig() {
    def ctx = new RenderContext()
    ctx.cssConfig = null

    assertNull(GeomUtils.generateElementId(ctx, 'point', 0),
        "Should return null when CSS config is null")
  }
}
