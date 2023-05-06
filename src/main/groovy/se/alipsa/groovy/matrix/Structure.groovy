package se.alipsa.groovy.matrix

class Structure {

    LinkedHashMap<String, List<String>> data = [:]

    def putAt(String key, List<String> value) {
        data[key] = value
    }

    def getAt(String key) {
        data[key]
    }

    LinkedHashMap<String, List<String>> getData() {
        return data
    }

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder()
        data.each {
            sb.append(it.key)
            .append(': ')
            .append(it.value)
            .append('\n')
        }
        return sb.toString()
    }
}
