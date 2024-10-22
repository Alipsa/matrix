package se.alipsa.groovy.matrix

import org.codehaus.groovy.runtime.DefaultGroovyMethods

class ListToColumnConversionStrategy {

  static def asType(List self, Class cls) {
    println('converting list to $cls')
    if (cls == Column) {
      new Column(self)
    } else {
      DefaultGroovyMethods.asType(self, cls)
    }
  }
}
