import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix

import java.util.concurrent.TimeUnit

/**
 * Performance sanity checks for rolling, cumulative, and diff operations.
 *
 * <p>These tests are tagged as {@code slow} and excluded from the default
 * test run. Enable with {@code -PrunSlowTests=true}.</p>
 */
@Tag('slow')
class PerformanceTest {

  private static final int LARGE_SIZE = 100_000

  private static Column largeNumericColumn() {
    List<Integer> values = new ArrayList<>(LARGE_SIZE)
    for (int i = 0; i < LARGE_SIZE; i++) {
      values.add(i)
    }
    new Column('value', values, Integer)
  }

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testRollingMeanOnLargeColumn() {
    Column c = largeNumericColumn()
    Column result = c.rolling(window: 100, minPeriods: 50).mean()
    assertEquals(LARGE_SIZE, result.size())
    assertNotNull(result[LARGE_SIZE - 1])
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testCumsumOnLargeColumn() {
    Column c = largeNumericColumn()
    Column result = c.cumsum()
    assertEquals(LARGE_SIZE, result.size())
    assertNotNull(result[LARGE_SIZE - 1])
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testDiffOnLargeColumn() {
    Column c = largeNumericColumn()
    Column result = c.diff()
    assertEquals(LARGE_SIZE, result.size())
    assertNull(result[0])
    assertEquals(1 as BigDecimal, result[LARGE_SIZE - 1])
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testRollingMeanOnLargeMatrix() {
    List<Integer> values = new ArrayList<>(LARGE_SIZE)
    for (int i = 0; i < LARGE_SIZE; i++) {
      values.add(i)
    }
    Matrix table = Matrix.builder()
        .data(a: new ArrayList<>(values), b: new ArrayList<>(values),
              c: new ArrayList<>(values), d: new ArrayList<>(values),
              e: new ArrayList<>(values))
        .types(Integer, Integer, Integer, Integer, Integer)
        .build()

    Matrix result = table.rolling(window: 100, minPeriods: 50).mean()
    assertEquals(LARGE_SIZE, result.rowCount())
    assertEquals(5, result.columnCount())
  }

  @Test
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void testRollingApplyOnLargeColumn() {
    Column c = largeNumericColumn()
    Column result = c.rolling(window: 100, minPeriods: 50).apply { Column window ->
      window.size()
    }
    assertEquals(LARGE_SIZE, result.size())
  }


  @Test
  void testRowMetadataBenchmark() {
    def bean = java.lang.management.ManagementFactory.threadMXBean as com.sun.management.ThreadMXBean
    [4, 80].each { width ->
      def columns = (0..<width).collectEntries { [("c$it".toString()): (0..<2000).toList()] }
      def matrix = Matrix.builder().data(columns).build()
      5.times { matrix.rows() }
      long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId())
      long start = System.nanoTime()
      10.times {
        def rows = matrix.rows()
        rows.each { row ->
          row.each { value -> assertNotNull(value) }
          assertEquals(row[0], row['c0'])
          row.subList(0, 2).each { value -> assertNotNull(value) }
          row.set(0, row[0])
        }
      }
      println "Row benchmark width=$width rows=2000 repetitions=10 elapsedMs=${(System.nanoTime() - start) / 1e6} allocatedBytes=${bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated}"
    }
  }

}
