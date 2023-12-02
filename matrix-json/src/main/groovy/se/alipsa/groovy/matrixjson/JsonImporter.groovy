package se.alipsa.groovy.matrixjson

import se.alipsa.groovy.matrix.Matrix
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class JsonImporter {


  /**
   *
   * @param str a json string containing a list of rows to import
   *        a row is defined as a json object
   * @return
   */
  Matrix parse(String str) {
    JsonSlurper importer = new JsonSlurper()
    def o = importer.parseText(str)
    return jsonToMatrix(o)
  }

  Matrix parse(InputStream is, Charset charset = StandardCharsets.UTF_8) {
    JsonSlurper importer = new JsonSlurper()
    def o = importer.parse(is, charset.name())
    return jsonToMatrix(o)
  }

  Matrix parse(File file, Charset charset = StandardCharsets.UTF_8) {
    JsonSlurper importer = new JsonSlurper()
    def o = importer.parse(file, charset.name())
    return jsonToMatrix(o)
  }

  Matrix parse(Reader reader) {
    JsonSlurper importer = new JsonSlurper()
    def o = importer.parse(reader)
    return jsonToMatrix(o)
  }

  /**
   * Create a Matrix from the result of a JsonSlurper.parse method
   *
   * @param o the result of a JsonSlurper.parse method; should be a List
   * @return a Matrix corresponding to the json data
   */
  static Matrix jsonToMatrix(Object o) {
    if (! o instanceof List) {
      throw new IllegalArgumentException('The Json string is not a list of objects')
    }
    def rows = o as List<Map<String, List<?>>>
    Map<String, List<?>> columnMap = new LinkedHashMap<>()
    for (Map<String, ?> map in rows) {
      map.each {
        columnMap.computeIfAbsent(it.key, k -> []) << it.value
      }
    }
    return new Matrix(columnMap)
  }
}