package se.alipsa.groovy.matrix

import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 * Allows a List to the cast to a Column e.g
 * <code> def col = [1,2,3] as Column</code>
 * Registered in resources/META-INF.groovy/org.codehaus.groovy.runtime.ExtensionModule
 */
class ColumnExtension {

  static def asType(List self, Class cls) {
    if (cls == Column) {
      new Column(self)
    } else {
      DefaultGroovyMethods.asType(self, cls)
    }
  }

  static def leftShift(Column self, Collection list) {
    self.addAll(list)
  }
}
