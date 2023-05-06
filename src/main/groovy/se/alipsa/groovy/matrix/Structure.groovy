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
        int rowCount = 0
        data.each {
            if (rowCount == 0) {
                sb.append(it.key)
                .append(' (')
                .append(String.join(', ', it.value))
                .append(')\n')
                .append("-"*(sb.length()-1))
                .append('\n')
            } else {
                sb.append(it.key)
                        .append(': ')
                        .append(it.value)
                        .append('\n')
            }
            rowCount++
        }
        return sb.toString()
    }
}
