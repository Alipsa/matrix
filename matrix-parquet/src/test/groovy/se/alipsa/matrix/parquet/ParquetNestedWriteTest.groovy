package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.schema.LogicalTypeAnnotation
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

class ParquetNestedWriteTest {
  @Test
  void concreteContainerClassesShareOneEncoding() {
    [ [a: List.of(1), b: new ArrayList([1, 2, 3]), c: Collections.emptyList()],
      [a: new LinkedHashMap([x: 1]), b: new HashMap([x: 2]), c: new TreeMap([x: 3])],
      [a: "x${1}", b: 'y'], [(1): "x${1}", (2): 'y'],
      [(1): List.of(1), (2): List.of(1, 2, 3)] ].each { map ->
      byte[] bytes = MatrixParquetWriter.writeBytes(Matrix.builder().columns(m: [map]).types(Map).build())
      assertInstanceOf(LogicalTypeAnnotation.MapLogicalTypeAnnotation, ParquetFixFixtures.footer(bytes).schema.getType('m').logicalTypeAnnotation)
      assertEquals(map.collectEntries { key, value -> [(key): value instanceof CharSequence ? value.toString() : value] }, MatrixParquetReader.read(bytes)[0, 'm'])
    }
  }

  @Test
  void integerKeyedListElementsAreMapsAndStringKeyedElementsRemainStructs() {
    [[(1): 'one'], [a: 'one']].each { map ->
      byte[] bytes = MatrixParquetWriter.writeBytes(Matrix.builder().columns(l: [[map]]).types(List).build())
      def element = ParquetFixFixtures.footer(bytes).schema.getType('l').asGroupType().getType(0).asGroupType().getType(0)
      if (map.containsKey(1)) { assertInstanceOf(LogicalTypeAnnotation.MapLogicalTypeAnnotation, element.logicalTypeAnnotation) }
      else { assertNull(element.logicalTypeAnnotation) }
      assertEquals([map], MatrixParquetReader.read(bytes)[0, 'l'])
    }
    byte[] bytes = MatrixParquetWriter.writeBytes(Matrix.builder().columns(s: ["x${1}"]).types(String).build())
    assertEquals('x1', MatrixParquetReader.read(bytes)[0, 's'])
  }
}
