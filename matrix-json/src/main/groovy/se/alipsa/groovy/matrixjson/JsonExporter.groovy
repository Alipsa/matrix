package se.alipsa.groovy.matrixjson

import se.alipsa.groovy.matrix.Grid
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.ValueConverter
import groovy.json.*

class JsonExporter {

    Matrix table

    JsonExporter(Grid grid, List<String> columnNames) {
        this.table = Matrix.builder()
        .columnNames(columnNames)
        .data(grid)
        .build()
    }

    JsonExporter(Matrix table) {
        this.table = table
    }

    String toJson(Map<String, Closure> columnFormatters = [:], boolean indent = false) {
        def nCol = table.columnCount()
        def colNames = table.columnNames()
        def rows = []
        for (def row in table.rows()) {
            def rowList = []
            for (int c = 0; c < nCol; c++) {
                def key = colNames[c]
                def value = row[c]
                if (columnFormatters.containsKey(key)) {
                    value = columnFormatters.get(key).call(value)
                }
                if (value instanceof Number) {
                    rowList << "\"${key}\":${value}"
                } else {
                    rowList << "\"${key}\":\"${ValueConverter.asString(value)}\""
                }
            }
            rows << "{${rowList.join(',')}}"
        }
        def json = "[${rows.join(',')}]"
        if (indent) {
            return JsonOutput.prettyPrint(json)
        } else {
            return json
        }
    }
}
