package se.alipsa.groovy.stats

import groovyjarjarantlr4.v4.runtime.misc.NotNull
import se.alipsa.groovy.matrix.Matrix

class Randomize {

  /**
   * Creates a new Matrix with the rows randomly distributed
   *
   * @param data the Matrix to reorder randomly
   * @return a new Matrix with the rows randomly distributed
   */
  static Matrix randomOrder(@NotNull Matrix data) {
    def copy = data.clone()
    def rows = copy.rows()
    rows.shuffle()
    return Matrix.create(copy.name, copy.columnNames(), rows, copy.columnTypes())
  }

  /**
   * Creates a new Matrix with the rows randomly distributed
   *
   * @param data the Matrix to reorder randomly
   * @param random the object used to generate a stream of pseudorandom numbers
   * @return a new Matrix with the rows randomly distributed
   */
  static Matrix randomOrder(Matrix data, Random random) {
    def copy = data.clone()
    def rows = copy.rows()
    rows.shuffle(random)
    return Matrix.create(copy.name, copy.columnNames(), rows, copy.columnTypes())
  }

  /**
   * Creates a new Matrix with the rows randomly distributed
   *
   * @param data the Matrix to reorder randomly
   * @param seed the initial value of the internal state of the pseudorandom number generator
   * @return a new Matrix with the rows randomly distributed
   */
  static Matrix randomOrder(Matrix data, long seed) {
    Random random = new Random(seed)
    return randomOrder(data, random)
  }
}
