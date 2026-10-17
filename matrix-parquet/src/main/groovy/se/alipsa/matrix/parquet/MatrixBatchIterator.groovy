package se.alipsa.matrix.parquet

import se.alipsa.matrix.core.Matrix

/** Closeable iterator of bounded Matrix batches; close explicitly on early termination. */
interface MatrixBatchIterator extends Iterator<Matrix>, Closeable {
  /** Returns an empty Matrix retaining this selection's schema and types. */
  Matrix emptyMatrix()
}
