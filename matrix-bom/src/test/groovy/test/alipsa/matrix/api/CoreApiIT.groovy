package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Converter
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.GroupedMatrix
import se.alipsa.matrix.core.JoinType
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.core.RollingMatrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.core.Structure
import se.alipsa.matrix.core.Summary
import se.alipsa.matrix.core.ValueConverter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertInstanceOf
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers the documented matrix-core builders, transforms, joins, statistics, and converters. */
@Tag('core')
class CoreApiIT implements ApiItSupport {

  @Test
  void buildersColumnsRowsAndJavaMapShapes() {
    Matrix matrix = Matrix.builder([x: [1, 2, 3], y: ['a', 'b', 'c']], [Integer, String], 'core').build()
    assertEquals(['x', 'y'], matrix.columnNames())
    assertEquals(3, matrix.rowCount())
    assertEquals(2, Matrix.builder().rows([[1, 'a'], [2, 'b']]).columnNames(['n', 'label'])
        .types([Integer, String]).build().rowCount())
    assertEquals(['c1', 'c2'], Matrix.anonymousHeader(2))
  }

  @Test
  void columnArithmeticAndCoreViews() {
    Matrix matrix = mtcars()
    Column mpg = matrix.column('mpg')
    assertInstanceOf(Column, mpg + 1)
    assertInstanceOf(Column, mpg - 1)
    assertInstanceOf(Column, mpg * 2)
    assertInstanceOf(Column, mpg / 2)
    assertInstanceOf(Column, mpg.power(2))
    assertEquals(5, matrix.top().rowCount())
    assertEquals(5, matrix.bottom().rowCount())
    assertEquals(4, matrix.sample(4, new Random(1)).rowCount())
    assertTrue(matrix.sampleFraction(0.25, new Random(1)).rowCount() > 0)
    assertEquals(matrix.rowCount(), matrix.dimensions().observations)
    assertEquals(['miles'], matrix.rename('mpg', 'miles').columnNames().findAll { it == 'miles' })
  }

  @Test
  void joinsGroupingRollingAndSummary() {
    Matrix left = Matrix.builder([id: [1, 2], value: [10, 20]], [Integer, Integer], 'left').build()
    Matrix right = Matrix.builder([id: [1, 2], label: ['a', 'b']], [Integer, String], 'right').build()
    assertEquals(2, left.merge(right, 'id', JoinType.INNER).rowCount())
    assertEquals(4, left.crossJoin(right).rowCount())
    GroupedMatrix grouped = mtcars().groupBy('cyl')
    assertTrue(grouped.keys().size() > 1)
    Summary summary = Stat.summary(mtcars())
    assertTrue(summary.toString().contains('mpg'))
    Structure structure = Stat.str(mtcars())
    assertTrue(structure.toString().contains('mpg'))
    RollingMatrix rolling = mtcars().rolling(3)
    assertEquals(mtcars().rowCount(), rolling.mean().rowCount())
  }

  @Test
  void convertersAndAssertionsRemainUsable() {
    def converted = ListConverter.toBigDecimals([1, 2])
    assertTrue(converted.every { it.toBigDecimal().compareTo(1G) == 0 || it.toBigDecimal().compareTo(2G) == 0 })
    assertEquals(12, ValueConverter.convert('12', Integer))
    assertEquals('x', Converter.of('x', Integer) { it as Integer }.columnName)
    Matrix copy = mtcars().clone()
    MatrixAssertions.assertContentMatches(mtcars(), copy, mtcars().diff(copy))
    Grid grid = new Grid([[1, 2], [3, 4]])
    assertEquals(2, grid.data.size())
    grid.data[0][1] = 9
    assertEquals(9, grid[0, 1])
    assertThrows(UnsupportedOperationException) { grid.data << [5, 6] }
  }
}
