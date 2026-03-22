package se.alipsa.matrix.core

import groovy.transform.CompileStatic

/**
 * Container for per-column summary statistics and descriptive metadata.
 *
 * <p>The summary is keyed by column name and stores a map of metric names to
 * values, with convenience accessors and a text representation for inspection.</p>
 */
@CompileStatic
class Summary {

  private Map<String, Map<String, ?>> data = [:]

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
  @SuppressWarnings('DuplicateStringLiteral')
  String toString() {
    def sb = new StringBuilder()
    data.each { it ->
      sb.append(it.key).append("\n")
      sb.append( "-"*it.key.length()).append("\n")
      it.value.each {
        sb.append("$it.key:\t$it.value\n")
      }
      sb.append("\n")
    }
    return sb.toString()
  }

  Map<String, Map<String, ?>> getData() {
    return data
  }
}
