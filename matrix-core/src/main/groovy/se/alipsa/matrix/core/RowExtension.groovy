package se.alipsa.matrix.core

/**
 * Overrides DefaultGroovyMethods so we can define our own getAt and putAt
 * see resources/META-INF.groovy
 */
class RowExtension {

  static Object getAt(Row self, String[] colNames) {
    return self.subList(colNames)
  }

  static Object getAt(Row self, Collection indices) {
    //println("${this.class}: $indices")
    if (indices instanceof IntRange) {
      return self[indices]
    }
    if (indices.size() == 2) {
      def column = indices[0]
      if (column instanceof String) {
        return self.getAt(indices[0] as String, indices[1] as Class)
      }
      if (column instanceof Number && indices[1] instanceof Class) {
        return self.getAt(indices[0] as Number, indices[1] as Class)
      }
      if (column instanceof Number && indices[1] instanceof Number) {
        return self.subList(indices)
      }
      if (column instanceof String && indices[1] instanceof String) {
        return self.subList(indices as String[])
      }
    }
    if (indices.size() == 3 && indices[0] instanceof String && indices[1] instanceof Class) {
      if (indices[0] instanceof String) {
        return self.getAt(indices[0] as String, indices[1] as Class, indices[2])
      }
    }
    if (!indices.any { ! it instanceof Number}) {
      return self.subList(indices)
    }
    if (!indices.any { ! it instanceof String}) {
      return self.subList(indices as String[])
    }
    throw new IllegalArgumentException("Dont know what to do with ${indices.size()} parameters ($indices) to getAt()")
  }

  static minus(Row self, String name) {
    self.minusColumn(name)
  }

  static minus(Row self, int index) {
    self.minusColumn(index)
  }

}
