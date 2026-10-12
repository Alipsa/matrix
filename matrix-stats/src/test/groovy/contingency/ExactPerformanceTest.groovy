package se.alipsa.matrix.stats.contingency

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ExactPerformanceTest {

  @Test
  void enumerationAndRefinementHaveBoundedWork() {
    Boschloo.test([[7, 12], [8, 3]] as int[][])
    def counts = UnconditionalExact.COUNTERS.get()
    assertEquals(20 * 12, counts.fisher)
    assertTrue(counts.evaluations <= 101 + UnconditionalExact.REFINEMENT_BUDGET)
    assertEquals(counts.maskSize * counts.evaluations, counts.terms)
  }

  @Test
  @Tag('slow')
  void moderateTablesFinishWithinBudget() {
    long start = System.nanoTime()
    Barnard.test([[130, 120], [100, 150]] as int[][])
    Boschloo.test([[60, 40], [40, 60]] as int[][])
    double seconds = (System.nanoTime() - start) / 1e9
    println "Barnard n=500 and Boschloo n=200: $seconds seconds"
    assertTrue(seconds < 10, 'Generous tenfold local performance budget')
  }
}
