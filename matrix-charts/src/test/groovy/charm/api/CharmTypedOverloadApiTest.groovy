package charm.api

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.ArrowSpec
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.ColumnExpr
import se.alipsa.matrix.charm.ColumnRef
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.MappingSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.geom.Bin2dBuilder
import se.alipsa.matrix.charm.geom.CurveBuilder
import se.alipsa.matrix.charm.geom.LabelBuilder
import se.alipsa.matrix.charm.geom.LayerBuilder
import se.alipsa.matrix.charm.geom.PointBuilder
import se.alipsa.matrix.charm.geom.SegmentBuilder
import se.alipsa.matrix.charm.geom.TextBuilder

import java.lang.reflect.Method
import java.lang.reflect.Modifier

@CompileStatic
class CharmTypedOverloadApiTest {

  private static final Set<String> MAPPING_FLUENT_METHODS = [
      'x', 'y', 'color', 'fill', 'size', 'shape', 'group', 'xend', 'yend',
      'xmin', 'xmax', 'ymin', 'ymax', 'alpha', 'linetype', 'label', 'tooltip', 'weight'
  ] as Set<String>

  @Test
  void testMappingSpecTypedOverloads() {
    MappingSpec spec = new MappingSpec()
    spec.x('column_name')
    assertNotNull(spec.x)
    assertTrue(spec.x instanceof ColumnRef)
    assertEquals('column_name', spec.x.columnName())

    ColumnExpr expr = new ColumnRef('other')
    spec.y(expr)
    assertNotNull(spec.y)
    assertSame(expr, spec.y)
  }

  @Test
  void testMappingSpecFluentApiDoesNotExposeObjectOverloads() {
    List<Method> objectOverloads = MappingSpec.declaredMethods.findAll { Method method ->
      MAPPING_FLUENT_METHODS.contains(method.name) &&
          isPublicSingleObjectParameter(method)
    }
    assertTrue(objectOverloads.isEmpty(), "Unexpected Object overloads: ${objectOverloads*.name}")
  }

  @Test
  void testTextBuilderFontfaceTypedOverloads() {
    TextBuilder builder = new TextBuilder()
    builder.fontface('bold')
    assertEquals('bold', builder.build().params['fontface'])

    builder.fontface(2)
    assertEquals(2, builder.build().params['fontface'])
  }

  @Test
  void testLabelBuilderFontfaceTypedOverloads() {
    LabelBuilder builder = new LabelBuilder()
    builder.fontface('italic')
    assertEquals('italic', builder.build().params['fontface'])

    builder.fontface(3)
    assertEquals(3, builder.build().params['fontface'])
  }

  @Test
  void testBin2dBuilderBinsTypedOverloads() {
    Bin2dBuilder builder = new Bin2dBuilder()
    builder.bins(10)
    assertEquals(10, builder.build().params['bins'])

    builder.bins([5, 8])
    assertEquals([5, 8], builder.build().params['bins'])
  }

  @Test
  void testLayerBuilderStatTypedOverloads() {
    LayerBuilder builder = new PointBuilder()
    builder.stat(CharmStatType.IDENTITY)
    assertEquals(CharmStatType.IDENTITY, builder.build().statType)

    builder.stat('count')
    assertEquals(CharmStatType.COUNT, builder.build().statType)

    builder.stat(StatSpec.of(CharmStatType.BIN))
    assertEquals(CharmStatType.BIN, builder.build().statType)
  }

  @Test
  void testLayerBuilderPositionTypedOverloads() {
    LayerBuilder builder = new PointBuilder()
    builder.position(CharmPositionType.DODGE)
    LayerSpec layer = builder.build()
    assertEquals(CharmPositionType.DODGE, layer.positionSpec.type)

    builder.position(PositionSpec.of(CharmPositionType.STACK))
    layer = builder.build()
    assertEquals(CharmPositionType.STACK, layer.positionSpec.type)

    builder.position('jitter')
    layer = builder.build()
    assertEquals(CharmPositionType.JITTER, layer.positionSpec.type)
  }

  @Test
  void testLineEndpointBuilderArrowTypedOverloads() {
    ArrowSpec arrow = ArrowSpec.end()

    SegmentBuilder segmentBuilder = new SegmentBuilder()
    segmentBuilder.arrow(arrow)
    assertSame(arrow, segmentBuilder.build().params['arrow'])

    CurveBuilder curveBuilder = new CurveBuilder()
    curveBuilder.arrow(arrow)
    assertSame(arrow, curveBuilder.build().params['arrow'])
  }

  @Test
  void testBuilderApisDoNotExposeTargetedObjectOverloads() {
    assertNoPublicObjectOverload(TextBuilder, 'fontface')
    assertNoPublicObjectOverload(LabelBuilder, 'fontface')
    assertNoPublicObjectOverload(Bin2dBuilder, 'bins')
    assertNoPublicObjectOverload(LayerBuilder, 'stat')
    assertNoPublicObjectOverload(LayerBuilder, 'position')
    assertNoPublicObjectOverload(SegmentBuilder, 'arrow')
    assertNoPublicObjectOverload(CurveBuilder, 'arrow')
  }

  private static void assertNoPublicObjectOverload(Class<?> type, String methodName) {
    List<Method> objectOverloads = type.declaredMethods.findAll { Method method ->
      method.name == methodName && isPublicSingleObjectParameter(method)
    }
    assertTrue(objectOverloads.isEmpty(), "${type.simpleName}.${methodName}(Object) should not be public API")
  }

  private static boolean isPublicSingleObjectParameter(Method method) {
    Modifier.isPublic(method.modifiers) &&
        method.parameterTypes.length == 1 &&
        method.parameterTypes[0] == Object
  }

}
