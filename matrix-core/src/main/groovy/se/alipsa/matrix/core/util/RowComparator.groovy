package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

import static se.alipsa.matrix.core.Matrix.*

@CompileStatic
class RowComparator<T> implements Comparator<List<T>> {

  LinkedHashMap<Integer, Boolean> columnIdx = [:]

  RowComparator(Integer... columnIdx) {
    if (columnIdx.length == 0) {
      throw new IllegalArgumentException("RowComparator cannot be created for no columns")
    }
    columnIdx.each {
      this.columnIdx[it] = ASC
    }
  }

  RowComparator(LinkedHashMap<Integer, Boolean> criteria) {
    columnIdx = criteria
  }

  @Override
  int compare(List<T> r1, List<T> r2) {
    int result = 0
    for (idx in columnIdx) {
      def v1 = r1[idx.key]
      def v2 = r2[idx.key]

      if (v1 instanceof Comparable) {
        if (idx.value == ASC) {
          result = v1 as Comparable <=> v2 as Comparable
        } else {
          result = v2 as Comparable <=> v1 as Comparable
        }
      } else if (v1 instanceof Number){
        if (idx.value == ASC) {
          result = ((Number)v1) <=> ((Number)v2)
        } else {
          result = ((Number)v2) <=> ((Number)v1)
        }
      } else {
        if (idx.value == ASC) {
          result = String.valueOf(v1) <=> String.valueOf(v2)
        } else {
          result = String.valueOf(v2) <=> String.valueOf(v1)
        }
      }
      if (result != 0) {
        return result
      }
    }
    return result
  }

}
