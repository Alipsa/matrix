import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.JoinCardinality
import se.alipsa.matrix.core.JoinType
import se.alipsa.matrix.core.Joiner
import se.alipsa.matrix.core.Matrix

class JoinCardinalityTest {

  @Test
  void testEveryJoinAndCardinality() {
    def left = Matrix.builder().data(id: [1, 1L, 9]).types(Number).build()
    def right = Matrix.builder().data(id: [1.0, 2]).types(Number).build()
    JoinType.values().findAll { it != JoinType.CROSS }.each { joinType ->
      assertThrows(IllegalArgumentException) {
        Joiner.merge(left, right, 'id', joinType, JoinCardinality.ONE_TO_ONE)
      }
      def error = assertThrows(IllegalArgumentException) {
        Joiner.merge(left, right, ['id'], joinType, JoinCardinality.ONE_TO_MANY)
      }
      assertTrue(error.message.contains('left'))
      assertTrue(error.message.contains('ONE_TO_MANY'))
      assertTrue(error.message.contains('1'))
      assertNotNull(left.merge(right, [x: 'id', y: 'id'], joinType, JoinCardinality.MANY_TO_ONE))
      assertNotNull(left.merge(right, ['id'], joinType, JoinCardinality.MANY_TO_MANY))
      assertThrows(IllegalArgumentException) {
        right.merge(left, 'id', joinType, JoinCardinality.MANY_TO_ONE)
      }
    }
    assertIterableEquals([1, 1L, 9], left['id'])
    assertEquals(4, left.merge(left, 'id', JoinType.INNER).rowCount() - 1)
    assertEquals(6, left.merge(right, 'id', JoinType.CROSS, JoinCardinality.MANY_TO_MANY).rowCount())
    JoinCardinality.values().findAll { it != JoinCardinality.MANY_TO_MANY }.each { cardinality ->
      assertThrows(IllegalArgumentException) { left.merge(right, 'id', JoinType.CROSS, cardinality) }
    }
    assertThrows(IllegalArgumentException) { left.merge(right, 'id', JoinType.LEFT, null) }
    assertThrows(IllegalArgumentException) { left.merge(right, 'id', null, JoinCardinality.MANY_TO_MANY) }
  }

  @Test
  void testWholeInputAndUnmatchableKeys() {
    def unique = Matrix.builder().data(id: [1]).types(Object).build()
    def absentDuplicates = Matrix.builder().data(id: [9, 9L]).types(Object).build()
    assertThrows(IllegalArgumentException) {
      unique.merge(absentDuplicates, 'id', JoinType.LEFT, JoinCardinality.MANY_TO_ONE)
    }
    def invalidKeys = Matrix.builder().data(id: [null, null, Double.NaN, Float.NaN,
        Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY]).types(Object).build()
    assertEquals(0, invalidKeys.merge(invalidKeys, 'id', JoinType.INNER, JoinCardinality.ONE_TO_ONE).rowCount())
    def typed = Matrix.builder().data(id: [1, '1', 9007199254740992L, 9007199254740993L]).types(Object).build()
    assertEquals(4, typed.merge(typed, ['id'], JoinType.INNER, JoinCardinality.ONE_TO_ONE).rowCount())
    def composite = Matrix.builder().data(id: [1, 1L, 1], part: ['a', 'b', null]).types(Object, String).build()
    assertEquals(2, composite.merge(composite, [x: ['id', 'part'], y: ['id', 'part']],
        JoinType.INNER, JoinCardinality.ONE_TO_ONE).rowCount())
    def repeated = Matrix.builder().data(id: [1, 1.0], part: ['a', 'a']).types(Number, String).build()
    assertThrows(IllegalArgumentException) {
      Joiner.merge(composite, repeated, ['id', 'part'], JoinType.INNER, JoinCardinality.MANY_TO_ONE)
    }
  }
}
