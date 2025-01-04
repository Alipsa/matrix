package se.alipsa.matrix.core

import groovy.transform.CompileStatic

@CompileStatic
class Summary {

  private LinkedHashMap<String, Map<String, ?>> data = new LinkedHashMap<>()

  def putAt(String column, Map value) {
    data[column] = value
  }

  def getAt(String column) {
    data[column]
  }

  def getAt(String column, key) {
    data[column][key]
  }

  Object getProperty(String key) {
    data[key]
  }

  @Override
  String toString() {
    def sb = new StringBuilder()
    data.each { it ->
      sb.append(it.key).append("\n")
      sb.append( "-"*it.key.length()).append("\n")
      it.value.each {
        sb.append(("$it.key:\t$it.value")).append("\n")
      }
      sb.append("\n")
    }
    return sb.toString()
  }

  LinkedHashMap<String, Map<String, ?>> getData() {
    return data
  }
}
