package se.alipsa.groovy.matrix

/**
 * Overrides DefaultGroovyMethods so we can define our own getAt and putAt
 */
class RowExtension {

  static <T> T getAt(Row self, Collection indices) {
    println indices
    if (indices.size() == 2) {
      def column = indices[0]
      if (column instanceof String) {
        return self.getAt(indices[0] as String, indices[1] as Class<T>)
      }
      if (column instanceof Number) {
        return self.getAt(indices[0] as Number, indices[1] as Class<T>)
      }
    }
    throw new IllegalArgumentException("Dont know what to do with ${indices.size()} parameters to getAt()")
  }
}
