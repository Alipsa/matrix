package se.alipsa.matrix.matrixjson

import se.alipsa.matrix.core.Matrix
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class JsonImporter {

  private JsonImporter() {
    // Only static methods
  }

  /**
   *
   * @param str a json string containing a list of rows to import
   *        a row is defined as a json object
   * @return
   */
  static Matrix parse(String str) {
    JsonSlurper importer = new JsonSlurper()
    def o = importer.parseText(str)
    return jsonToMatrix(o)
  }

  static Matrix parse(InputStream is, Charset charset = StandardCharsets.UTF_8) {
    JsonSlurper importer = new JsonSlurper()
    def o = importer.parse(is, charset.name())
    return jsonToMatrix(o)
  }

  static Matrix parse(File file, Charset charset = StandardCharsets.UTF_8) {
    JsonSlurper importer = new JsonSlurper()
    def o = importer.parse(file, charset.name())
    return jsonToMatrix(o)
  }

  static Matrix parse(Reader reader) {
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
  private static Matrix jsonToMatrix(Object root) {
    List<Map<String, String>> flatMaps = []
    Set<String> keySet = new LinkedHashSet<>()
    Iterator it = root.iterator()
    while (it.hasNext()) {
      def o = it.next()
      Map<String, String> flatMap = [:]
      flatten('', o, flatMap)
      it.remove()
      flatMaps << flatMap
      keySet.addAll(flatMap.keySet())
    }
    // Find the max of each key and fill the rest with null
    List rows = []
    for (int i = 0; i < flatMaps.size(); i++) {
      def m = flatMaps.get(i)
      keySet.each {
        if (!m.containsKey(it)) {
          m.put(it, null)
        }
      }
      rows << new ArrayList<>(m.values())
    }
    flatMaps = null
    return Matrix.builder()
        .rows(rows)
        .columnNames(keySet)
        .build()
  }

  private static def flatten(String currentPath, Object jsonNode, Map<String, Object> map) {
    if (jsonNode instanceof Map) {
      def pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";
      jsonNode.each {
        flatten(pathPrefix + it.getKey(), it.getValue(), map)
      }
    } else if (jsonNode instanceof List) {
      jsonNode.eachWithIndex { it, idx ->
        flatten(currentPath + '[' + idx + ']', it, map)
      }
    } else {
      // its a value
      map.put(currentPath, jsonNode)
    }
  }
}