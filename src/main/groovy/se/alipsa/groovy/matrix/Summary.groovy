package se.alipsa.groovy.matrix

class Summary {

  def private data = new LinkedHashMap<String, Map<String, ?>>()

  def putAt(String key, Map value) {
    data[key] = value
  }

  def getAt(String key) {
    data[key]
  }

  @Override
  String toString() {
    def sb = new StringBuilder()
    data.each {
      sb.append(it.key).append("\n")
      sb.append( "-"*it.key.length()).append("\n")
      it.value.each {
        sb.append(("$it.key:\t$it.value")).append("\n")
      }
      sb.append("\n")
    }
    return sb.toString()
  }
}
