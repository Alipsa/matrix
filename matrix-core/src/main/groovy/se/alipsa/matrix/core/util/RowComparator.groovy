package se.alipsa.matrix.core.util

import static se.alipsa.matrix.core.Matrix.*

import groovy.transform.CompileStatic

/**
 * Comparator for Matrix rows based on one or more column indices.
 *
 * Null values sort before non-null values in ascending order and after non-null values
 * in descending order.
 */
@CompileStatic
class RowComparator<T> implements Comparator<List<T>> {

  @SuppressWarnings('ImplementationAsType')
  LinkedHashMap<Integer, Boolean> columnIdx = [:]

  RowComparator(Integer... columnIdx) {
    if (columnIdx.length == 0) {
      throw new IllegalArgumentException('RowComparator cannot be created for no columns')
    }
    columnIdx.each {
      this.columnIdx[it] = ASC
    }
  }

  @SuppressWarnings('ImplementationAsType')
  RowComparator(LinkedHashMap<Integer, Boolean> criteria) {
    columnIdx = criteria
  }

  @Override
  int compare(List<T> r1, List<T> r2) {
    for (idx in columnIdx) {
      int result = idx.value == ASC
          ? compareValues(r1[idx.key], r2[idx.key])
          : compareValues(r2[idx.key], r1[idx.key])
      if (result != 0) {
        return result
      }
    }
    return 0
  }

  @SuppressWarnings('Instanceof')
  private static int compareValues(Object v1, Object v2) {
    if (v1 == v2) {
      return 0
    }
    if (v1 == null) {
      return -1
    }
    if (v2 == null) {
      return 1
    }
    if (v1 instanceof Comparable) {
      return (v1 as Comparable) <=> (v2 as Comparable)
    }
    return String.valueOf(v1) <=> String.valueOf(v2)
  }

}
