package se.alipsa.matrix.stats.contingency

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class ExactPerformanceTest {

  @Test
  void enumerationVisitsEveryTableOnce() {
    Set<List<Integer>> visited = [] as Set
    double[] result = UnconditionalExact.maximise(7, 9) { int x1, int x2, double[] logs ->
      assertTrue(visited.add([x1, x2]))
      true
    }
    assertEquals(8 * 10, visited.size())
    assertEquals(1d, result[0], 1e-12)
  }

  @Test
  void moderateTablesFinishWithinBudget() {
    long start = System.nanoTime()
    Barnard.test([[130, 120], [100, 150]] as int[][])
    Boschloo.test([[60, 40], [40, 60]] as int[][])
    double seconds = (System.nanoTime() - start) / 1e9
    println "Barnard n=500 and Boschloo n=200: $seconds seconds"
    assertTrue(seconds < 10, 'Generous tenfold local performance budget')
  }
}
