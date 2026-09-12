package test.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.arff.ArffTypeDecl
import se.alipsa.matrix.arff.ArffWriteOptions

class ArffWriteOptionsTest {

  @Test
  void toMapDoesNotExposeMutableInternalState() {
    ArffWriteOptions options = new ArffWriteOptions()
        .nominalColumns(['a'])
        .stringColumns(['b'])
        .nominalMappings([a: ['x', 'y']])
        .attributeTypesByColumn([c: ArffTypeDecl.DATE])
        .dateFormatsByColumn([c: 'yyyy'])
    Map<String, ?> map = options.toMap()

    assertThrows(UnsupportedOperationException) { (map.nominalColumns as Set).add('z') }
    assertThrows(UnsupportedOperationException) { (map.stringColumns as Set).add('z') }
    assertThrows(UnsupportedOperationException) { (map.nominalMappings as Map).remove('a') }
    assertThrows(UnsupportedOperationException) { ((map.nominalMappings as Map).a as List).add('z') }
    assertThrows(UnsupportedOperationException) { (options.nominalMappings.a as List).add('z') }
    assertThrows(UnsupportedOperationException) { (map.attributeTypesByColumn as Map).remove('c') }
    assertThrows(UnsupportedOperationException) { (map.dateFormatsByColumn as Map).remove('c') }

    assertEquals(['a'] as Set, options.nominalColumns)
    assertEquals(['b'] as Set, options.stringColumns)
    assertEquals([a: ['x', 'y']], options.nominalMappings)
    assertSame(options.nominalMappings, options.nominalMappings, 'the immutable view should be cached')
  }
}
