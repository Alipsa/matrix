package se.alipsa.matrix.json

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import groovy.json.*

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

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

    String toJson(boolean indent = false) {
        toJson([:], indent)
    }

    String toJson(Map<String, Closure> columnFormatters) {
        toJson(columnFormatters, false)
    }

    String toJson(String dateFormat) {
        toJson([:], false, dateFormat)
    }

    /**
     * If your data contains temporal data, you should add a converter for each such column
     * that converts the data into strings. If you are satisfied with converting all temporal data
     * to the format yyyy-MM-dd then you can skip that, or alternatively supply a dateFormat pattern which
     * applies to all Date and TemporalAccessor columns
     *
     * @param columnFormatters
     * @param indent whether to pretty print the json string or not
     * @param dateFormat optional date format pattern, default is yyyy-MM-dd
     * @return a json string
     */
    String toJson(Map<String, Closure> columnFormatters, boolean indent, String dateFormat='yyyy-MM-dd') {
        // Only clone if we need to apply formatters (avoids unnecessary memory allocation)
        def t = columnFormatters.isEmpty() ? table : table.clone()
        if (!columnFormatters.isEmpty()) {
            columnFormatters.each { k,v ->
                t.apply(k, v)
            }
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat)

        def jsonGenerator = new JsonGenerator.Options()
            .dateFormat(dateFormat)
            .addConverter(new JsonGenerator.Converter() {
                @Override
                boolean handles(Class<?> type) {
                    return TemporalAccessor.isAssignableFrom(type)
                }
                @Override
                Object convert(Object date, String key) {
                    dtf.format(date as TemporalAccessor)
                }
            })
            .build()

        // Stream JSON generation - convert one row at a time instead of building full List<Map>
        StringBuilder sb = new StringBuilder()
        sb.append('[')
        boolean first = true
        for (def row : t) {
            if (!first) {
                sb.append(',')
            }
            first = false
            sb.append(jsonGenerator.toJson(row.toMap()))
        }
        sb.append(']')

        String json = sb.toString()
        return indent ? JsonOutput.prettyPrint(json) : json
    }
}
