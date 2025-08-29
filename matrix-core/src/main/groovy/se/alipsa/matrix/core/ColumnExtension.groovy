package se.alipsa.matrix.core

import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 * Allows a List to the cast to a Column e.g
 * <code> def col = [1,2,3] as Column</code>
 * Registered in resources/META-INF.groovy/org.codehaus.groovy.runtime.ExtensionModule
 */
@CompileStatic
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

  static Object getAt(Column self, Collection indices) {
    if (indices.size() == 2 && indices[1] instanceof Class) {
      return self.getAt(indices[0] as Number, indices[1] as Class)
    }
    self[indices]
  }
}
