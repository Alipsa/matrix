package gg.scale

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.alipsa.matrix.gg.scale.ScaleAlphaIdentity
import se.alipsa.matrix.gg.scale.ScaleColorIdentity
import se.alipsa.matrix.gg.scale.ScaleFillIdentity
import se.alipsa.matrix.gg.scale.ScaleLinetypeIdentity
import se.alipsa.matrix.gg.scale.ScaleShapeIdentity
import se.alipsa.matrix.gg.scale.ScaleSizeIdentity

import java.util.stream.Stream

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Shared contract tests for all six Scale*Identity types.
 * Each parameterised test runs once per aesthetic.
 * Replaces: testName, testAesthetic, testNullValue, testCustomNaValue,
 *           testFactoryMethod, testFactoryMethodWithParams in each individual file.
 */
class ScaleIdentityContractTest {

  // ---- helpers -------------------------------------------------------

  private static def createScale(String aesthetic, Map params = [:]) {
    switch (aesthetic) {
      case 'alpha':    return params ? new ScaleAlphaIdentity(params) : new ScaleAlphaIdentity()
      case 'color':    return params ? new ScaleColorIdentity(params) : new ScaleColorIdentity()
      case 'fill':     return params ? new ScaleFillIdentity(params) : new ScaleFillIdentity()
      case 'linetype': return params ? new ScaleLinetypeIdentity(params) : new ScaleLinetypeIdentity()
      case 'shape':    return params ? new ScaleShapeIdentity(params) : new ScaleShapeIdentity()
      case 'size':     return params ? new ScaleSizeIdentity(params) : new ScaleSizeIdentity()
      default: throw new IllegalArgumentException("Unknown aesthetic: ${aesthetic}")
    }
  }

  private static def callFactory(String aesthetic, Map params = [:]) {
    switch (aesthetic) {
      case 'alpha':    return params ? scale_alpha_identity(params) : scale_alpha_identity()
      case 'color':    return params ? scale_color_identity(params) : scale_color_identity()
      case 'fill':     return params ? scale_fill_identity(params) : scale_fill_identity()
      case 'linetype': return params ? scale_linetype_identity(params) : scale_linetype_identity()
      case 'shape':    return params ? scale_shape_identity(params) : scale_shape_identity()
      case 'size':     return params ? scale_size_identity(params) : scale_size_identity()
      default: throw new IllegalArgumentException("Unknown aesthetic: ${aesthetic}")
    }
  }

  // ---- data ----------------------------------------------------------

  /**
   * Arguments per aesthetic:
   *   aesthetic (String), expectedClass (Class),
   *   customNaParams (Map – used in testCustomNaValue; naValue is the input AND expected output),
   *   factoryParams  (Map – used in testFactoryMethodWithParams; must include 'name')
   */
  static Stream<Arguments> aesthetics() {
    Stream.of(
      Arguments.of('alpha',    ScaleAlphaIdentity,
                   [naValue: 0.5],                         [name: 'My Alpha',     naValue: 0.8]),
      Arguments.of('color',    ScaleColorIdentity,
                   [naValue: 'black'],                      [name: 'My Colors',    naValue: 'purple']),
      Arguments.of('fill',     ScaleFillIdentity,
                   [naValue: 'white'],                      [name: 'My Fills',     naValue: 'lightgray']),
      Arguments.of('linetype', ScaleLinetypeIdentity,
                   [naValue: 'dashed'],                     [name: 'My Linetypes', naValue: 'dotted']),
      Arguments.of('shape',    ScaleShapeIdentity,
                   [naValue: 'square'],                     [name: 'My Shapes',    naValue: 'diamond']),
      Arguments.of('size',     ScaleSizeIdentity,
                   [naValue: 5.0],                          [name: 'My Sizes',     naValue: 10.0])
    )
  }

  // ---- contract tests ------------------------------------------------

  @ParameterizedTest(name = '{0}')
  @MethodSource('aesthetics')
  void testName(String aesthetic, Class scaleClass, Map customNaParams, Map factoryParams) {
    def scale = createScale(aesthetic, [name: 'Identity Scale'])
    assertEquals('Identity Scale', scale.name)
  }

  @ParameterizedTest(name = '{0}')
  @MethodSource('aesthetics')
  void testAesthetic(String aesthetic, Class scaleClass, Map customNaParams, Map factoryParams) {
    def scale = createScale(aesthetic)
    assertEquals(aesthetic, scale.aesthetic)
  }

  @ParameterizedTest(name = '{0}')
  @MethodSource('aesthetics')
  void testNullValue(String aesthetic, Class scaleClass, Map customNaParams, Map factoryParams) {
    // Contract: transform(null) never returns null — the scale's naValue is the fallback.
    def scale = createScale(aesthetic)
    assertNotNull(scale.transform(null))
  }

  @ParameterizedTest(name = '{0}')
  @MethodSource('aesthetics')
  void testCustomNaValue(String aesthetic, Class scaleClass, Map customNaParams, Map factoryParams) {
    def scale = createScale(aesthetic, [naValue: customNaParams.naValue])
    def expected = customNaParams.naValue instanceof Number
        ? customNaParams.naValue as BigDecimal
        : customNaParams.naValue
    assertEquals(expected, scale.transform(null))
  }

  @ParameterizedTest(name = '{0}')
  @MethodSource('aesthetics')
  void testFactoryMethod(String aesthetic, Class scaleClass, Map customNaParams, Map factoryParams) {
    def scale = callFactory(aesthetic)
    assertNotNull(scale)
    assertTrue(scaleClass.isInstance(scale))
    assertEquals(aesthetic, scale.aesthetic)
  }

  @ParameterizedTest(name = '{0}')
  @MethodSource('aesthetics')
  void testFactoryMethodWithParams(String aesthetic, Class scaleClass, Map customNaParams, Map factoryParams) {
    def scale = callFactory(aesthetic, factoryParams)
    assertNotNull(scale)
    assertEquals(factoryParams.name, scale.name)
  }
}
