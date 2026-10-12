package se.alipsa.matrix.stats.regression

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class DecisionTreePerformanceTest {

  @Test
  void fitsStepFunctionExactly() {
    def x = (1..200).toList()
    def tree = new DecisionTree(x, x.collect { it < 100 ? 1 : 5 })
    assertEquals(1d, tree.RSquared as double, 1e-12)
    assertEquals(1G, tree.predict(50))
    assertEquals(5G, tree.predict(150))
  }

  @Test
  @Tag('slow')
  void twentyThousandObservationsFitWithinBudget() {
    def x = (1..20000).toList()
    long start = System.nanoTime()
    def tree = new DecisionTree(x, x.collect { Math.sin(it / 1000d) }, 8, 2)
    double seconds = (System.nanoTime() - start) / 1e9
    println "DecisionTree n=20000: $seconds seconds, R²=${tree.RSquared}"
    assertTrue(seconds < 40, 'Generous tenfold local performance budget')
    assertTrue(tree.RSquared > 0.95)
  }
}
